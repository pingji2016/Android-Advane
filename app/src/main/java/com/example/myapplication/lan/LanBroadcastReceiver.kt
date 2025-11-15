package com.example.myapplication.lan

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.wifi.p2p.WifiP2pManager
import android.net.wifi.p2p.WifiP2pManager.P2P_UNSUPPORTED
import android.net.wifi.p2p.WifiP2pManager.WIFI_P2P_STATE_ENABLED

/**
 * 负责接收 Wi-Fi P2P 系统广播，解析 NSD 服务，更新房间列表
 */
class LanBroadcastReceiver(
    private val manager: WifiP2pManager,
    private val channel: WifiP2pManager.Channel?,
    private val lanManager: LanManager
) : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION -> {
                val state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1)
                if (state == WIFI_P2P_STATE_ENABLED) {
                    // OK
                } else if (state == P2P_UNSUPPORTED) {
                    lanManager.eventChannel.trySend(LanEvent.P2pNotSupported)
                }
            }
            WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION -> {
                // 可请求 peers（暂不处理，NSD 为主）
            }
            WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION -> {
                // 连接状态变化，可获取 GO IP（后续 TCP 握手用）
            }
            WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION -> {
                // 本机设备信息变化
            }
        }
    }
}