# 技术设计文档

## 1 技术选型
- **语言**：Kotlin 100 %
- **UI**：Jetpack Compose（全屏 Canvas 游戏渲染 + 传统 Compose UI 做菜单）
- **构建**：Gradle Kotlin DSL（已存在）
- **最低 SDK**：26（Android 8.0）
- **协程**：Kotlinx Coroutines（游戏循环、网络下载、IO）
- **网络**：标准 `HttpURLConnection`，无第三方库，减少体积
- **地图工具**：Tiled → 导出 JSON，瓦片尺寸 32 px 或 64 px
- **资源格式**：
  - 图像：PNG / WebP（透明），图集合并为 `atlas.png` + `atlas.json`
  - 音频：OGG（短音效 SoundPool）、MP3/OGG（长 BGM MediaPlayer/ExoPlayer）
- **开源资源**：Kenney、OpenGameArt，统一放 `assets/open_source_attribution.txt`

## 2 总体架构
```
app/src/main/java/com/example/myapplication/
├── MainActivity.kt              // 入口，锁定横屏，导航
├── game/                        // 纯游戏循环与渲染
│   ├── GameEngine.kt            // 固定步长更新 + 渲染调度
│   ├── GameScreen.kt            // Compose Canvas 全屏渲染
│   ├── World.kt                 // 关卡容器，管理实体与碰撞
│   ├── entities/
│   │   ├── Player.kt
│   │   ├── Enemy.kt
│   │   └── Projectile.kt
│   ├── level/
│   │   ├── Level.kt             // 瓦片网格 + 碰撞层
│   │   └── TiledLoader.kt       // Tiled JSON → Level 对象
│   └── physics/
│       ├── AABB.kt
│       └── CollisionGrid.kt
├── ui/                          // 传统 Compose UI（菜单、设置、更新）
│   ├── menu/
│   │   ├── MainMenuScreen.kt
│   │   ├── LevelSelectScreen.kt
│   │   └── SettingsScreen.kt
│   ├── component/
│   │   ├── VirtualPad.kt        // 虚拟按键
│   │   └── StarRating.kt
│   └── theme/                   // 已存在，按需扩展
├── assets/                      // 首包资源（gradle assets 目录）
│   ├── levels/                  // 内置 5 关 *.json
│   ├── atlas/                   // atlas.png + atlas.json
│   └── audio/                   // 首包 BGM & SFX
├── data/                        // 运行时私有目录（filesDir）
│   ├── save.json
│   ├── settings.json
│   └── assets/<version>/        // 在线更新后资源
├── update/                      // 在线更新模块
│   ├── Manifest.kt              // 清单数据类
│   ├── UpdateManager.kt         // 检查 → 下载 → 校验 → 切换
│   └── Downloader.kt            // 并发下载 + 进度回调
└── utils/
    ├── JsonUtils.kt
    ├── FileUtils.kt             // 原子写入、备份、哈希
    └── Logger.kt                // 简单文件日志
```

## 3 核心模块设计

### 3.1 GameEngine（固定步长）
```kotlin
class GameEngine(val world: World) {
    private var accumulatorNs = 0L
    private val stepNs = 16_666_667L // 60 Hz

    fun update(frameDtNs: Long) {
        accumulatorNs += frameDtNs
        while (accumulatorNs >= stepNs) {
            world.step(stepNs)
            accumulatorNs -= stepNs
        }
    }

    fun render(scope: DrawScope) = world.draw(scope)
}
```
Compose 侧使用 `LaunchedEffect(Unit) { withFrameNanos { ... } }` 驱动。

### 3.2 World & Level
- `World` 持有 `Player`、`List<Enemy>`、当前 `Level` 实例。
- `Level` 内部用 `IntArray` 保存瓦片索引，碰撞层单独 `BooleanArray` 或稀疏网格。
- 渲染：遍历可见瓦片，从 `ImageBitmap` 图集裁剪绘制。
- 碰撞：AABB 与地形网格相交测试；玩家与敌人分离轴解算。

### 3.3 输入系统
- 虚拟按键：Compose `Box` + `pointerInput`，命中区域圆形/矩形。
- 手柄：`onKeyEvent` 映射到 `InputCommand`（Left、Right、Jump、Attack）。
- 输入缓存：每帧写入 `InputState`，逻辑线程读取。

