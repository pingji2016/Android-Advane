# 技术设计文档（深化版）

## 1 总体架构补充
在原有单机架构上新增 **lan** 模块，负责局域网联机全部逻辑：
```
app/src/main/java/com/example/myapplication/
├── lan/
│   ├── LanManager.kt            // Wi-Fi P2P + NSD 生命周期管理
│   ├── Room.kt                  // 房间数据类
│   ├── RoomManager.kt           // 房间创建/加入/离开/开始游戏
│   ├── Peer.kt                  // 对端玩家信息
│   ├── NetChannel.kt            // 可靠 UDP 封装（回退/预测）
│   ├── Packet.kt                // 协议数据单元
│   ├── SyncManager.kt           // 游戏状态同步（权威+预测）
│   ├── ChatManager.kt           // 房间内聊天（可靠 UDP）
│   └── MigrateManager.kt        // 主机迁移（投票）
```

## 2 网络技术选型
- **Wi-Fi P2P**：Android 官方 API，支持 2–4 人直连，无需路由器。
- **NSD（Network Service Discovery）**：广播房间信息，支持服务名、TXT 记录（房间名、模式、人数、密码哈希）。
- **传输层**：UDP + 自定义可靠机制（ACK+重传+乱序缓存），单包 ≤ 512 B；关键事件（伤害、道具）强制可靠。
- **频率**：60 Hz 逻辑帧，每帧位置/速度广播；丢包时客户端预测+服务器权威修正。
- **延迟**：局域网下 < 20 ms，预测误差 > 2 格时平滑插值纠正。

## 3 协议设计
### 3.1 包类型（PacketType）
```kotlin
enum class PacketType(val id: Byte) {
    HANDSHAKE(0),       // 加入房间握手
    WELCOME(1),         // 主机返回玩家ID、初始状态
    HEARTBEAT(2),       // 心跳（每秒 1 次）
    INPUT(3),           // 玩家输入（不可靠，60 Hz）
    STATE(4),           // 权威状态修正（可靠，按需）
    SPAWN(5),           // 生成实体（敌人、道具）
    DAMAGE(6),          // 伤害事件（可靠）
    ITEM(7),            // 道具拾取/使用（可靠）
    CHAT(8),            // 聊天（可靠）
    MIGRATE(9),         // 主机迁移通知
    DISCONNECT(10)      // 离开房间
}
```

### 3.2 包头格式（共 12 B）
| 字段 | 长度 | 说明 |
|------|------|------|
| type | 1 B  | PacketType.id |
| seq  | 2 B  | 自增序列号，用于 ACK/重传 |
| ack  | 2 B  | 最近收到对端 seq |
| ackBits | 4 B | 历史 ACK 位图 |
| payloadLen | 2 B | 后续载荷长度 |
| checksum | 1 B | 简单和校验 |

### 3.3 载荷示例
- **INPUT**：playerId(1) + flags(1) + vx(2) + vy(2) + aimX(2) + aimY(2) = 10 B
- **STATE**：playerId + x(4) + y(4) + vx(2) + vy(2) + hp(1) + state(1) = 18 B
- **CHAT**：playerId + msgLen(1) + msg(≤128 UTF-8) ≤ 140 B

## 4 房间生命周期
### 4.1 创建房间（主机）
1. 初始化 Wi-Fi P2P Group Owner，获取 GO IP（通常为 192.168.49.1）。
2. 启动 ServerSocket（端口 19999），监听客户端 TCP 握手（交换密钥+版本校验）。
3. 注册 NSD 服务：
   - 服务名：`_platformer._udp`
   - TXT 记录：`room=房间名&mode=coop&players=1/4&pwd=sha256(password)&ver=1.0.0`
4. 等待客户端发现与连接，维护 Peer 列表，广播 WELCOME 包分配 playerId。

