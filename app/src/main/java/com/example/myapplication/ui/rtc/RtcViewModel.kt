package com.example.myapplication.ui.rtc

import android.app.Application
import android.content.Intent
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.service.ControlService
import com.example.rtc.WebSocketSignalingClient
import com.example.rtc.WebRtcClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.webrtc.*

data class RtcUiState(
    val logs: List<String> = emptyList(),
    val connectionStatus: String = "Disconnected",
    val serverUrl: String = "ws://10.0.2.2:8080",
    val roomId: String = "1234",
    val isConnected: Boolean = false,
    val isScreenCapturing: Boolean = false,
    val isAudioCapturing: Boolean = false
)

class RtcViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(RtcUiState())
    val uiState: StateFlow<RtcUiState> = _uiState.asStateFlow()

    private var rtcClient: WebRtcClient? = null
    private var signalingClient: WebSocketSignalingClient? = null
    
    // We keep a reference to the Observer to pass to WebRtcClient
    private val peerObserver = object : PeerConnection.Observer {
        override fun onSignalingChange(state: PeerConnection.SignalingState?) { log("Signaling: $state") }
        override fun onIceConnectionChange(state: PeerConnection.IceConnectionState?) { log("ICE Conn: $state") }
        override fun onIceConnectionReceivingChange(receiving: Boolean) {}
        override fun onIceGatheringChange(state: PeerConnection.IceGatheringState?) { log("ICE Gathering: $state") }
        override fun onIceCandidate(candidate: IceCandidate?) { 
            log("Local ICE Candidate generated")
            candidate?.let { signalingClient?.sendIceCandidate(it) }
        }
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

    private val signalingListener = object : WebSocketSignalingClient.SignalingListener {
        override fun onConnectionEstablished() {
            log("Signaling Connected")
            _uiState.update { it.copy(connectionStatus = "Connected") }
            signalingClient?.joinRoom(_uiState.value.roomId)
        }
        override fun onJoined(id: String) {
            log("Joined Room: $id")
            _uiState.update { it.copy(connectionStatus = "Joined: $id") }
        }
        override fun onRemotePeerReady() {
            log("Remote Peer Ready - Creating Offer")
            rtcClient?.createOffer(object : SdpObserver {
                override fun onCreateSuccess(desc: SessionDescription?) {
                    desc?.let { signalingClient?.sendOffer(it) }
                    log("Offer Sent")
                }
                override fun onSetSuccess() {}
                override fun onCreateFailure(s: String?) { log("Create Offer Failed: $s") }
                override fun onSetFailure(s: String?) {}
            })
        }
        override fun onOfferReceived(description: SessionDescription) {
            log("Offer Received")
            rtcClient?.setRemoteDescription(description, object : SdpObserver {
                override fun onSetSuccess() {
                    log("Remote Offer Set - Creating Answer")
                    rtcClient?.createAnswer(object : SdpObserver {
                        override fun onCreateSuccess(desc: SessionDescription?) {
                            desc?.let { signalingClient?.sendAnswer(it) }
                            log("Answer Sent")
                        }
                        override fun onSetSuccess() {}
                        override fun onCreateFailure(s: String?) { log("Create Answer Fail: $s") }
                        override fun onSetFailure(s: String?) { log("Set Remote Offer Fail: $s") }
                    })
                }
                override fun onCreateSuccess(desc: SessionDescription?) {}
                override fun onCreateFailure(s: String?) {}
                override fun onSetFailure(s: String?) { log("Set Remote Offer Fail: $s") }
            })
        }
        override fun onAnswerReceived(description: SessionDescription) {
            log("Answer Received")
            rtcClient?.setRemoteDescription(description, object : SdpObserver {
                override fun onSetSuccess() { log("Remote Answer Set") }
                override fun onCreateSuccess(desc: SessionDescription?) {}
                override fun onCreateFailure(s: String?) {}
                override fun onSetFailure(s: String?) { log("Set Remote Answer Fail: $s") }
            })
        }
        override fun onIceCandidateReceived(candidate: IceCandidate) {
            log("ICE Candidate Rx")
            rtcClient?.addIceCandidate(candidate)
        }
        override fun onRemotePeerLeft() {
            log("Remote Peer Left")
        }
        override fun onError(message: String) {
            log("Signaling Error: $message")
        }
    }

    init {
        // Initialize WebRTC Client
        rtcClient = WebRtcClient(getApplication(), peerObserver)
        val iceServers = listOf(
            PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer()
        )
        rtcClient?.createPeerConnection(iceServers)
        
        // Setup local data channel
        rtcClient?.createDataChannel("control", object : DataChannel.Observer {
            override fun onBufferedAmountChange(amount: Long) {}
            override fun onStateChange() { log("Local DC State changed") }
            override fun onMessage(buffer: DataChannel.Buffer) {
                val data = ByteArray(buffer.data.remaining())
                buffer.data.get(data)
                log("Local DC Rx: ${String(data)}")
            }
        })
    }

    fun updateServerUrl(url: String) {
        _uiState.update { it.copy(serverUrl = url) }
    }

    fun updateRoomId(id: String) {
        _uiState.update { it.copy(roomId = id) }
    }

    fun connectSignaling() {
        if (signalingClient == null) {
            signalingClient = WebSocketSignalingClient(_uiState.value.serverUrl, signalingListener)
            signalingClient?.connect()
            _uiState.update { it.copy(isConnected = true) }
        }
    }

    fun disconnectSignaling() {
        signalingClient?.close()
        signalingClient = null
        _uiState.update { it.copy(isConnected = false, connectionStatus = "Disconnected") }
    }

    fun startScreenCapture(intent: Intent) {
        rtcClient?.startScreenCapture(intent)
        _uiState.update { it.copy(isScreenCapturing = true) }
        log("Screen Capture Started")
    }

    fun stopScreenCapture() {
        rtcClient?.stopScreenCapture()
        _uiState.update { it.copy(isScreenCapturing = false) }
        log("Screen Capture Stopped")
    }

    fun startAudioCapture() {
        rtcClient?.startAudioCapture()
        _uiState.update { it.copy(isAudioCapturing = true) }
        log("Audio Capture Started")
    }

    fun stopAudioCapture() {
        rtcClient?.stopAudioCapture()
        _uiState.update { it.copy(isAudioCapturing = false) }
        log("Audio Capture Stopped")
    }

    fun sendMessage(msg: String) {
        rtcClient?.sendMessage(msg)
        log("Msg Sent: $msg")
    }
    
    fun setPreferredCodec(codec: String) {
        rtcClient?.setPreferredVideoCodec(codec)
        log("Preferred Codec set to $codec")
        // Trigger renegotiation
        rtcClient?.createOffer(object : SdpObserver {
            override fun onCreateSuccess(desc: SessionDescription?) {
                desc?.let { signalingClient?.sendOffer(it) }
                log("Offer Created with $codec pref")
            }
            override fun onSetSuccess() {}
            override fun onCreateFailure(s: String?) { log("Create Offer Fail: $s") }
            override fun onSetFailure(s: String?) {}
        })
    }

    private fun log(msg: String) {
        val newLogs = _uiState.value.logs + msg
        _uiState.update { it.copy(logs = newLogs) }
    }

    override fun onCleared() {
        super.onCleared()
        signalingClient?.close()
        rtcClient?.close()
    }
}
