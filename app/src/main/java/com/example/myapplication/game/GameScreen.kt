package com.example.myapplication.game

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.foundation.Canvas
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import kotlinx.coroutines.isActive
import kotlin.math.min

/**
 * 全屏 Canvas 游戏渲染层
 */
@Composable
fun GameScreen(
    engine: GameEngine,
    modifier: Modifier = Modifier
) {
    // 使用 remember 保持引擎实例
    val gameEngine = remember { engine }

    LaunchedEffect(Unit) {
        while (isActive) {
            androidx.compose.ui.platform.LocalDensity.current
            // 使用 withFrameNanos 驱动固定步长
            androidx.compose.animation.core.withFrameNanos { frameTimeNanos ->
                val dtNanos = if (frameTimeNanos == 0L) 16_666_667L else frameTimeNanos
                gameEngine.update(min(dtNanos, 33_333_334L)) // 限制最大 dt
            }
        }
    }

    Canvas(modifier = modifier) {
        gameEngine.render(this)
    }
}