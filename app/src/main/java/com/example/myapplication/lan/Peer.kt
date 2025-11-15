package com.example.myapplication.lan

/**
 * 对端玩家信息
 */
data class Peer(
    val playerId: Byte,
    val nickname: String,
    val isReady: Boolean = false,
    val isHost: Boolean = false,
    val pingMs: Int = 0
)