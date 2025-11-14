package com.example.myapplication.data

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

/**
 * 本地存档数据类
 */
@Serializable
data class SaveData(
    val currentLevel: String = "level_1",
    val levelScores: Map<String, LevelScore> = emptyMap()
) {
    @Serializable
    data class LevelScore(
        val bestTimeMs: Long,
        val stars: Int
    )
}

object SaveManager {
    private val json = Json { prettyPrint = true }

    fun load(rootDir: File): SaveData {
        val file = File(rootDir, "data/save.json")
        if (!file.exists()) return SaveData()
        return runCatching {
            json.decodeFromString<SaveData>(file.readText())
        }.getOrDefault(SaveData())
    }

    fun save(rootDir: File, data: SaveData) {
        val dir = File(rootDir, "data").apply { mkdirs() }
        val file = File(dir, "save.json")
        val tmp = File(dir, "save.json.tmp")
        tmp.writeText(json.encodeToString(data))
        tmp.renameTo(file)
    }
}