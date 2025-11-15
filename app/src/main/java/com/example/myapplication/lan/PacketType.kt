package com.example.myapplication.lan

/**
 * 协议包类型
 */
enum class PacketType(val id: Byte) {
    HANDSHAKE(0),
    WELCOME(1),
    HEARTBEAT(2),
    INPUT(3),
    STATE(4),
    SPAWN(5),
    DAMAGE(6),
    ITEM(7),
    CHAT(8),
    MIGRATE(9),
    DISCONNECT(10)
}