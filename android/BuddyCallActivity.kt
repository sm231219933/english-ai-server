package com.smnm.englishtrackingai

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.floatingactionbutton.FloatingActionButton
import org.json.JSONObject
import org.webrtc.*
import java.util.*

class BuddyCallActivity : AppCompatActivity(), AudioSignalingClient.AudioListener {

    private var signalingClient: AudioSignalingClient? = null
    private var peerConnection: PeerConnection? = null
    private var factory: PeerConnectionFactory? = null
    private var localStream: MediaStream? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_buddy_call)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        if (checkPermissions()) initAudioCall() 
        else ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), 601)

        findViewById<FloatingActionButton>(R.id.hangupBtn).setOnClickListener { finish() }
    }

    private fun initAudioCall() {
        PeerConnectionFactory.initialize(PeerConnectionFactory.InitializationOptions.builder(this).createInitializationOptions())
        factory = PeerConnectionFactory.builder().createPeerConnectionFactory()
        
        localStream = factory?.createLocalMediaStream("ARDAMS")
        localStream?.addTrack(factory?.createAudioTrack("audio0", factory?.createAudioSource(MediaConstraints())))
        
        signalingClient = AudioSignalingClient(this, this)
        signalingClient?.findBuddy("Any")
    }

    private fun setupPeerConnection() {
        val iceServers = listOf(
            PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer(),
            PeerConnection.IceServer.builder(ConfigManager.turnUrl).setUsername(ConfigManager.turnUser).setPassword(ConfigManager.turnPass).createIceServer()
        )
        peerConnection = factory?.createPeerConnection(PeerConnection.RTCConfiguration(iceServers), object : PeerConnection.Observer {
            override fun onIceCandidate(candidate: IceCandidate) {
                val signal = JSONObject().apply { put("candidate", JSONObject().apply { put("sdpMid", candidate.sdpMid); put("sdpMLineIndex", candidate.sdpMLineIndex); put("candidate", candidate.sdp) }) }
                signalingClient?.sendSignal(signal)
            }
            override fun onIceConnectionChange(state: PeerConnection.IceConnectionState?) {
                if (state == PeerConnection.IceConnectionState.CONNECTED) {
                    runOnUiThread { 
                        findViewById<TextView>(R.id.callStatus).text = "Connected! 🟢" 
                        findViewById<TextView>(R.id.callTimer).visibility = android.view.View.VISIBLE
                    }
                }
            }
            override fun onAddStream(p0: MediaStream?) {}
            override fun onSignalingChange(p0: PeerConnection.SignalingState?) {}
            override fun onIceConnectionReceivingChange(p0: Boolean) {}
            override fun onIceGatheringChange(p0: PeerConnection.IceGatheringState?) {}
            override fun onIceCandidatesRemoved(p0: Array<out IceCandidate>?) {}
            override fun onRemoveStream(p0: MediaStream?) {}
            override fun onDataChannel(p0: DataChannel?) {}
            override fun onRenegotiationNeeded() {}
            override fun onAddTrack(p0: RtpReceiver?, p1: Array<out MediaStream>?) {}
        })
        peerConnection?.addStream(localStream)
    }

    override fun onMatched(isInitiator: Boolean) {
        runOnUiThread { findViewById<TextView>(R.id.callStatus).text = "Buddy Found! ✅" }
        setupPeerConnection()
        if (isInitiator) {
            peerConnection?.createOffer(object : SimpleSdpObserver() {
                override fun onCreateSuccess(sdp: SessionDescription?) {
                    peerConnection?.setLocalDescription(this, sdp)
                    val signal = JSONObject().apply { put("sdp", JSONObject().apply { put("type", "offer"); put("sdp", sdp?.description) }) }
                    signalingClient?.sendSignal(signal)
                }
            }, MediaConstraints())
        }
    }

    override fun onSdpReceived(description: SessionDescription) {
        if (peerConnection == null) setupPeerConnection()
        peerConnection?.setRemoteDescription(object : SimpleSdpObserver() {
            override fun onSetSuccess() {
                if (description.type == SessionDescription.Type.OFFER) {
                    peerConnection?.createAnswer(object : SimpleSdpObserver() {
                        override fun onCreateSuccess(ans: SessionDescription?) {
                            peerConnection?.setLocalDescription(this, ans)
                            val signal = JSONObject().apply { put("sdp", JSONObject().apply { put("type", "answer"); put("sdp", ans?.description) }) }
                            signalingClient?.sendSignal(signal)
                        }
                    }, MediaConstraints())
                }
            }
        }, description)
    }

    override fun onIceReceived(candidate: IceCandidate) { peerConnection?.addIceCandidate(candidate) }
    override fun onDisconnected() { runOnUiThread { Toast.makeText(this, "Buddy Left", Toast.LENGTH_SHORT).show(); finish() } }

    private fun checkPermissions() = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED

    override fun onDestroy() {
        super.onDestroy()
        peerConnection?.close(); signalingClient?.destroy(); factory?.dispose()
    }

    open class SimpleSdpObserver : org.webrtc.SdpObserver {
        override fun onCreateSuccess(p0: SessionDescription?) {}
        override fun onSetSuccess() {}
        override fun onCreateFailure(p0: String?) {}
        override fun onSetFailure(p0: String?) {}
    }
}
