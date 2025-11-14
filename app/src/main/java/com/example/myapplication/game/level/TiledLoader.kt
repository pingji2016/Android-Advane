package com.example.myapplication.game.level

import com.example.myapplication.game.Level
import androidx.compose.ui.graphics.ImageBitmap
import org.json.JSONObject
import java.io.File

/**
 * Tiled 导出 JSON → Level 对象
 */
object TiledLoader {
    fun load(jsonFile: File, tilesetBitmap: ImageBitmap, tileSize: Int): Level {
        val obj = JSONObject(jsonFile.readText())
        val layers = obj.getJSONArray("layers")
        // 找到地形层
        val tileLayer = (0 until layers.length())
            .map { layers.getJSONObject(it) }
            .first { it.getString("type") == "tilelayer" }
        val width = tileLayer.getInt("width")
        val height = tileLayer.getInt("height")
        val data = tileLayer.getJSONArray("data")
        val tiles = IntArray(width * height) { idx ->
            data.getInt(idx) - 1 // Tiled 全局图块 ID 从 1 开始
        }
        return Level(tilesetBitmap, tiles, width, height, tileSize)
    }
}