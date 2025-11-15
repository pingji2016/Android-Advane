package com.example.myapplication.lan

import android.content.BroadcastReceiver
import android.content.Context
import android.content.IntentFilter
import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pManager
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceInfo
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceRequest
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Wi-Fi P2P + NSD 生命周期管理
 * 单例，绑定 MainActivity 生命周期
 */
class LanManager private constructor(context: Context) {

    companion object {
        @Volatile
        private var INSTANCE: LanManager? = null
        fun getInstance(context: Context): LanManager =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: LanManager(context.applicationContext).also { INSTANCE = it }
            }
    }

    private val manager: WifiP2pManager? = context.getSystemService(Context.WIFI_P2P_SERVICE) as? WifiP2pManager
    private var channel: WifiP2pManager.Channel? = null
    private val intentFilter = IntentFilter().apply {
        addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION)
        addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION)
        addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION)
        addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION)
    }
    private var receiver: BroadcastReceiver? = null

    // 扫描到的房间列表（NSD 结果）
    private val _rooms = MutableStateFlow<List<Room>>(emptyList())
    val rooms: StateFlow<List<Room>> = _rooms.asStateFlow()

    // 事件通道
    val eventChannel = Channel<LanEvent>(Channel.UNLIMITED)

    fun start() {
        if (manager == null) {
            eventChannel.trySend(LanEvent.P2pNotSupported)
            return
        }
        channel = manager.initialize(LanApp.instance, LanApp.instance.mainLooper, null)
        receiver = LanBroadcastReceiver(manager, channel, this)
        LanApp.instance.registerReceiver(receiver, intentFilter)
        startDiscovery()
    }

    fun stop() {
        stopDiscovery()
        channel?.also { manager?.stopPeerDiscovery(it, null) }
        receiver?.let { LanApp.instance.unregisterReceiver(it) }
        channel = null
        receiver = null
    }

    // 创建房间：成为 Group Owner，注册 NSD
    fun createRoom(room: Room) {
        val info = WifiP2pDnsSdServiceInfo.newInstance(
            room.name,          // instance name
            "_platformer._udp", // service type
            mapOf(
                "mode" to room.mode,
                "players" to "${room.players}/${room.maxPlayers}",
                "pwd" to if (room.hasPassword) "1" else "0",
                "ver" to "1.0.0"
            )
        )
        manager?.addLocalService(channel, info, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                eventChannel.trySend(LanEvent.RoomCreated(room))
            }
            override fun onFailure(reason: Int) {
                eventChannel.trySend(LanEvent.Error("createService $reason"))
            }
        })
    }

    // 开始发现服务
    private fun startDiscovery() {
        val req = WifiP2pDnsSdServiceRequest.newInstance()
        manager?.addServiceRequest(channel, req, null)
        manager?.discoverServices(channel, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {}
            override fun onFailure(reason: Int) {
                eventChannel.trySend(LanEvent.Error("discoverServices $reason"))
            }
        })
    }

    private fun stopDiscovery() {
        manager?.clearServiceRequests(channel, null)
        manager?.clearLocalServices(channel, null)
    }

    // 内部更新房间列表（由 BroadcastReceiver 回调）
    internal fun updateRooms(newList: List<Room>) {
        _rooms.value = newList
    }
}

sealed class LanEvent {
    object P2pNotSupported : LanEvent()
    data class RoomCreated(val room: Room) : LanEvent()
    data class Error(val msg: String) : LanEvent()
}

// 全局 Context 辅助（可替换为 Hilt 注入）
object LanApp {
    lateinit var instance: Context
}