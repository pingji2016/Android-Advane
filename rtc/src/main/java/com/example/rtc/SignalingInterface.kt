package com.example.rtc

import org.webrtc.IceCandidate
import org.webrtc.SessionDescription

interface SignalingInterface {
    fun onOfferReceived(description: SessionDescription)
    fun onAnswerReceived(description: SessionDescription)
    fun onIceCandidateReceived(candidate: IceCandidate)
    fun sendOffer(description: SessionDescription)
    fun sendAnswer(description: SessionDescription)
    fun sendIceCandidate(candidate: IceCandidate)
}
