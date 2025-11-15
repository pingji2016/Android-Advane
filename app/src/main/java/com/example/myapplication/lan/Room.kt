package com.example.myapplication.lan

/**
 * 房间数据类
 */
data class Room(
    val name: String,
    val mode: String, // coop / race / brawl
    val players: Int,
    val maxPlayers: Int = 4,
    val hasPassword: Boolean,
    val goIp: String, // 192.168.49.1
    val port: Int = 19999,
    val txtRecord: Map<String, String>
)