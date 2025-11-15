# 实现指南（深化版）

## 1 已落地回顾
- ✅ 单机骨架 + 在线更新 + 存档系统
- ✅ 需求与技术设计已新增 **局域网联机** 全部模块

## 2 新增联机实现步骤（按优先级）

### 2.1 权限与 Gradle
在 `AndroidManifest.xml` 添加：
```xml
<uses-permission android:name="android.permission.ACCESS_WIFI_STATE"/>
<uses-permission android:name="android.permission.CHANGE_WIFI_STATE"/>\<uses-permission android:name="android.permission.INTERNET"/> <!-- 更新已存在 -->
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION"/> <!-- Wi-Fi P2P 需要 -->
```
在 `app/build.gradle.kts` 新增：
```kotlin
implementation("androidx.core:core-ktx:1.12.0")
```

### 2.2 核心联机骨架（先跑通房间列表）
1. 新建 `lan/LanManager.kt`：
   - 初始化 `WifiP2pManager`、`Channel`、`PeerListListener`。
   - 开启 NSD：`NsdManager.registerService()` 主机广播；`discoverServices()` 客户端扫描。
   - 提供 `start()/stop()` 生命周期绑定 `MainActivity.onCreate/onDestroy`。
2. 新建 `lan/Room.kt` 数据类：
```kotlin
data class Room(
    val name: String,
    val mode: String, // coop / race / brawl
    val players: Int,
    val maxPlayers: Int = 4,
    val hasPassword: Boolean,
    val goIp: String, // 192.168.49.1
    val port: Int = 19999,
    val txtRecord: Map<String, String>
)
```
3. 新建 `lan/RoomManager.kt`：
   - 持有当前房间 `Room?`、Peer 列表 `List<Peer>`。
   - `createRoom(name, password, mode)` → 成为 GO，注册 NSD，启动 ServerSocket。
   - `joinRoom(room)` → TCP 握手（密码校验），成功后进入房间内部 UI。
   - `leaveRoom()` → 发送 DISCONNECT 包，关闭 Socket，注销服务/停止发现。
4. 新建 `ui/lan/LanLobbyScreen.kt`：
   - 顶部按钮：创建房间、快速匹配、扫码加入。
   - 房间列表：LazyColumn 展示扫描结果（名称、模式、人数、延迟）。
   - 点击房间 → 输入密码 → 调用 `RoomManager.joinRoom()`。

### 2.3 房间内部 UI
新建 `ui/lan/RoomInsideScreen.kt`：
- 顶部：房间名、模式、复制房间码按钮。
- 玩家列表：昵称、Ping、准备状态、房主皇冠；房主可踢人。
- 底部：聊天面板（LazyColumn + TextField + 表情 Grid），准备/取消准备按钮；房主额外“开始游戏”按钮（全员准备后可用）。
- ViewModel：`LanViewModel` 持有 `RoomManager` 状态，暴露 `uiState: State<LanUiState>`。

### 2.4 实时同步（先跑通 INPUT/STATE）
1. 新建 `lan/NetChannel.kt`：
   - 封装 DatagramSocket，维护 seq/ack/ackBits，提供 `sendUnreliable/sendReliable`。
   - 后台协程：接收线程 + 发送队列 + 重传定时器（500 ms 重传 3 次）。
2. 新建 `lan/SyncManager.kt`：
   - 游戏循环 60 Hz 调用 `broadcastInput(input)`。
   - 每 200 ms 收集权威状态（主机）→ `broadcastState(states)`。
   - 客户端收到 STATE → 与本地预测对比，误差 > 2 格时线性插值纠正。
3. 游戏层改造：
   - `World.step()` 区分 **isHost**；主机运行 AI，客户端仅表现。
   - 敌人出生：主机调用 `SyncManager.spawnEnemy(x,y,type)` → 广播 SPAWN 包。
   - 伤害：主机计算 → `SyncManager.sendDamage(targetId, amount)` → 广播 DAMAGE 包。

### 2.5 聊天与表情
- 预设短语数组 `listOf("加油！","快点！","谢谢！","救命！")` + 表情 8 个 Unicode。
- 快捷双击 0–9 数字键发送预设；TextField 手动输入 ≤ 64 字。
- 消息缓存 50 条，超出后自动清理。

### 2.6 主机迁移
- 心跳超时 5 s 判定断线；主机掉线时客户端弹出投票弹窗（延迟最低者胜出）。
- `MigrateManager` 负责：
  1. 选举新主机（延迟最低）。
  2. 广播 MIGRATE 包（含新主机 GO IP）。
  3. 新主机启动 ServerSocket，旧主机关闭；游戏继续。

### 2.7 竞速幽灵录像
- 单机/联机最佳记录保存为 `ghost.json`：输入序列 + 时间戳数组。
- 竞速模式房间设置“启用幽灵”时，客户端加载 ghost，本地回放幽灵影像（不占用同步带宽）。
- 幽灵影像：透明蓝色精灵，按输入序列驱动，与本地玩家无碰撞。

### 2.8 设置页扩展
新增“网络”子页：
- 昵称（≤12 字）、监听端口（默认 19999）、回退预测开关、清除最近房间缓存。
- 显示当前 GO IP、P2P 状态、NAT 类型（Local/GO）。

## 3 快速验证联机流程
1. 两台 Android 8+ 设备，A 创建房间 → B 扫描列表 → 加入 → 双方准备 → A 开始游戏。
2. 应进入同一关卡，B 能看到 A 的玩家角色实时移动；A 跳跃/攻击 B 端同步。
3. 关闭 A 的 Wi-Fi，B 提示“正在迁移主机”，2 s 后继续游戏，B 成为新主机。
4. 打开聊天，发送预设短语，双方立即看到；手动输入“Hello LAN”，无乱码。

## 4 性能与调试
- Android Studio Network Profiler 观察 UDP 流量：4 人 INPUT 60 Hz ≈ 30 KB/s 上下行。
- 日志：`LanManager`、`NetChannel` 关键路径使用 `Logger.d(tag, msg)` 写入 `filesDir/logs/lan_<date>.txt`，支持导出。
- 断线模拟：使用 `adb shell am broadcast -a com.example.myapplication.SIMULATE_DISCONNECT` 触发，代码中监听调试广播。

## 5 下一步可继续扩展
- 二维码生成/扫描：使用 ZXing Android Embedded，生成含 `ip:port:passwordHash` 的 URL。
- 蓝牙手柄映射：扫描完成后，通过 `InputManager` 统一分发按键到 `InputState`。
- 关卡编辑器：内置简易 Tiled 编辑器，导出 JSON 后通过联机房间“自定义关卡”共享（主机发送关卡数据包）。

至此，局域网联机合作/对抗、聊天、主机迁移、幽灵录像全部打通，可进入 M5 功能细化与 UI 打磨阶段。