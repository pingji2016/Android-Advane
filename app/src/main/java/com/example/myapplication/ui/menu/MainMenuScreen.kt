package com.example.myapplication.ui.menu

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun MainMenuScreen(
    onStartGame: () -> Unit,
    onLevelSelect: () -> Unit,
    onSettings: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("横屏闯关", style = MaterialTheme.typography.headlineLarge)
        Spacer(Modifier.height(32.dp))
        Button(onClick = onStartGame, modifier = Modifier.fillMaxWidth(0.6f)) {
            Text("开始游戏")
        }
        Spacer(Modifier.height(16.dp))
        OutlinedButton(onLevelSelect, modifier = Modifier.fillMaxWidth(0.6f)) {
            Text("关卡选择")
        }
        Spacer(Modifier.height(16.dp))
        OutlinedButton(onSettings, modifier = Modifier.fillMaxWidth(0.6f)) {
            Text("设置")
        }
    }
}