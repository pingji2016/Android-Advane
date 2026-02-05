package com.example.rtc

import android.util.Log
import okhttp3.*
import org.json.JSONObject
import org.webrtc.IceCandidate
import org.webrtc.SessionDescription

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
    private val client = OkHttpClient()
    private val TAG = "SignalingClient"

    fun connect() {
        val request = Request.Builder().url(serverUrl).build()
        webSocket = client.newWebSocket(request, this)
    }

    fun joinRoom(roomId: String) {
        val json = JSONObject().apply {
            put("type", "join")
            put("roomId", roomId)
        }
        sendMessage(json)
    }

    fun sendOffer(description: SessionDescription) {
        val json = JSONObject().apply {
            put("type", "offer")
            put("payload", JSONObject().apply {
                put("type", "offer")
                put("sdp", description.description)
            })
        }
        sendMessage(json)
    }

    fun sendAnswer(description: SessionDescription) {
        val json = JSONObject().apply {
            put("type", "answer")
            put("payload", JSONObject().apply {
                put("type", "answer")
                put("sdp", description.description)
            })
        }
        sendMessage(json)
    }

    fun sendIceCandidate(candidate: IceCandidate) {
        val json = JSONObject().apply {
            put("type", "candidate")
            put("payload", JSONObject().apply {
                put("sdpMid", candidate.sdpMid)
                put("sdpMLineIndex", candidate.sdpMLineIndex)
                put("candidate", candidate.sdp)
            })
        }
        sendMessage(json)
    }

    private fun sendMessage(json: JSONObject) {
        val text = json.toString()
        Log.d(TAG, "Sending: $text")
        webSocket?.send(text)
    }

    fun close() {
        webSocket?.close(1000, "Bye")
    }

    override fun onOpen(webSocket: WebSocket, response: Response) {
        Log.d(TAG, "Connected to signaling server")
        listener.onConnectionEstablished()
    }

    override fun onMessage(webSocket: WebSocket, text: String) {
        Log.d(TAG, "Received: $text")
        try {
            val json = JSONObject(text)
            val type = json.optString("type")

            when (type) {
                "joined" -> {
                    listener.onJoined(json.optString("roomId"))
                }
                "ready" -> {
                    listener.onRemotePeerReady()
                }
                "offer" -> {
                    val payload = json.getJSONObject("payload")
                    val sdp = payload.getString("sdp")
                    listener.onOfferReceived(SessionDescription(SessionDescription.Type.OFFER, sdp))
                }
                "answer" -> {
                    val payload = json.getJSONObject("payload")
                    val sdp = payload.getString("sdp")
                    listener.onAnswerReceived(SessionDescription(SessionDescription.Type.ANSWER, sdp))
                }
                "candidate" -> {
                    val payload = json.getJSONObject("payload")
                    val sdpMid = payload.getString("sdpMid")
                    val sdpMLineIndex = payload.getInt("sdpMLineIndex")
                    val sdp = payload.getString("candidate")
                    listener.onIceCandidateReceived(IceCandidate(sdpMid, sdpMLineIndex, sdp))
                }
                "bye" -> {
                    listener.onRemotePeerLeft()
                }
                "error" -> {
                    listener.onError(json.optString("message"))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing message", e)
        }
    }

    override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
        Log.d(TAG, "Disconnected: $reason")
    }

    override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
        Log.e(TAG, "Connection failed", t)
        listener.onError("Connection failed: ${t.message}")
    }
}
