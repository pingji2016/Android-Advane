package com.example.myapplication.game

import androidx.compose.ui.graphics.drawscope.DrawScope

/**
 * 玩家实体：基础移动、跳跃、重力
 */
class Player {
    var x = 0f
    var y = 0f
    var vx = 0f
    var vy = 0f
    private val gravity = 0.00098f // 每纳秒加速度， tuned for 60Hz

    fun update(stepNs: Long, level: Level) {
        // 简单重力
        vy += gravity * stepNs
        x += vx * stepNs / 1_000_000f
        y += vy * stepNs / 1_000_000f
        // TODO: 输入读取、碰撞解算
        level.resolveCollisions(this)
    }

    fun draw(scope: DrawScope) {
        // TODO: 从图集绘制角色帧
    }
}