package com.example.rtc

import org.json.JSONObject
import org.webrtc.IceCandidate
import org.webrtc.SessionDescription

// Simple data models to wrap JSON operations
sealed class SignalingMessage {
    abstract fun toJson(): JSONObject
    
    data class Join(val roomId: String) : SignalingMessage() {
        override fun toJson() = JSONObject().apply {
            put("type", "join")
            put("roomId", roomId)
        }
    }

    data class Offer(val sdp: SessionDescription) : SignalingMessage() {
        override fun toJson() = JSONObject().apply {
            put("type", "offer")
            put("payload", JSONObject().apply {
                put("type", "offer")
                put("sdp", sdp.description)
            })
        }
    }

    data class Answer(val sdp: SessionDescription) : SignalingMessage() {
        override fun toJson() = JSONObject().apply {
            put("type", "answer")
            put("payload", JSONObject().apply {
                put("type", "answer")
                put("sdp", sdp.description)
            })
        }
    }

    data class Candidate(val candidate: IceCandidate) : SignalingMessage() {
        override fun toJson() = JSONObject().apply {
            put("type", "candidate")
            put("payload", JSONObject().apply {
                put("sdpMid", candidate.sdpMid)
                put("sdpMLineIndex", candidate.sdpMLineIndex)
                put("candidate", candidate.sdp)
            })
        }
    }
}
