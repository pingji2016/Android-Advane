package com.example.myapplication.game

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope

/**
 * 关卡：瓦片地图 + 碰撞层
 */
class Level(
    val tileset: ImageBitmap,
    val tiles: IntArray,        // 瓦片索引，行优先
    val width: Int,             // 瓦片列数
    val height: Int,            // 瓦片行数
    val tileSize: Int           // 瓦片像素尺寸
) {
    fun draw(scope: DrawScope) {
        // TODO: 遍历可见区域，从 tileset 裁剪绘制
    }

    fun resolveCollisions(player: Player) {
        // TODO: AABB 与地形网格相交测试，修正 player 位置
    }
}