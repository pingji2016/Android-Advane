package com.example.myapplication.update

/**
 * 远端清单数据类
 */
data class Manifest(
    val version: String,
    val timestamp: Long,
    val assets: List<Asset>,
    val levels: List<LevelEntry>
) {
    data class Asset(
        val id: String,
        val type: String,   // image / audio
        val url: String,
        val sha256: String,
        val size: Long
    )

    data class LevelEntry(
        val id: String,
        val url: String,
        val sha256: String,
        val size: Long
    )
}