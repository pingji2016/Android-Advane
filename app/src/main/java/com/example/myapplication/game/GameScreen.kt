package com.example.myapplication.game

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
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
        // 获取初始帧时间
        var lastFrameTime = withFrameNanos { it }
        
        while (isActive) {
            // 使用 withFrameNanos 驱动固定步长
            withFrameNanos { frameTimeNanos ->
                val dtNanos = frameTimeNanos - lastFrameTime
                lastFrameTime = frameTimeNanos
                
                // 避免第一帧或负值
                if (dtNanos > 0) {
                    gameEngine.update(min(dtNanos, 33_333_334L)) // 限制最大 dt
                }
            }
        }
    }

    Canvas(modifier = modifier) {
        gameEngine.render(this)
    }
}
