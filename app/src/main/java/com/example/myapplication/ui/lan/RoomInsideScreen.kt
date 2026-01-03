package com.example.myapplication.ui.lan

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.myapplication.lan.Peer
import com.example.myapplication.lan.RoomManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoomInsideScreen(
    onBack: () -> Unit
) {
    val room = RoomManager.getInstance().currentRoom
    val peers = RoomManager.getInstance().peers
    val isHost = RoomManager.getInstance().isHost

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(room?.name ?: "房间") },
                navigationIcon = {
                    IconButton(onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回") }
                },
                actions = {
                    IconButton(onClick = { /* TODO 复制房间码 */ }) {
                        Icon(Icons.Default.Star, contentDescription = "复制")
                    }
                }
            )
        },
        bottomBar = {
            BottomAppBar {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    var ready by remember { mutableStateOf(false) }
                    Button(
                        onClick = { ready = !ready },
                        modifier = Modifier.weight(1f).padding(end = 8.dp)
                    ) { Text(if (ready) "取消准备" else "准备") }
                    if (isHost) {
                        Button(
                            onClick = { RoomManager.getInstance().startGame() },
                            enabled = peers.all { it.isReady } && peers.size >= 1,
                            modifier = Modifier.weight(1f)
                        ) { Text("开始游戏") }
                    }
                }
            }
        }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            // 玩家列表
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(peers) { peer ->
                    PlayerCard(peer, isHost)
                }
            }
            Spacer(Modifier.height(16.dp))
            // 聊天面板（简化占位）
            Card(Modifier.fillMaxWidth()) {
                Text("聊天功能稍后实现", Modifier.padding(16.dp))
            }
        }
    }
}

@Composable
private fun PlayerCard(peer: Peer, isHostView: Boolean) {
    Card(Modifier.fillMaxWidth()) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Row {
                    Text(peer.nickname, style = MaterialTheme.typography.titleMedium)
                    if (peer.isHost) {
                        Icon(Icons.Default.Star, contentDescription = "房主", tint = MaterialTheme.colorScheme.primary)
                    }
                }
                Text("Ping ${peer.pingMs} ms · ${if (peer.isReady) "已准备" else "未准备"}", style = MaterialTheme.typography.bodySmall)
            }
            if (isHostView && !peer.isHost) {
                IconButton(onClick = { /* TODO 踢人 */ }) {
                    Icon(Icons.Default.Close, contentDescription = "踢出")
                }
            }
        }
    }
}