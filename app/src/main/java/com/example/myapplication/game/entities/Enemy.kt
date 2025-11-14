package com.example.myapplication.game

import androidx.compose.ui.graphics.drawscope.DrawScope

/**
 * 敌人实体基类：巡逻、追击等状态机预留
 */
abstract class Enemy {
    var x = 0f
    var y = 0f
    var vx = 0f
    var vy = 0f

    abstract fun update(stepNs: Long, level: Level)
    abstract fun draw(scope: DrawScope)
}