### 3.4 资源管理
- 启动时根据 `current_version` 选择 `filesDir/assets/<version>/`；无则回退首包 `assets/`。
- 图集加载：`AssetLoader.loadAtlas(dir)` → `ImageBitmap` + `Map<String, Rect>`。
- 音频：`SoundPool` 实例单例，BGM 使用 `MediaPlayer` 生命周期绑定 `GameScreen`。

### 3.5 在线更新
#### 3.5.1 清单格式（manifest.json）
```json
{
  "version": "1.0.3",
  "timestamp": 1731500000,
  "assets": [
    {"id":"tileset_main","type":"image","url":"...","sha256":"...","size":512340},
    {"id":"bgm_stage1","type":"audio","url":"...","sha256":"...","size":1023412}
  ],
  "levels": [
    {"id":"level_6","url":"...","sha256":"...","size":39812}
  ]
}
```

#### 3.5.2 更新流程
1. `UpdateManager.check(manifestUrl)` → 下载清单 → 与本地 `current_version` 对比。
2. 差异计算：按 `id` 与 `sha256` 判断缺失或变更。
3. 并发下载（`Dispatchers.IO`，最大 3 线程）→ 写入临时文件 → 校验 SHA256。
4. 全部成功：移动到 `filesDir/assets/<newVersion>/`，更新 `current_version`，重启游戏生效。
5. 任一失败：删除临时文件，提示用户重试，保持旧版本可玩。

#### 3.5.3 版本回滚
- 记录 `previous_version`；加载资源若抛出异常 → 回退到 `previous_version` 并提示。

### 3.6 存档与设置
- `SaveData` 数据类 → `Json.encodeToString()` → 原子写入 `save.json.tmp` → 重命名。
- 备份：写入前复制 `save.json → save.bak`。
- 设置同理，`settings.json` 存音量、画质、操作方案、语言。

### 3.7 日志与调试
- `Logger.d(tag, msg)` 写入 `filesDir/logs/<date>.txt`，文件大小 ≥ 1 MB 自动滚动。
- 提供“导出日志”按钮（设置页），方便用户反馈。

## 4 性能与内存
- 图集最大 2048×2048，避免多重绑定；瓦片按需裁剪缓存。
- 对象池：`Projectile`、`Particle` 使用 `ObjectPool` 避免频繁 GC。
- 纹理内存：使用 `ImageBitmap` 自动管理；关卡切换时主动 `bitmap.recycle()` 旧图集。
- 音频流：BGM 只保留一首，切换关卡时释放旧实例。

## 5 安全与合规
- 资源哈希校验，清单 HTTPS（CDN 支持）。
- 不申请敏感权限，仅 `INTERNET`（更新时）。
- 开源资源在“关于”页列出作者与许可证，附带 `open_source_attribution.txt` 文件。

## 6 测试策略
- 单元测试：
  - `CollisionGridTest`：AABB 与地形相交边界。
  - `ManifestTest`：清单解析、差异计算、回滚场景。
  - `FileUtilsTest`：原子写入、哈希校验。
- 集成测试：
  - 无网启动 → 可玩首包。
  - 弱网更新 → 中断后重试 → 完整性校验。
  - 低端机 30 fps 不掉帧（Profiler 验证）。

## 7 构建与发布
- Gradle 配置：
  - `abiFilters 'armeabi-v7a','arm64-v8a'`
  - `resources.excludes += 'DebugProbesKt.bin'`
  - `minifyEnabled true` + `proguard-rules.pro`（仅混淆代码，不压缩资源）。
- 输出：AAB 上传 Play；APK 侧载用户。
- 版本号规则：`major.minor.patch`，与 `manifest.json` 保持一致。

## 8 未来扩展（可选）
- 云存档：Google Play Games Services。
- 关卡编辑器：内置简易编辑器，导出 JSON 上传 CDN。
- 多人竞速：同一关卡幽灵竞速（需服务器，超出单机范畴，暂不实现）。

---
以上设计可直接落地到 `app/src/main/java` 目录，下一步将按包结构创建骨架代码。