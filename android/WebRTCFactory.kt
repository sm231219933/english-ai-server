package com.smnm.englishtrackingai

import android.content.Context
import org.webrtc.*

object WebRTCFactory {
    private var factory: PeerConnectionFactory? = null
    private var rootEglBase: EglBase? = null

    @Synchronized
    fun getFactory(context: Context): PeerConnectionFactory {
        if (factory == null) {
            PeerConnectionFactory.initialize(
                PeerConnectionFactory.InitializationOptions.builder(context.applicationContext)
                    .createInitializationOptions()
            )
            
            val options = PeerConnectionFactory.Options()
            factory = PeerConnectionFactory.builder()
                .setOptions(options)
                .setVideoEncoderFactory(DefaultVideoEncoderFactory(getEglContext(), true, true))
                .setVideoDecoderFactory(DefaultVideoDecoderFactory(getEglContext()))
                .createPeerConnectionFactory()
        }
        return factory!!
    }

    @Synchronized
    fun getEglContext(): EglBase.Context {
        if (rootEglBase == null) {
            rootEglBase = EglBase.create()
        }
        return rootEglBase!!.eglBaseContext
    }
}

// Global Standard SDP Observer to prevent Cross-Activity reference crashes
open class AppSdpObserver : SdpObserver {
    override fun onCreateSuccess(p0: SessionDescription?) {}
    override fun onSetSuccess() {}
    override fun onCreateFailure(p0: String?) {}
    override fun onSetFailure(p0: String?) {}
}
