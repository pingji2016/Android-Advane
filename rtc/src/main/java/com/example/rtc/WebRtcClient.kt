package com.example.rtc

import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjection
import org.webrtc.*

class WebRtcClient(
    private val context: Context,
    private val observer: PeerConnection.Observer
) {
    private val rootEglBase: EglBase = EglBase.create()
    private val peerConnectionFactory: PeerConnectionFactory
    private var peerConnection: PeerConnection? = null
    private var videoCapturer: VideoCapturer? = null
    private var videoSource: VideoSource? = null
    private var videoTrack: VideoTrack? = null
    private var audioSource: AudioSource? = null
    private var localAudioTrack: AudioTrack? = null
    private var dataChannel: DataChannel? = null
    private var preferredVideoCodec: String? = null

    init {
        initPeerConnectionFactory(context)
        peerConnectionFactory = createPeerConnectionFactory()
    }

    fun setPreferredVideoCodec(codec: String) {
        preferredVideoCodec = codec
    }

    fun getEglBaseContext(): EglBase.Context {
        return rootEglBase.eglBaseContext
    }

    private fun initPeerConnectionFactory(context: Context) {
        val options = PeerConnectionFactory.InitializationOptions.builder(context)
            .setEnableInternalTracer(true)
            .setFieldTrials("WebRTC-H264HighProfile/Enabled/")
            .createInitializationOptions()
        PeerConnectionFactory.initialize(options)
    }

    private fun createPeerConnectionFactory(): PeerConnectionFactory {
        val options = PeerConnectionFactory.Options()
        val encoderFactory = DefaultVideoEncoderFactory(rootEglBase.eglBaseContext, true, true)
        val decoderFactory = DefaultVideoDecoderFactory(rootEglBase.eglBaseContext)
        
        return PeerConnectionFactory.builder()
            .setOptions(options)
            .setVideoEncoderFactory(encoderFactory)
            .setVideoDecoderFactory(decoderFactory)
            .createPeerConnectionFactory()
    }
    
    fun createPeerConnection(iceServers: List<PeerConnection.IceServer>) {
        val rtcConfig = PeerConnection.RTCConfiguration(iceServers)
        rtcConfig.sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN
        peerConnection = peerConnectionFactory.createPeerConnection(rtcConfig, observer)
    }

    fun startAudioCapture() {
        val constraints = MediaConstraints()
        audioSource = peerConnectionFactory.createAudioSource(constraints)
        localAudioTrack = peerConnectionFactory.createAudioTrack("ARDAMSa0", audioSource)
        peerConnection?.addTrack(localAudioTrack, listOf("ARDAMS"))
    }

    fun stopAudioCapture() {
        localAudioTrack?.dispose()
        localAudioTrack = null
        audioSource?.dispose()
        audioSource = null
    }

    fun createDataChannel(label: String, observer: DataChannel.Observer) {
        val init = DataChannel.Init()
        dataChannel = peerConnection?.createDataChannel(label, init)
        dataChannel?.registerObserver(observer)
    }

    fun sendMessage(message: String) {
        val buffer = DataChannel.Buffer(java.nio.ByteBuffer.wrap(message.toByteArray()), false)
        dataChannel?.send(buffer)
    }

    fun startScreenCapture(permissionResultData: Intent, width: Int = 720, height: Int = 1280, fps: Int = 30) {
        videoCapturer = createScreenCapturer(permissionResultData)
        
        videoSource = peerConnectionFactory.createVideoSource(videoCapturer!!.isScreencast)
        videoCapturer!!.initialize(SurfaceTextureHelper.create("CaptureThread", rootEglBase.eglBaseContext), context, videoSource!!.capturerObserver)
        videoCapturer!!.startCapture(width, height, fps)

        videoTrack = peerConnectionFactory.createVideoTrack("ARDAMSv0", videoSource)
        
        // Add track to PeerConnection
        peerConnection?.addTrack(videoTrack, listOf("ARDAMS"))
    }

    fun stopScreenCapture() {
        videoCapturer?.stopCapture()
        videoCapturer?.dispose()
        videoCapturer = null
        
        videoSource?.dispose()
        videoSource = null
        
        videoTrack?.dispose()
        videoTrack = null
    }

    private fun createScreenCapturer(resultData: Intent): VideoCapturer {
        return ScreenCapturerAndroid(resultData, object : MediaProjection.Callback() {
            override fun onStop() {
                super.onStop()
                // Handle projection stop
            }
        })
    }

    fun createOffer(callback: SdpObserver) {
        val constraints = MediaConstraints()
        peerConnection?.createOffer(object : SdpObserver by callback {
            override fun onCreateSuccess(desc: SessionDescription?) {
                val newDesc = if (desc != null) usePreferredCodec(desc) else desc
                peerConnection?.setLocalDescription(object : SdpObserver by callback {}, newDesc)
                callback.onCreateSuccess(newDesc)
            }
        }, constraints)
    }

    fun createAnswer(callback: SdpObserver) {
        val constraints = MediaConstraints()
        peerConnection?.createAnswer(object : SdpObserver by callback {
            override fun onCreateSuccess(desc: SessionDescription?) {
                val newDesc = if (desc != null) usePreferredCodec(desc) else desc
                peerConnection?.setLocalDescription(object : SdpObserver by callback {}, newDesc)
                callback.onCreateSuccess(newDesc)
            }
        }, constraints)
    }

    private fun usePreferredCodec(sessionDescription: SessionDescription): SessionDescription {
        if (preferredVideoCodec == null) return sessionDescription
        val newSdp = preferCodec(sessionDescription.description, preferredVideoCodec!!, true)
        return SessionDescription(sessionDescription.type, newSdp)
    }

    private fun preferCodec(sdpDescription: String, codec: String, isVideo: Boolean): String {
        val lines = sdpDescription.split("\r\n").toMutableList()
        val mLineIndex = lines.indexOfFirst {
            it.startsWith("m=${if (isVideo) "video" else "audio"}")
        }
        if (mLineIndex == -1) return sdpDescription

        var payloadType = -1
        val codecPattern = "a=rtpmap:(\\d+) $codec/\\d+".toRegex()

        for (line in lines) {
            val match = codecPattern.find(line)
            if (match != null) {
                payloadType = match.groupValues[1].toInt()
                break
            }
        }

        if (payloadType == -1) return sdpDescription

        val mLine = lines[mLineIndex]
        val parts = mLine.split(" ").toMutableList()
        if (parts.size > 3) {
            val payloads = parts.subList(3, parts.size)
            if (payloads.remove(payloadType.toString())) {
                payloads.add(0, payloadType.toString())
                lines[mLineIndex] = parts.subList(0, 3).joinToString(" ") + " " + payloads.joinToString(" ")
            }
        }

        return lines.joinToString("\r\n")
    }

    fun setRemoteDescription(desc: SessionDescription, callback: SdpObserver) {
        peerConnection?.setRemoteDescription(callback, desc)
    }

    fun addIceCandidate(candidate: IceCandidate) {
        peerConnection?.addIceCandidate(candidate)
    }
    
    fun close() {
        peerConnection?.close()
        peerConnection = null
        peerConnectionFactory.dispose()
        rootEglBase.release()
    }
}