### 4.2 加入房间（客户端）
1. 扫描 NSD，过滤相同 `ver`；展示房间列表（名称、模式、人数、延迟）。
2. 选择房间，TCP 握手（密码校验），成功后记录 GO IP+端口。
3. 启动 NetChannel，发送 HANDSHAKE，等待 WELCOME 获得初始状态与 playerId。
4. 进入房间 UI，可聊天、准备；房主点击开始后进入游戏同步阶段。

### 4.3 游戏同步
- 主机权威：敌人 AI、机关状态、道具生成、伤害判定。
- 客户端预测：本地输入立即表现，每帧广播 INPUT；主机每 200 ms 广播 STATE 修正，差异 > 阈值时平滑插值。
- 关键事件（DAMAGE、ITEM、SPAWN）强制可靠 ACK；超时 500 ms 重传 3 次。

### 4.4 断线与重连
- 心跳超时 5 s 判定断线，缓存玩家状态 30 s；重连时发送 HANDSHAKE+旧 playerId，主机比对缓存恢复。
- 主机断线：客户端投票（延迟最低者胜出），MigrateManager 移交权威，广播 MIGRATE 包，新主机继续游戏。

## 5 数据同步细节
### 5.1 玩家状态
- 位置：主机权威，客户端预测+插值；跳跃/攻击状态同步 flags。
- 血量：主机计算，DAMAGE 包可靠广播；客户端表现受击动画。
- 道具：主机生成 ITEM 包，客户端拾取后立即表现，失败时回滚。

### 5.2 敌人与机关
- 敌人 AI 只在主机运行，SPAWN 包广播出生位置、类型、初始朝向；客户端表现代理。
- 机关（按钮、升降台）状态由主机发送 STATE 包，客户端表现同步。

### 5.3 竞速幽灵
- 单机/联机最佳记录保存为输入序列+时间戳（ghost.json）。
- 联机竞速时，主机选择“启用幽灵”，客户端加载对应 ghost 数据，本地回放幽灵影像，不影响同步。

## 6 聊天与信号
- 预设短语 20 条（快捷键 0–9 双按），表情 8 个；支持 UTF-8 手动输入 ≤ 64 字。
- CHAT 包可靠传输，顺序广播；刷屏限制：每人每秒最多 3 条。

## 7 性能与带宽
- 4 人满员场景：
  - 上行：每人 60 Hz INPUT ≈ 600 B/s + 可靠事件平均 200 B/s ≈ 0.8 KB/s
  - 下行：主机广播 STATE 200 ms 周期 ≈ 4×18 B×5 = 360 B/s + 可靠事件 200 B/s ≈ 0.6 KB/s
- 总带宽 < 30 KB/s，局域网完全无压力；数据包单包 < 512 B，避免分片。

## 8 安全与作弊
- 局域网信任模型，仅做基础校验：版本号、包长度、checksum；不处理恶意客户端。
- 主机权威+状态修正可有效防止客户端修改位置/血量；发现异常（位置跳跃 > 5 格）可踢出房间。

## 9 错误处理与提示
- P2P 连接失败：提示“请检查 Wi-Fi 是否开启，或尝试重启 WLAN”。
- 房间已满：提示“房间人数已达上限”。
- 密码错误：提示“房间密码错误”。
- 主机迁移：顶部横幅“正在迁移主机…”，2 s 内完成。

## 10 联机 UI 状态机
```
MainMenu -> LanLobby -> RoomList -> CreatingRoom -> RoomInside(Ready/Chat) -> GameSync -> GameRunning -> GameResult -> RoomInside
```
所有状态通过 `LanViewModel` 暴露 Compose UI，单向数据流；网络事件通过 `Channel<LanEvent>` 发送到 UI 层。

## 11 兼容与降级
- 不支持 Wi-Fi P2P 的设备（Android 4.x 或阉割）提示“当前设备不支持局域网联机”，隐藏入口。
- 联机失败后自动提供“单机继续”按钮，不阻塞核心体验。

---
下一版将实现上述骨架代码与 UI。