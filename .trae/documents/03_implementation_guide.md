# 横屏闯关小游戏 技术实现说明

## 1 已落地骨架
- ✅ 需求文档：`.trae/documents/01_requirements.md`
- ✅ 技术设计：`.trae/documents/02_technical_design.md`
- ✅ 代码骨架：
  - `game/`：GameEngine、GameScreen、World、Player、Enemy、Level、TiledLoader
  - `update/`：Manifest、UpdateManager（清单获取 → 下载 → 校验 → 版本切换）
  - `data/`：SaveManager（kotlinx-serialization-json）
  - `ui/menu/`：MainMenuScreen（Compose）
  - `MainActivity.kt`：横屏锁定 + 简单导航（菜单 ↔ 游戏）
- ✅ 依赖：
  - `minSdk 26`（Android 8.0）
  - 新增 `kotlinx-serialization-json` 与 `kotlinx-coroutines-android`

## 2 下一步可继续实现（M1 原型）
1. 放置开源图集与首关 Tiled JSON 到 `app/src/main/assets/`
2. 在 `MainActivity` 启动时调用 `UpdateManager.checkAndUpdate()`（可选）
3. 实现 `AssetLoader.loadAtlas()` 与 `TiledLoader.load()` 真实加载
4. 虚拟按键：新建 `ui/component/VirtualPad.kt`，在 `GameScreen` 叠加
5. 输入系统：将按键状态写入 `InputState`，供 `Player.update()` 读取
6. 碰撞与地形：`Level.resolveCollisions()` 实现 AABB vs 瓦片网格
7. 敌人派生类：`PatrolEnemy`、`ChaseEnemy` 等，加入 `World.enemies`
8. 存档：通关后调用 `SaveManager.save()` 写 `bestTimeMs` 与 `stars`

## 3 资源建议（开源）
- 图集：Kenney "Platformer Art Deluxe"（CC0）
- 音效：Kenney "Digital Audio"（CC0）
- BGM：OpenGameArt "Platformer Beat"（CC-BY 3.0，需署名）

## 4 快速验证
运行 `gradlew installDebug` → 应看到主菜单 → 点击“开始游戏”进入空关卡画布（黑色）。
后续填充图集与关卡即可出现角色与地形。

## 5 在线更新验证
1. 把示例 `manifest.json` 放到任意 HTTPS 可访问地址（如 GitHub Pages）
2. 在设置页放“检查更新”按钮 → 调用 `UpdateManager.checkAndUpdate(url, filesDir)`
3. 观察 `filesDir/assets/<version>/` 出现新资源，重启后加载新关卡

## 6 性能注意
- 图集最大 2048×2048，避免多图绑定
- 对象池复用 `Projectile`/`Particle`
- 使用 Android Studio Profiler 监控帧率与内存

至此，单机横屏闯关 + 在线增量更新核心框架已就绪，可继续按里程碑迭代。