package com.smnm.englishtrackingai

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.gms.ads.*
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.material.floatingactionbutton.FloatingActionButton
import org.json.JSONObject
import org.webrtc.*
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.util.*

class BuddyVideoMatchActivity : AppCompatActivity(), VideoSignalingClient.VideoListener {

    private var signalingClient: VideoSignalingClient? = null
    private var peerConnection: PeerConnection? = null
    private var localStream: MediaStream? = null
    private var videoCapturer: VideoCapturer? = null
    
    private var isConnected = false
    private var secondsPassed = 0
    private var lockTimeRemaining = 60
    private val handler = Handler(Looper.getMainLooper())
    
    private val timeoutHandler = Handler(Looper.getMainLooper())
    private val timeoutRunnable = Runnable { showNoPartnerDialog() }

    private lateinit var localView: SurfaceViewRenderer
    private lateinit var remoteView: SurfaceViewRenderer
    
    private var isMuted = false
    private var isSpeakerOn = true
    
    private var mInterstitialAd: InterstitialAd? = null

    private val liveSignalingListener = object : SignalingListener {
        override fun onConnected() {
            val name = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE).getString("user_name", "Learner") ?: "Learner"
            SignalingManager.joinPool(name, "video_match")
        }
        override fun onLiveCountUpdate(count: Int) {}
        override fun onIncomingCall(fromUserId: String, fromUserName: String, offer: String, callType: String) {}
        override fun onCallAnswered(answer: String) {}
        override fun onCallRejected() {}
        override fun onCallEnded() {}
        override fun onIceCandidateReceived(candidate: String) {}
        override fun onCallFailed(reason: String) {}
        override fun onUserListUpdated(users: List<Map<String, String>>) {}
        override fun onPageCountUpdate(count: Int) {}
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        SignalingManager.init(this, "video_match", liveSignalingListener)
        
        Thread.setDefaultUncaughtExceptionHandler { _, t ->
            val sw = StringWriter(); t.printStackTrace(PrintWriter(sw))
            try { File(filesDir, "crash_log.txt").writeText("VIDEO_CRASH:\n$sw") } catch (e: Exception) {}
            finish()
        }

