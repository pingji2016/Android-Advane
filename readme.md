# Android-Advane: Remote Control & Game Project

## 1. 项目简介
本项目是一个集成了休闲小游戏与远程控制功能的 Android 应用。表面上是一个横屏闯关小游戏，实则内置了基于 WebRTC 的远程控制模块，允许授权用户通过网络远程监视手机屏幕、监听环境音，并进行远程触控操作。

## 2. 功能模块

### 2.1 游戏模块 (伪装层)
- **类型**: 横屏物理闯关游戏
- **引擎**: 自研轻量级游戏引擎 (基于 Compose/Canvas)
- **状态**: 已实现基础物理碰撞和关卡加载

### 2.2 远程控制模块 (核心层)
- **实时投屏**: 使用 MediaProjection API 采集屏幕，通过 WebRTC VideoTrack 传输。
- **环境音监听**: 采集麦克风音频，通过 WebRTC AudioTrack 传输。
- **低延迟指令传输**: 利用 WebRTC DataChannel 双向传输控制指令（点击、滑动、按键）。
- **远程操作执行**:
    - **技术方案**: Android AccessibilityService (无障碍服务)。
    - **能力**: 模拟全局点击、长按、滑动、Home/Back 键等。
    - **高级功能**: 
        - **远程输入**: 将文本直接注入到当前焦点的输入框。
        - **音量控制**: 远程调节设备媒体音量。
        - **系统控制**: 展开通知栏/控制中心、锁屏、截屏(Android 9+)。
        - **应用管理**: 通过包名远程启动指定 App。
        - **剪贴板**: 远程设置手机剪贴板内容。
        - **电源控制**: 远程点亮屏幕/唤醒设备。

## 3. 技术架构

### 3.1 客户端 (Android)
- **语言**: Kotlin
- **UI 框架**: Jetpack Compose
- **WebRTC**: Google WebRTC 库 (org.webrtc)
- **核心组件**:
    - `WebRtcClient`: 封装 PeerConnection, SDP 协商, ICE 处理。
    - `RtcService`: 前台服务，维持 MediaProjection 和连接保活。
    - `ControlService`: 继承 AccessibilityService，解析 DataChannel 指令并执行模拟操作。

### 3.2 服务端 (Signaling)
- *待实现*
- 职责: 交换 SDP Offer/Answer 和 ICE Candidates。
- 协议: WebSocket / HTTP。

## 4. 开发计划 (Roadmap)

### Phase 1: 基础架构搭建 (已完成)
- [x] WebRTC 基础库集成
- [x] 游戏 Demo 实现
- [x] 屏幕录制与推流 (Video)
- [x] 音频采集与传输 (Audio)
- [x] DataChannel 信令通道打通
- [x] 多编码格式支持 (VP8/VP9/H264) 切换

### Phase 2: 远程控制实现 (Doing)
- [ ] **无障碍服务集成**: 实现 `AccessibilityService`，获取系统授权。
- [ ] **指令解析器**: 定义 JSON 协议（如 `{ "type": "touch", "x": 100, "y": 200 }`）。
- [ ] **坐标映射**: 处理不同分辨率设备的坐标转换。
- [ ] **后台保活优化**: 确保切出游戏后服务依然运行。

### Phase 3: 服务端与 P2P 优化 (Planned)
- [ ] 部署信令服务器 (Node.js/Go)。
- [ ] 部署 TURN/STUN 服务器 (Coturn) 解决内网穿透。
- [ ] 增加安全验证（连接密码/PIN码）。

### Phase 4: 完善与发布
- [ ] 隐藏 App 图标或伪装入口。
- [ ] 性能优化（码率自适应、电量优化）。

## 5. 快速开始
1. 编译并安装 `app` 模块。
2. 打开应用，授予“录音”、“相机”、“悬浮窗”及“无障碍服务”权限。
3. 在“WebRTC Demo”页面测试连接。
