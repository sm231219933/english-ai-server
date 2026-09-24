package com.smnm.englishtrackingai

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.view.View
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.ads.*
import java.util.*

class BuddyChatMatchActivity : AppCompatActivity(), ChatSignalingClient.ChatListener {

    private var signalingClient: ChatSignalingClient? = null
    private lateinit var chatAdapter: ChatAdapter
    private val messageList = ArrayList<ChatMessage>()
    private lateinit var recyclerView: RecyclerView
    private lateinit var inputField: EditText
    private lateinit var partnerNameTxt: TextView
    
    private val timeoutHandler = Handler(Looper.getMainLooper())
    private val timeoutRunnable = Runnable { showNoPartnerDialog() }
    
    private lateinit var speechRecognizer: SpeechRecognizer
    private var isListening = false

    private val liveSignalingListener = object : SignalingListener {
        override fun onConnected() {
            val name = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE).getString("user_name", "Learner") ?: "Learner"
            SignalingManager.joinPool(name, "chat_match")
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
        
        SignalingManager.init(this, "chat_match", liveSignalingListener)
        
        setContentView(R.layout.activity_buddy_chat_match)

        val rootLayout = findViewById<View>(R.id.chatRootLayout)
        
        ViewCompat.setOnApplyWindowInsetsListener(rootLayout) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        recyclerView = findViewById(R.id.chatRecyclerView)
        inputField = findViewById(R.id.chatInput)
        partnerNameTxt = findViewById(R.id.chatPartnerName)

        chatAdapter = ChatAdapter(messageList)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = chatAdapter

        ViewCompat.setOnApplyWindowInsetsListener(rootLayout) { v, insets ->
            val imeInsets = insets.getInsets(WindowInsetsCompat.Type.ime())
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val bottomPadding = if (imeInsets.bottom > 0) imeInsets.bottom else systemBars.bottom
            v.setPadding(0, 0, 0, bottomPadding)
            if (imeInsets.bottom > 0 && messageList.isNotEmpty()) recyclerView.postDelayed({ recyclerView.smoothScrollToPosition(messageList.size - 1) }, 100)
            insets
        }

        loadChatAd()

        // Start 90 Second Timeout Timer
        timeoutHandler.postDelayed(timeoutRunnable, 90000)

        signalingClient = ChatSignalingClient(this, this)
        signalingClient?.findBuddy()

        findViewById<ImageButton>(R.id.sendBtn).setOnClickListener {
            val msg = inputField.text.toString().trim()
            if (msg.isNotEmpty()) {
                signalingClient?.sendMessage(msg)
                addMessage(msg, true)
                inputField.text.clear()
            }
        }
        
        setupSpeechToText()
    }

    private fun setupSpeechToText() {
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
        val speechIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
        }

        speechRecognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(p0: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(p0: Float) {}
            override fun onBufferReceived(p0: ByteArray?) {}
            override fun onEndOfSpeech() { isListening = false }
            override fun onError(p0: Int) { isListening = false }
            override fun onResults(r: Bundle?) {
                val data = r?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!data.isNullOrEmpty()) inputField.append(" " + data[0])
            }
            override fun onPartialResults(p0: Bundle?) {}
            override fun onEvent(p0: Int, p1: Bundle?) {}
        })

        findViewById<ImageButton>(R.id.micBtn).setOnClickListener {
            if (!isListening) {
                speechRecognizer.startListening(speechIntent)
                isListening = true
                Toast.makeText(this, "Listening...", Toast.LENGTH_SHORT).show()
            } else {
                speechRecognizer.stopListening()
                isListening = false
            }
        }
    }

    private fun loadChatAd() {
        if (!ConfigManager.shouldShowBannerAds(this)) return
        val adContainer = findViewById<LinearLayout>(R.id.chatAdContainer) ?: return
        val adView = AdView(this)
        adView.adUnitId = getString(R.string.admob_banner_id)
        adView.setAdSize(AdSize.BANNER)
        
        adView.adListener = object : AdListener() {
            override fun onAdLoaded() {
                android.util.Log.d("AdMob", "Chat Banner Loaded")
            }
            override fun onAdFailedToLoad(error: LoadAdError) {
                android.util.Log.e("AdMob", "Chat Banner Failed: ${error.message} (Code: ${error.code})")
            }
        }
        
        adContainer.addView(adView)
        adView.loadAd(AdRequest.Builder().build())
    }

    private fun addMessage(text: String, isUser: Boolean) {
        runOnUiThread {
            messageList.add(ChatMessage(text, isUser))
            chatAdapter.notifyItemInserted(messageList.size - 1)
            recyclerView.smoothScrollToPosition(messageList.size - 1)
        }
    }

    private fun showNoPartnerDialog() {
        if (isFinishing || partnerNameTxt.text.toString().contains("Connected")) return
        try {
            androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Partner Not Found")
                .setMessage("No learners are available at the moment. Please try again after a few minutes.")
                .setCancelable(false)
                .setPositiveButton("Back to Dashboard") { _, _ -> finish() }
                .show()
        } catch (e: Exception) { finish() }
    }

    override fun onMatched() { 
        runOnUiThread { 
            timeoutHandler.removeCallbacks(timeoutRunnable) // Match found, cancel timer
            partnerNameTxt.text = "Stranger Connected ✅" 
        } 
    }
    override fun onMessageReceived(message: String) { addMessage(message, false) }
    override fun onDisconnected() { runOnUiThread { 
        Toast.makeText(this@BuddyChatMatchActivity, "Stranger Left ❌", Toast.LENGTH_SHORT).show()
        partnerNameTxt.text = "Stranger Left ❌"
        inputField.isEnabled = false 
    } }
    override fun onResume() {
        super.onResume()
        val name = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE).getString("user_name", "Learner") ?: "Learner"
        SignalingManager.joinPool(name, "chat_match")
    }

    override fun onPause() {
        super.onPause()
        SignalingManager.leavePool()
    }

    override fun onDestroy() { 
        super.onDestroy()
        timeoutHandler.removeCallbacks(timeoutRunnable)
        signalingClient?.destroy()
        speechRecognizer.destroy()
        SignalingManager.removeListener(liveSignalingListener)
    }
}
