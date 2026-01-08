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
    private var localAudioTrack: AudioTrack? = null

    init {
        initPeerConnectionFactory(context)
        peerConnectionFactory = createPeerConnectionFactory()
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
                peerConnection?.setLocalDescription(object : SdpObserver by callback {}, desc)
                callback.onCreateSuccess(desc)
            }
        }, constraints)
    }

    fun createAnswer(callback: SdpObserver) {
        val constraints = MediaConstraints()
        peerConnection?.createAnswer(object : SdpObserver by callback {
            override fun onCreateSuccess(desc: SessionDescription?) {
                peerConnection?.setLocalDescription(object : SdpObserver by callback {}, desc)
                callback.onCreateSuccess(desc)
            }
        }, constraints)
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
