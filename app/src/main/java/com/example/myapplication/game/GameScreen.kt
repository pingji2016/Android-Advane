package com.example.myapplication.game

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
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

    Canvas(modifier = modifier
        .pointerInput(Unit) {
            awaitEachGesture {
                // 1. 等待第一个手指按下
                val down = awaitFirstDown()
                // 触发按下逻辑
                gameEngine.onTouchStart(down.position.x, down.position.y)
                
                var currentPointerId = down.id
                
                // 2. 循环处理直到手指抬起
                do {
                    val event = awaitPointerEvent()
                    val changes = event.changes
                    
                    // 找到我们追踪的那个手指
                    val activeChange = changes.firstOrNull { it.id == currentPointerId }
                    
                    if (activeChange != null && activeChange.pressed) {
                        // 如果还在按压状态，更新位置
                        gameEngine.onTouchMove(activeChange.position.x, activeChange.position.y)
                        // 消费事件防止冲突（虽然这里是唯一消费者）
                        // activeChange.consume() 
                    } else {
                        // 手指抬起或丢失
                        break
                    }
                } while (changes.any { it.pressed })
                
                // 3. 触发结束逻辑
                gameEngine.onTouchEnd()
            }
        }
    ) {
        gameEngine.render(this)
    }
}
