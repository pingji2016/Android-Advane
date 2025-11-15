package com.example.myapplication.ui.lan

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.myapplication.lan.LanManager
import com.example.myapplication.lan.Room

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LanLobbyScreen(
    onBack: () -> Unit,
    onRoomClick: (Room) -> Unit
) {
    val rooms by LanManager.getInstance(com.example.myapplication.lan.LanApp.instance).rooms.collectAsState()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("联机大厅") },
                navigationIcon = {
                    IconButton(onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回") }
                },
                actions = {
                    IconButton(onClick = { /* 刷新 */ }) {
                        Icon(Icons.Default.Refresh, contentDescription = "刷新")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { /* TODO 创建房间对话框 */ },
                    modifier = Modifier.weight(1f)
                ) { Text("创建房间") }
                OutlinedButton(
                    onClick = { /* TODO 快速匹配 */ },
                    modifier = Modifier.weight(1f)
                ) { Text("快速匹配") }
            }
            Spacer(Modifier.height(16.dp))
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(rooms) { room ->
                    RoomCard(room, onClick = { onRoomClick(room) })
                }
            }
        }
    }
}

@Composable
private fun RoomCard(room: Room, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(room.name, style = MaterialTheme.typography.titleMedium)
                Text("${room.mode} · ${room.players}/${room.maxPlayers}", style = MaterialTheme.typography.bodySmall)
            }
            if (room.hasPassword) {
                Icon(Icons.Default.Lock, contentDescription = "有密码", tint = MaterialTheme.colorScheme.secondary)
            }
        }
    }
}