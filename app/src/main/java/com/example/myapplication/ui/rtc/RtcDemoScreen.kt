package com.example.myapplication.ui.rtc

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.myapplication.service.ControlService
import com.example.rtc.WebRtcClient
import org.webrtc.*

@Composable
fun RtcDemoScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var logs by remember { mutableStateOf(listOf<String>()) }
    
    fun log(msg: String) {
        logs = logs + msg
    }

    val observer = remember {
        object : PeerConnection.Observer {
            override fun onSignalingChange(state: PeerConnection.SignalingState?) { log("Signaling: $state") }
            override fun onIceConnectionChange(state: PeerConnection.IceConnectionState?) { log("ICE Conn: $state") }
            override fun onIceConnectionReceivingChange(receiving: Boolean) {}
            override fun onIceGatheringChange(state: PeerConnection.IceGatheringState?) { log("ICE Gathering: $state") }
            override fun onIceCandidate(candidate: IceCandidate?) { log("ICE Candidate: ${candidate?.sdpMid}") }
            override fun onIceCandidatesRemoved(candidates: Array<out IceCandidate>?) {}
            override fun onAddStream(stream: MediaStream?) { log("Add Stream: ${stream?.id}") }
            override fun onRemoveStream(stream: MediaStream?) { log("Remove Stream") }
            override fun onDataChannel(dc: DataChannel?) { 
                log("DataChannel Received: ${dc?.label()}")
                dc?.registerObserver(object : DataChannel.Observer {
                    override fun onBufferedAmountChange(amount: Long) {}
                    override fun onStateChange() { log("Remote DC State: ${dc.state()}") }
                    override fun onMessage(buffer: DataChannel.Buffer) {
                        val data = ByteArray(buffer.data.remaining())
                        buffer.data.get(data)
                        val message = String(data)
                        log("Rx Msg: $message")
                        
                        // Dispatch to ControlService
                        ControlService.instance?.executeCommand(message) ?: run {
                            log("ControlService not connected")
                        }
                    }
                })
            }
            override fun onRenegotiationNeeded() { log("Renegotiation Needed") }
            override fun onAddTrack(receiver: RtpReceiver?, streams: Array<out MediaStream>?) { log("Add Track") }
        }
    }

    val rtcClient = remember { WebRtcClient(context, observer) }
    
    // Initialize PC on first load
    LaunchedEffect(Unit) {
        // Use Google STUN server for testing
        val iceServers = listOf(
            PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer()
        )
        rtcClient.createPeerConnection(iceServers)
        
        // Setup local data channel
        rtcClient.createDataChannel("control", object : DataChannel.Observer {
            override fun onBufferedAmountChange(amount: Long) {}
            override fun onStateChange() { log("Local DC State changed") }
            override fun onMessage(buffer: DataChannel.Buffer) {
                val data = ByteArray(buffer.data.remaining())
                buffer.data.get(data)
                log("Local DC Rx: ${String(data)}")
            }
        })
    }
    
    // Cleanup on dispose
    DisposableEffect(Unit) {
        onDispose {
            rtcClient.close()
        }
    }

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
            rtcClient.startScreenCapture(result.data!!)
            log("Screen Capture Started")
        } else {
            log("Screen Capture Permission Denied")
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Button(onClick = onBack) { Text("Back") }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = {
                screenCaptureLauncher.launch(projectionManager.createScreenCaptureIntent())
            }) { Text("Start Screen") }
            
            Button(onClick = {
                rtcClient.stopScreenCapture()
                context.stopService(Intent(context, RtcService::class.java))
                log("Screen Capture Stopped")
            }) { Text("Stop Screen") }
        }
        
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = {
                rtcClient.startAudioCapture()
                log("Audio Capture Started")
            }) { Text("Start Audio") }
            
            Button(onClick = {
                rtcClient.stopAudioCapture()
                log("Audio Capture Stopped")
            }) { Text("Stop Audio") }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = {
                rtcClient.sendMessage("Hello World ${System.currentTimeMillis()}")
                log("Msg Sent")
            }) { Text("Send Msg") }
        }
        
        Text("Protocol/Codec Switching:", style = MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("VP8", "VP9", "H264").forEach { codec ->
                Button(onClick = {
                    rtcClient.setPreferredVideoCodec(codec)
                    log("Preferred Codec set to $codec")
                    // Trigger renegotiation (Create Offer)
                    rtcClient.createOffer(object : SdpObserver {
                        override fun onCreateSuccess(desc: SessionDescription?) {
                            log("Offer Created with $codec pref")
                        }
                        override fun onSetSuccess() { log("Set Local Desc Success") }
                        override fun onCreateFailure(s: String?) { log("Create Offer Fail: $s") }
                        override fun onSetFailure(s: String?) { log("Set Local Fail: $s") }
                    })
                }) { Text(codec) }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text("Logs:", style = MaterialTheme.typography.titleMedium)
        LazyColumn {
            items(logs) { logMsg ->
                Text(logMsg, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
