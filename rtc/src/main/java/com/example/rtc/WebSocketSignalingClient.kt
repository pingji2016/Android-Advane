package com.example.rtc

import android.os.Handler
import android.os.Looper
import android.util.Log
import okhttp3.*
import org.json.JSONObject
import org.webrtc.IceCandidate
import org.webrtc.SessionDescription
import java.util.concurrent.TimeUnit

class WebSocketSignalingClient(
    private val serverUrl: String,
    private val listener: SignalingListener
) : WebSocketListener() {

    interface SignalingListener {
        fun onConnectionEstablished()
        fun onOfferReceived(description: SessionDescription)
        fun onAnswerReceived(description: SessionDescription)
        fun onIceCandidateReceived(candidate: IceCandidate)
        fun onJoined(roomId: String)
        fun onRemotePeerReady()
        fun onRemotePeerLeft()
        fun onError(message: String)
    }

    private var webSocket: WebSocket? = null
    private val client = OkHttpClient.Builder()
        .readTimeout(0, TimeUnit.MILLISECONDS) // WebSocket requires 0 timeout
        .pingInterval(30, TimeUnit.SECONDS) // Keep-alive
        .build()
        
    private val TAG = "SignalingClient"
    private val mainHandler = Handler(Looper.getMainLooper())
    private var isConnected = false
    private var shouldReconnect = true
    private var reconnectDelay = 1000L

    fun connect() {
        shouldReconnect = true
        initConnection()
    }

    private fun initConnection() {
        if (isConnected) return
        val request = Request.Builder().url(serverUrl).build()
        webSocket = client.newWebSocket(request, this)
    }

    fun joinRoom(roomId: String) {
        sendMessage(SignalingMessage.Join(roomId))
    }

    fun sendOffer(description: SessionDescription) {
        sendMessage(SignalingMessage.Offer(description))
    }

    fun sendAnswer(description: SessionDescription) {
        sendMessage(SignalingMessage.Answer(description))
    }

    fun sendIceCandidate(candidate: IceCandidate) {
        sendMessage(SignalingMessage.Candidate(candidate))
    }

    private fun sendMessage(message: SignalingMessage) {
        val text = message.toJson().toString()
        Log.d(TAG, "Sending: $text")
        webSocket?.send(text)
    }

    fun close() {
        shouldReconnect = false
        webSocket?.close(1000, "Bye")
        webSocket = null
        isConnected = false
    }

    override fun onOpen(webSocket: WebSocket, response: Response) {
        Log.d(TAG, "Connected to signaling server")
        isConnected = true
        reconnectDelay = 1000L // Reset backoff
        notify { listener.onConnectionEstablished() }
    }

    override fun onMessage(webSocket: WebSocket, text: String) {
        Log.d(TAG, "Received: $text")
        try {
            val json = JSONObject(text)
            val type = json.optString("type")

            when (type) {
                "joined" -> {
                    val roomId = json.optString("roomId")
                    notify { listener.onJoined(roomId) }
                }
                "ready" -> {
                    notify { listener.onRemotePeerReady() }
                }
                "offer" -> {
                    val payload = json.getJSONObject("payload")
                    val sdp = payload.getString("sdp")
                    notify { listener.onOfferReceived(SessionDescription(SessionDescription.Type.OFFER, sdp)) }
                }
                "answer" -> {
                    val payload = json.getJSONObject("payload")
                    val sdp = payload.getString("sdp")
                    notify { listener.onAnswerReceived(SessionDescription(SessionDescription.Type.ANSWER, sdp)) }
                }
                "candidate" -> {
                    val payload = json.getJSONObject("payload")
                    val sdpMid = payload.getString("sdpMid")
                    val sdpMLineIndex = payload.getInt("sdpMLineIndex")
                    val sdp = payload.getString("candidate")
                    notify { listener.onIceCandidateReceived(IceCandidate(sdpMid, sdpMLineIndex, sdp)) }
                }
                "bye" -> {
                    notify { listener.onRemotePeerLeft() }
                }
                "error" -> {
                    val msg = json.optString("message")
                    notify { listener.onError(msg) }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing message", e)
        }
    }

    override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
        Log.d(TAG, "Disconnected: $reason")
        isConnected = false
        if (shouldReconnect) {
            scheduleReconnect()
        }
    }

    override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
        Log.e(TAG, "Connection failed", t)
        isConnected = false
        notify { listener.onError("Connection failed: ${t.message}") }
        if (shouldReconnect) {
            scheduleReconnect()
        }
    }
    
    private fun scheduleReconnect() {
        Log.d(TAG, "Scheduling reconnect in ${reconnectDelay}ms")
        mainHandler.postDelayed({
            if (shouldReconnect && !isConnected) {
                reconnectDelay = (reconnectDelay * 2).coerceAtMost(60000L) // Exponential backoff max 60s
                initConnection()
            }
        }, reconnectDelay)
    }
    
    // Helper to run on Main Thread
    private fun notify(block: () -> Unit) {
        mainHandler.post(block)
    }
}
