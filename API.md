# 接口文档 (API Documentation)

本文档定义了 Android-Advane 项目中远程控制模块的通信协议，主要包括基于 WebRTC DataChannel 的控制指令协议，以及建议的信令交换协议。

## 1. DataChannel 控制协议

客户端（控制端）通过 WebRTC 的 DataChannel通道发送 JSON 格式的字符串指令，受控端（Android App）解析并执行相应的无障碍操作。

### 1.1 坐标系统
所有坐标参数 (`x`, `y`, `dx`, `dy`) 均采用 **归一化坐标 (Normalized Coordinates)**，范围为 `0.0` 至 `1.0`。
- `(0.0, 0.0)`: 屏幕左上角
- `(1.0, 1.0)`: 屏幕右下角

### 1.2 指令列表

#### 1.2.1 点击 (Click)
模拟手指在屏幕指定位置的点击操作。

**JSON 格式:**
```json
{
  "type": "click",
  "x": 0.5,      // [必填] X轴坐标 (0.0-1.0)
  "y": 0.5       // [必填] Y轴坐标 (0.0-1.0)
}
```

#### 1.2.2 滑动 (Scroll/Swipe)
模拟手指在屏幕上的滑动操作。

**JSON 格式:**
```json
{
  "type": "scroll",
  "x": 0.5,      // [必填] 起始点 X轴坐标
  "y": 0.5,      // [必填] 起始点 Y轴坐标
  "dx": 0.0,     // [必填] X轴滑动距离向量 (-1.0 到 1.0)
  "dy": -0.2,    // [必填] Y轴滑动距离向量 (-1.0 到 1.0)
  "duration": 300 // [可选] 滑动持续时间(毫秒)，默认 300ms
}
```
*示例解释：从屏幕中心向上滑动屏幕高度的 20%（通常用于浏览网页或列表）。*

#### 1.2.4 文本输入 (Text Input)
将指定文本直接设置到当前具有焦点的输入框（需输入框处于可编辑状态）。

**JSON 格式:**
```json
{
  "type": "text",
  "text": "Hello World" // [必填] 要输入的文本内容
}
```

#### 1.2.5 音量控制 (Volume Control)
调节设备的媒体音量。

**JSON 格式:**
```json
{
  "type": "volume",
  "action": "up" | "down" | "mute" // [必填] 操作类型：增加、减少、静音切换
}
```

#### 1.2.6 系统控制 (System Controls)
执行系统级界面操作。

**JSON 格式:**
```json
{
  "type": "notifications" | "quick_settings" | "lock" | "screenshot"
}
```
*   `notifications`: 展开通知栏
*   `quick_settings`: 展开快速设置面板
*   `lock`: 锁定屏幕 (Android 9+)
*   `screenshot`: 截取屏幕 (Android 9+)

#### 1.2.7 应用启动 (Launch App)
通过包名启动指定应用。

**JSON 格式:**
```json
{
  "type": "launch",
  "package": "com.android.settings" // [必填] 目标应用包名
}
```

#### 1.2.8 剪贴板控制 (Clipboard)
设置设备剪贴板内容。

**JSON 格式:**
```json
{
  "type": "clipboard",
  "text": "Copied Text" // [必填] 要复制的文本
}
```

#### 1.2.9 唤醒屏幕 (Wake Up)
点亮屏幕并唤醒设备。

**JSON 格式:**
```json
{
  "type": "wake"
}
```

#### 1.2.10 全局操作 (Global Actions)
执行 Android 系统级导航操作。

**JSON 格式:**
```json
{
  "type": "action_type"
}
```

**支持的 `type` 值:**
- `"back"`: 模拟点击返回键
- `"home"`: 模拟点击 Home 键
- `"recent"`: 打开最近任务列表

---

## 2. 信令接口 (Signaling Interface)

*注：本部分为设计规范，用于指导 Phase 3 服务端开发。*

信令服务器用于协助两个对等端（Peer）建立 WebRTC 连接。建议使用 WebSocket 或 HTTP 长轮询。

### 2.1 消息结构
所有信令消息建议采用以下 JSON 结构：
```json
{
  "type": "offer" | "answer" | "candidate",
  "payload": { ... }
}
```

### 2.2 流程定义

#### 2.2.1 交换 SDP (Session Description Protocol)

**Offer (发起端 -> 服务器 -> 接收端):**
```json
{
  "type": "offer",
  "payload": {
    "sdp": "v=0...",
    "type": "offer"
  }
}
```

**Answer (接收端 -> 服务器 -> 发起端):**
```json
{
  "type": "answer",
  "payload": {
    "sdp": "v=0...",
    "type": "answer"
  }
}
```

#### 2.2.2 交换 ICE Candidate

当 WebRTC 引擎发现新的网络路径时触发。

**Candidate (双向):**
```json
{
  "type": "candidate",
  "payload": {
    "sdpMid": "0",
    "sdpMLineIndex": 0,
    "candidate": "candidate:..."
  }
}
```
devtest3
## 3. 代码参考
- **指令解析实现**: [ControlService.kt](app/src/main/java/com/example/myapplication/service/ControlService.kt)
- **WebRTC 客户端**: [WebRtcClient.kt](rtc/src/main/java/com/example/rtc/WebRtcClient.kt)
