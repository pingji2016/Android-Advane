package com.example.myapplication.game

import androidx.compose.ui.graphics.drawscope.DrawScope

/**
 * 游戏主循环引擎：固定时间步长更新 + 可变渲染插值
 */
class GameEngine(
    private val world: World
) {
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