        setContentView(R.layout.activity_buddy_video_match)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (isConnected && lockTimeRemaining > 0) Toast.makeText(this@BuddyVideoMatchActivity, "Lock active! ${lockTimeRemaining}s", Toast.LENGTH_SHORT).show()
                else saveStatsAndFinish()
            }
        })

        localView = findViewById(R.id.localVideoView)
        remoteView = findViewById(R.id.remoteVideoView)
        
        val hangupBtn = findViewById<FloatingActionButton>(R.id.hangupBtn)
        val reportBtn = findViewById<FloatingActionButton>(R.id.reportBtn)
        val muteBtn = findViewById<FloatingActionButton>(R.id.muteBtn)
        val speakerBtn = findViewById<FloatingActionButton>(R.id.speakerBtn)
        
        hangupBtn.isEnabled = false
        hangupBtn.alpha = 0.3f
        reportBtn.visibility = View.GONE

        hangupBtn.setOnClickListener { saveStatsAndFinish() }
        
        reportBtn.setOnClickListener {
            Toast.makeText(this, "User Reported!", Toast.LENGTH_LONG).show()
            reportBtn.isEnabled = false; reportBtn.alpha = 0.5f
        }

        muteBtn.setOnClickListener {
            isMuted = !isMuted
            localStream?.audioTracks?.forEach { it.setEnabled(!isMuted) }
            muteBtn.setImageResource(if (isMuted) android.R.drawable.ic_lock_silent_mode else android.R.drawable.ic_btn_speak_now)
            Toast.makeText(this, if (isMuted) "Mic Muted" else "Mic Active", Toast.LENGTH_SHORT).show()
        }

        speakerBtn.setOnClickListener {
            isSpeakerOn = !isSpeakerOn
            val am = getSystemService(Context.AUDIO_SERVICE) as AudioManager
            am.isSpeakerphoneOn = isSpeakerOn
            speakerBtn.setImageResource(if (isSpeakerOn) android.R.drawable.ic_lock_silent_mode_off else android.R.drawable.ic_btn_speak_now)
            Toast.makeText(this, if (isSpeakerOn) "Speaker ON" else "Speaker OFF (Earpiece)", Toast.LENGTH_SHORT).show()
        }

        loadInterstitial()

        // Start 90 Second Timeout Timer
        timeoutHandler.postDelayed(timeoutRunnable, 90000)

        if (checkPermissions()) initVideoSystem() 
        else ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO, Manifest.permission.CAMERA), 902)
    }

    private fun loadInterstitial() {
        val adRequest = AdRequest.Builder().build()
        InterstitialAd.load(this, getString(R.string.admob_interstitial_id), adRequest, object : InterstitialAdLoadCallback() {
            override fun onAdLoaded(interstitialAd: InterstitialAd) { mInterstitialAd = interstitialAd }
            override fun onAdFailedToLoad(loadAdError: LoadAdError) { mInterstitialAd = null }
        })
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK && isConnected && lockTimeRemaining > 0) return true 
        return super.onKeyDown(keyCode, event)
    }

    private fun initVideoSystem() {
        try {
            val factory = WebRTCFactory.getFactory(this)
            val eglContext = WebRTCFactory.getEglContext()
            runOnUiThread { localView.init(eglContext, null); localView.setZOrderMediaOverlay(true); remoteView.init(eglContext, null) }
            localStream = factory.createLocalMediaStream("VIDEO_STREAM")
            
            val audioConstraints = MediaConstraints().apply {
                mandatory.add(MediaConstraints.KeyValuePair("googEchoCancellation", "true"))
                mandatory.add(MediaConstraints.KeyValuePair("googNoiseSuppression", "true"))
                mandatory.add(MediaConstraints.KeyValuePair("googHighpassFilter", "true"))
            }

            val videoSource = factory.createVideoSource(false)
            val enumerator = Camera2Enumerator(this)
            val deviceName = enumerator.deviceNames.find { enumerator.isFrontFacing(it) } ?: enumerator.deviceNames[0]
            videoCapturer = enumerator.createCapturer(deviceName, null)
            videoCapturer?.initialize(SurfaceTextureHelper.create("CaptureThread", eglContext), applicationContext, videoSource.capturerObserver)
            videoCapturer?.startCapture(640, 480, 30)
            
            localStream?.addTrack(factory.createVideoTrack("v0", videoSource).apply { addSink(localView) })
            localStream?.addTrack(factory.createAudioTrack("a0", factory.createAudioSource(audioConstraints)))
            
            // Re-Initialize Socket.IO Signaling for Fast Matching
            signalingClient = VideoSignalingClient(this, this)
            handler.postDelayed({ if (!isFinishing) signalingClient?.findBuddy("Any") }, 400)
        } catch (e: Exception) { Log.e("VideoInit", e.message ?: "Error") }
    }

    private fun setupPeerConnection() {
        if (peerConnection != null) return
        try {
            val factory = WebRTCFactory.getFactory(this)
            val iceServers = listOf(
                PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer(),
                PeerConnection.IceServer.builder(ConfigManager.turnUrl).setUsername(ConfigManager.turnUser).setPassword(ConfigManager.turnPass).createIceServer()
            )
            val rtcConfig = PeerConnection.RTCConfiguration(iceServers).apply {
                sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN
                continualGatheringPolicy = PeerConnection.ContinualGatheringPolicy.GATHER_CONTINUALLY
            }
            peerConnection = factory.createPeerConnection(rtcConfig, object : PeerConnection.Observer {
                override fun onIceCandidate(c: IceCandidate) {
                    val s = JSONObject().apply { put("candidate", JSONObject().apply { put("sdpMid", c.sdpMid); put("sdpMLineIndex", c.sdpMLineIndex); put("candidate", c.sdp) }) }
                    signalingClient?.sendSignal(s)
                }
                override fun onAddStream(st: MediaStream) { runOnUiThread { if (!isFinishing && st.videoTracks.isNotEmpty()) { st.videoTracks[0].addSink(remoteView); onConnectedUI() } } }
                override fun onIceConnectionChange(s: PeerConnection.IceConnectionState?) { 
                    if (s == PeerConnection.IceConnectionState.CONNECTED || s == PeerConnection.IceConnectionState.COMPLETED) runOnUiThread { onConnectedUI() } 
                }
                override fun onSignalingChange(p0: PeerConnection.SignalingState?) {}
                override fun onIceConnectionReceivingChange(p0: Boolean) {}
                override fun onIceGatheringChange(p0: PeerConnection.IceGatheringState?) {}
                override fun onIceCandidatesRemoved(p0: Array<out IceCandidate>?) {}
                override fun onRemoveStream(p0: MediaStream?) {}
                override fun onDataChannel(p0: DataChannel?) {}
                override fun onRenegotiationNeeded() {}
                override fun onAddTrack(p0: RtpReceiver?, p1: Array<out MediaStream>?) {}
            })
            localStream?.videoTracks?.forEach { peerConnection?.addTrack(it, listOf("VIDEO_STREAM")) }
            localStream?.audioTracks?.forEach { peerConnection?.addTrack(it, listOf("VIDEO_STREAM")) }
        } catch (e: Exception) { Log.e("PCSetup", e.message ?: "Error") }
    }

    private fun onConnectedUI() {
        if (isConnected) return
        isConnected = true
        timeoutHandler.removeCallbacks(timeoutRunnable) // Match found, cancel timer
        runOnUiThread {
            if (!isFinishing) {
                decrementDeviceLimit()
                val am = getSystemService(Context.AUDIO_SERVICE) as AudioManager
                am.mode = AudioManager.MODE_IN_COMMUNICATION
                am.isSpeakerphoneOn = true 
                
                findViewById<TextView>(R.id.callStatus).text = "Connected! 🎥"
                findViewById<FloatingActionButton>(R.id.reportBtn).visibility = View.VISIBLE
                startTimer()
                startLockCountdown()
            }
        }
    }

    private fun decrementDeviceLimit() {
        val hwId = Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID) ?: "unknown"
        val prefs = getSharedPreferences("DeviceUsagePrefs", Context.MODE_PRIVATE)
        val currentCount = prefs.getInt("usage_video_$hwId", 0)
        prefs.edit().putInt("usage_video_$hwId", currentCount + 1).apply()
    }

    private fun startLockCountdown() {
        handler.post(object : Runnable {
            override fun run() {
                if (lockTimeRemaining > 0) {
                    lockTimeRemaining--
                    findViewById<TextView>(R.id.lockStatusText).apply { visibility = View.VISIBLE; text = "Hang up locked for ${lockTimeRemaining}s" }
                    handler.postDelayed(this, 1000)
                } else {
                    findViewById<FloatingActionButton>(R.id.hangupBtn).apply { isEnabled = true; alpha = 1.0f }
                    findViewById<TextView>(R.id.lockStatusText).visibility = View.GONE
                }
            }
        })
    }

    override fun onMatched(i: Boolean) {
        runOnUiThread { if (!isFinishing) { setupPeerConnection(); if (i) peerConnection?.createOffer(object : AppSdpObserver() { override fun onCreateSuccess(s: SessionDescription?) { peerConnection?.setLocalDescription(this, s); signalingClient?.sendSignal(JSONObject().apply { put("sdp", JSONObject().apply { put("type", "offer"); put("sdp", s?.description) }) }) } }, MediaConstraints()) } }
    }

    override fun onSdpReceived(d: SessionDescription) {
        runOnUiThread { if (isFinishing) return@runOnUiThread; setupPeerConnection(); peerConnection?.setRemoteDescription(object : AppSdpObserver() { override fun onSetSuccess() { if (d.type == SessionDescription.Type.OFFER) peerConnection?.createAnswer(object : AppSdpObserver() { override fun onCreateSuccess(a: SessionDescription?) { peerConnection?.setLocalDescription(this, a); signalingClient?.sendSignal(JSONObject().apply { put("sdp", JSONObject().apply { put("type", "answer"); put("sdp", a?.description) }) }) } }, MediaConstraints()) } }, d) }
    }

    override fun onIceReceived(c: IceCandidate) { runOnUiThread { if (!isFinishing) peerConnection?.addIceCandidate(c) } }
    override fun onDisconnected() { runOnUiThread { 
        Toast.makeText(this@BuddyVideoMatchActivity, "Partner Left", Toast.LENGTH_SHORT).show()
        saveStatsAndFinish() 
    } }

    private fun startTimer() {
        handler.post(object : Runnable { override fun run() { if (isConnected && !isFinishing) { secondsPassed++; handler.postDelayed(this, 1000) } } })
    }

    private fun saveStatsAndFinish() {
        val hwId = Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID) ?: "unknown"
        val prefs = getSharedPreferences("DeviceUsagePrefs", Context.MODE_PRIVATE)
        val historyKey = "call_durations_$hwId"
        val history = prefs.getString(historyKey, "") ?: ""
        val newHistory = (if (history.isEmpty()) "" else "$history,") + secondsPassed
        val limitedHistory = newHistory.split(",").takeLast(5).joinToString(",")
        val editor = prefs.edit()
        editor.putString(historyKey, limitedHistory)
        val durations = limitedHistory.split(",").filter { it.isNotEmpty() }.filter { it.all { c -> c.isDigit() } }.map { it.toInt() }
        if (durations.size >= 5 && durations.average() < 180) editor.putLong("ban_until_$hwId", System.currentTimeMillis() + (24 * 60 * 60 * 1000))
        editor.apply()

        if (mInterstitialAd != null && secondsPassed > 30) {
            mInterstitialAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() { finish() }
                override fun onAdFailedToShowFullScreenContent(adError: AdError) { finish() }
            }
            mInterstitialAd?.show(this)
        } else {
            finish()
        }
    }

    private fun showNoPartnerDialog() {
        if (isFinishing || isConnected) return
        try {
            androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Partner Not Found")
                .setMessage("No learners are available at the moment. Please try again after a few minutes.")
                .setCancelable(false)
                .setPositiveButton("Back to Dashboard") { _, _ -> finish() }
                .show()
        } catch (e: Exception) { finish() }
    }

    private fun checkPermissions() = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) { super.onRequestPermissionsResult(requestCode, permissions, grantResults) ; if (grantResults.all { it == PackageManager.PERMISSION_GRANTED }) initVideoSystem() else finish() }
    override fun onResume() {
        super.onResume()
        val name = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE).getString("user_name", "Learner") ?: "Learner"
        SignalingManager.joinPool(name, "video_match")
    }

    override fun onPause() {
        super.onPause()
        SignalingManager.leavePool()
    }

    override fun onDestroy() { 
        isConnected = false
        timeoutHandler.removeCallbacks(timeoutRunnable)
        try { videoCapturer?.stopCapture(); videoCapturer?.dispose(); peerConnection?.close(); signalingClient?.destroy() } catch (e: Exception) {}
        SignalingManager.removeListener(liveSignalingListener)
        super.onDestroy() 
    }
}
