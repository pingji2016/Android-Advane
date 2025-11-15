package com.example.myapplication.lan

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.*
import java.net.InetAddress

/**
 * 房间管理：创建/加入/离开/开始游戏
 * 单例，依赖 LanManager 与 NetChannel
 */
class RoomManager private constructor() {

    companion object {
        @Volatile
        private var INSTANCE: RoomManager? = null
        fun getInstance(): RoomManager =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: RoomManager().also { INSTANCE = it }
            }
    }

    var currentRoom: Room? by mutableStateOf(null)
        private set
    var peers: List<Peer> by mutableStateOf(emptyList())
        private set
    var isHost: Boolean by mutableStateOf(false)
        private set

    private var channel: NetChannel? = null
    private var serverJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // 创建房间：成为 GO，启动 ServerSocket，注册 NSD
    fun createRoom(name: String, password: String, mode: String) {
        isHost = true
        val room = Room(name, mode, 1, 4, password.isNotEmpty(), "192.168.49.1", 19999, emptyMap())
        currentRoom = room
        // 启动 NSD 由 LanManager 完成
        LanManager.getInstance(LanApp.instance).createRoom(room)
        startServer()
        connectChannel("192.168.49.1")
    }

    // 加入房间：TCP 握手（简化：直接 UDP），密码校验
    fun joinRoom(room: Room, password: String) {
        isHost = false
        currentRoom = room
        connectChannel(room.goIp)
        // 发送 HANDSHAKE 包
        val payload = buildString {
            append("pwd=${password.sha256()}")
        }.toByteArray()
        channel?.sendReliable(PacketType.HANDSHAKE, payload)
    }

    fun leaveRoom() {
        channel?.sendReliable(PacketType.DISCONNECT, ByteArray(0))
        serverJob?.cancel()
        channel?.close()
        currentRoom = null
        peers = emptyList()
        isHost = false
    }

    // 开始游戏（仅主机）
    fun startGame() {
        if (!isHost) return
        // 广播 SPAWN 关卡数据（简化：仅通知）
        channel?.sendReliable(PacketType.SPAWN, "level_1".toByteArray())
    }

    private fun connectChannel(goIp: String) {
        val inet = InetAddress.getByName(goIp)
        channel = NetChannel(inet, 19999).also { ch ->
            scope.launch {
                while (isActive) {
                    val pkt = ch.recv()
                    if (pkt != null) handlePacket(pkt)
                    ch.resendTimeout()
                    delay(16) // 60 Hz
                }
            }
        }
    }

    private fun startServer() {
        serverJob = scope.launch {
            java.net.ServerSocket(19999).use { server ->
                while (isActive) {
                    val socket = server.accept()
                    // 简化：直接交给 NetChannel，此处仅占位
                    socket.close()
                }
            }
        }
    }

    private fun handlePacket(pkt: Packet) {
        when (pkt.type) {
            PacketType.WELCOME -> {
                // 客户端收到：解析 playerId
            }
            PacketType.CHAT -> {
                // 广播到 UI
            }
            else -> {}
        }
    }

    private fun String.sha256(): String {
        val md = java.security.MessageDigest.getInstance("SHA-256")
        return md.digest(this.toByteArray()).joinToString("") { "%02x".format(it) }
    }
}