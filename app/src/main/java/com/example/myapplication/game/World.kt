package com.example.myapplication.game

import androidx.compose.ui.graphics.drawscope.DrawScope

/**
 * 关卡世界容器：持有玩家、敌人列表与当前关卡，负责每步更新与渲染
 */
class World(
    val level: Level
) {
    val player = Player()
    val enemies = mutableListOf<Enemy>()

    fun step(stepNs: Long) {
        player.update(stepNs, level)
        enemies.forEach { it.update(stepNs, level) }
        // TODO: 碰撞检测、实体交互
    }

    fun draw(scope: DrawScope) {
        level.draw(scope)
        player.draw(scope)
        enemies.forEach { it.draw(scope) }
    }
}