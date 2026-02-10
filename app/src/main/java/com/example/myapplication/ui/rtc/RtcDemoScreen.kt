package com.example.myapplication.ui.rtc

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.myapplication.R

@Composable
fun RtcDemoScreen(
    onBack: () -> Unit,
    viewModel: RtcViewModel = viewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()

    val projectionManager = context.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
    val screenCaptureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            val intent = Intent(context, RtcService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
            viewModel.startScreenCapture(result.data!!)
        } else {
            // log("Screen Capture Permission Denied")
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Button(
            onClick = onBack,
            modifier = Modifier.fillMaxWidth()
        ) { 
            Text(stringResource(R.string.rtc_back)) 
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Signaling Controls
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(8.dp)) {
                Text("Signaling Server", style = MaterialTheme.typography.titleMedium)
                TextField(
                    value = uiState.serverUrl,
                    onValueChange = { viewModel.updateServerUrl(it) },
                    label = { Text("Server URL") },
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextField(
                        value = uiState.roomId,
                        onValueChange = { viewModel.updateRoomId(it) },
                        label = { Text("Room ID") },
                        modifier = Modifier.weight(1f)
                    )
                    Button(onClick = {
                        if (uiState.isConnected) {
                            viewModel.disconnectSignaling()
                        } else {
                            viewModel.connectSignaling()
                        }
                    }) {
                        Text(if (!uiState.isConnected) "Connect" else "Disconnect")
                    }
                }
                Text("Status: ${uiState.connectionStatus}", style = MaterialTheme.typography.bodySmall)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            item {
                Button(
                    onClick = { screenCaptureLauncher.launch(projectionManager.createScreenCaptureIntent()) },
                    enabled = !uiState.isScreenCapturing,
                    modifier = Modifier.fillMaxWidth()
                ) { Text(stringResource(R.string.rtc_start_screen)) }
            }
            item {
                Button(
                    onClick = {
                        viewModel.stopScreenCapture()
                        context.stopService(Intent(context, RtcService::class.java))
                    },
                    enabled = uiState.isScreenCapturing,
                    modifier = Modifier.fillMaxWidth()
                ) { Text(stringResource(R.string.rtc_stop_screen)) }
            }
            item {
                Button(
                    onClick = { viewModel.startAudioCapture() },
                    enabled = !uiState.isAudioCapturing,
                    modifier = Modifier.fillMaxWidth()
                ) { Text(stringResource(R.string.rtc_start_audio)) }
            }
            item {
                Button(
                    onClick = { viewModel.stopAudioCapture() },
                    enabled = uiState.isAudioCapturing,
                    modifier = Modifier.fillMaxWidth()
                ) { Text(stringResource(R.string.rtc_stop_audio)) }
            }
            item {
                Button(
                    onClick = {
                        viewModel.sendMessage("Hello World ${System.currentTimeMillis()}")
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text(stringResource(R.string.rtc_send_msg)) }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(stringResource(R.string.rtc_protocol_switch), style = MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            val selectedCodec = remember { mutableStateOf<String?>(null) }
            listOf("VP8", "VP9", "H264").forEach { codec ->
                Button(
                    onClick = {
                        selectedCodec.value = codec
                        viewModel.setPreferredCodec(codec)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (selectedCodec.value == codec) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                    )
                ) { Text(codec) }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text(stringResource(R.string.rtc_logs), style = MaterialTheme.typography.titleMedium)
        LazyColumn {
            items(uiState.logs) { logMsg ->
                Text(logMsg, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
