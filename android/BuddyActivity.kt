package com.smnm.englishtrackingai

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.view.WindowManager
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.gms.ads.*
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import java.text.SimpleDateFormat
import java.util.*

class BuddyActivity : AppCompatActivity() {

    private var matchMode = "audio"
    private var rewardedAd: RewardedAd? = null
    
    private var signalingListener: SignalingListener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_buddy)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        matchMode = intent.getStringExtra("matchMode") ?: "audio"
        
        setupSignalingCount()
        
        updateUIBasedOnMode()
        loadRewardedAd()
        loadBannerAd()

        findViewById<Button>(R.id.btnGetRewardCalls).setOnClickListener {
            showRewardedAd { addRewardedCalls() }
        }

        findViewById<CardView>(R.id.startMatchBtn).setOnClickListener {
            checkQualityAndWarn {
                val selectedId = findViewById<RadioGroup>(R.id.matchGenderGroup).checkedRadioButtonId
                val prefGender = when (selectedId) {
                    R.id.rbMale -> "Male"
                    R.id.rbFemale -> "Female"
                    else -> "Any"
                }

                if (prefGender != "Any" && !isUserVip()) {
                    showPremiumFilterDialog(prefGender)
                } else {
                    if (matchMode == "text" || hasMatchesLeftOnDevice()) {
                        startMatching(prefGender)
                    }
                }
            }
        }
    }

    private fun showPremiumFilterDialog(prefGender: String) {
        val dialog = AlertDialog.Builder(this)
            .setTitle("Premium Gender Filter 👑")
            .setMessage("This filter is for VIP members. You can either watch a short ad to use it once or upgrade to VIP for unlimited ad-free access.")
            .setPositiveButton("WATCH AD") { _, _ ->
                showRewardedAd { startMatching(prefGender) }
            }
            .setNeutralButton("GO AD-FREE") { _, _ ->
                startActivity(Intent(this, VipActivity::class.java))
            }
            .setNegativeButton("CANCEL", null)
            .show()
            
        // Style buttons
        dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setTextColor(resources.getColor(R.color.blue_primary))
    }

    private fun loadBannerAd() {
        try {
            val adSlot = findViewById<FrameLayout>(R.id.matchAdSlot) ?: return
            val adView = AdView(this)
            adView.adUnitId = getString(R.string.admob_banner_id)
            adView.setAdSize(AdSize.MEDIUM_RECTANGLE)
            
            adView.adListener = object : AdListener() {
                override fun onAdLoaded() {
                    adSlot.removeAllViews()
                    adSlot.addView(adView)
                }
                override fun onAdFailedToLoad(error: LoadAdError) {
                    android.util.Log.e("AdMob", "Banner failed: ${error.message}")
                }
            }
            adView.loadAd(AdRequest.Builder().build())
        } catch (e: Exception) {}
    }

    private fun setupSignalingCount() {
        signalingListener = object : SignalingListener {
            override fun onLiveCountUpdate(count: Int) {
                runOnUiThread {
                    if (count > 0) {
                        findViewById<TextView>(R.id.liveCountText).text = "$count Learners active worldwide"
                    }
                }
            }

            override fun onPageCountUpdate(count: Int) {
                runOnUiThread {
                    if (count > 0) {
                        val modeLabel = when(matchMode) {
                            "audio" -> "Audio"
                            "video" -> "Video"
                            else -> "Chat"
                        }
                        findViewById<TextView>(R.id.liveCountText).text = "$count Active $modeLabel partners"
                    }
                }
            }
            
            override fun onUserListUpdated(users: List<Map<String, String>>) {}
            override fun onIncomingCall(f: String, n: String, o: String, t: String) {}
            override fun onCallAnswered(a: String) {}
            override fun onCallRejected() {}
            override fun onCallEnded() {}
            override fun onIceCandidateReceived(c: String) {}
            override fun onCallFailed(r: String) {}
            override fun onConnected() {}
        }
        
        signalingListener?.let { SignalingManager.init(this, matchMode, it) }
    }

    private fun getUniqueHardwareId(): String {
        return Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID) ?: "unknown_hw_id"
    }

    private fun checkQualityAndWarn(onProceed: () -> Unit) {
        val hwId = getUniqueHardwareId()
        val prefs = getSharedPreferences("DeviceUsagePrefs", Context.MODE_PRIVATE)
        val historyKey = "call_durations_$hwId"
        val warningCountKey = "warning_count_$hwId"
        
        val history = prefs.getString(historyKey, "") ?: ""
        val durations = history.split(",").filter { it.isNotEmpty() }.filter { it.all { c -> c.isDigit() } }.map { it.toInt() }
        val warningCount = prefs.getInt(warningCountKey, 0)

        if (durations.size >= 5 && durations.average() < 180 && warningCount < 2) {
            AlertDialog.Builder(this)
                .setTitle("Low Duration")
                .setMessage("Please try to talk for at least 3 minutes. Continue?")
                .setPositiveButton("PROCEED") { _, _ -> 
                    prefs.edit().putInt(warningCountKey, warningCount + 1).apply()
                    onProceed() 
                }
                .setNegativeButton("CANCEL", null)
                .show()
        } else {
            onProceed()
        }
    }

    private fun hasMatchesLeftOnDevice(): Boolean {
        if (isUserVip()) return true
        val hwId = getUniqueHardwareId()
        val prefs = getSharedPreferences("DeviceUsagePrefs", Context.MODE_PRIVATE)
        val today = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date())
        val lastReset = prefs.getString("reset_date_$hwId", "")
        
        var currentCount = prefs.getInt("usage_${matchMode}_$hwId", 0)

        if (today != lastReset) {
            currentCount = 0
            prefs.edit().putString("reset_date_$hwId", today)
                .putInt("usage_audio_$hwId", 0)
                .putInt("usage_video_$hwId", 0)
                .apply()
        }

        if (currentCount >= 5) {
            showVipDialog("Limit Reached", "Daily 5 match limit reached. Watch an ad to get 2 more!")
            return false
        }

        return true
    }

    private fun updateUIBasedOnMode() {
        val desc = findViewById<TextView>(R.id.modeDescription)
        val btnText = findViewById<TextView>(R.id.startMatchText)
        val limitText = findViewById<TextView>(R.id.limitIndicator)
        val rewardBtn = findViewById<Button>(R.id.btnGetRewardCalls)
        
        if (isUserVip() || matchMode == "text") {
            limitText.visibility = View.GONE
            rewardBtn.visibility = View.GONE
        } else {
            val hwId = getUniqueHardwareId()
            val prefs = getSharedPreferences("DeviceUsagePrefs", Context.MODE_PRIVATE)
            val count = prefs.getInt("usage_${matchMode}_$hwId", 0)
            val remaining = (5 - count).coerceAtLeast(0)
            limitText.visibility = View.VISIBLE
            limitText.text = "Daily Limit: $remaining/5 Left"
            rewardBtn.visibility = if (remaining == 0) View.VISIBLE else View.GONE
        }

        when(matchMode) {
            "text" -> {
                desc.text = "Chat with a stranger to practice your English writing skills."
                btnText.text = "⚡ START CHATTING"
            }
            "audio" -> {
                desc.text = "Voice call a random learner to improve your fluency."
                btnText.text = "⚡ START CALLING"
            }
            "video" -> {
                desc.text = "Video call a random buddy for a real face-to-face conversation."
                btnText.text = "⚡ START VIDEO MATCH"
            }
        }
    }

    private fun loadRewardedAd() {
        val adRequest = AdRequest.Builder().build()
        RewardedAd.load(this, getString(R.string.admob_rewarded_id),
            adRequest, object : RewardedAdLoadCallback() {
                override fun onAdFailedToLoad(adError: LoadAdError) { 
                    android.util.Log.e("AdMob", "Rewarded Failed: ${adError.message} (Code: ${adError.code})")
                    rewardedAd = null 
                }
                override fun onAdLoaded(ad: RewardedAd) { 
                    android.util.Log.d("AdMob", "Rewarded Ad Loaded successfully")
                    rewardedAd = ad 
                }
            })
    }

    private fun showRewardedAd(onReward: () -> Unit) {
        if (rewardedAd != null) {
            rewardedAd?.show(this) {
                onReward()
                rewardedAd = null // Clear after show
                loadRewardedAd() // Start loading next one
            }
        } else {
            Toast.makeText(this, "Ad is loading, please wait 2 seconds...", Toast.LENGTH_SHORT).show()
            loadRewardedAd()
            // Optional: You can add a small delay here and auto-trigger onReward if ad fails multiple times
        }
    }

    private fun addRewardedCalls() {
        val hwId = getUniqueHardwareId()
        val prefs = getSharedPreferences("DeviceUsagePrefs", Context.MODE_PRIVATE)
        val currentAudio = prefs.getInt("usage_audio_$hwId", 0)
        val currentVideo = prefs.getInt("usage_video_$hwId", 0)

        prefs.edit().apply {
            putInt("usage_audio_$hwId", (currentAudio - 2).coerceAtLeast(0))
            putInt("usage_video_$hwId", (currentVideo - 2).coerceAtLeast(0))
            apply()
        }
        
        Toast.makeText(this, "Bonus Calls Added! 🎉", Toast.LENGTH_LONG).show()
        updateUIBasedOnMode()
    }

    private fun startMatching(gender: String) {
        when(matchMode) {
            "text" -> startActivity(Intent(this, BuddyChatMatchActivity::class.java).putExtra("prefGender", gender))
            "audio" -> startActivity(Intent(this, BuddyAudioMatchActivity::class.java).putExtra("prefGender", gender))
            "video" -> startActivity(Intent(this, BuddyVideoMatchActivity::class.java).putExtra("prefGender", gender))
        }
    }

    private fun isUserVip(): Boolean = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE).getBoolean("is_vip", false)

    private fun showVipDialog(title: String, msg: String) {
        AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(msg)
            .setPositiveButton("UPGRADE") { _, _ -> startActivity(Intent(this, VipActivity::class.java)) }
            .setNegativeButton("NOT NOW", null)
            .show()
    }
    
    override fun onResume() {
        super.onResume()
        updateUIBasedOnMode()
        SignalingManager.joinPage("${matchMode}_page")
    }

    override fun onPause() {
        super.onPause()
        SignalingManager.leavePage("${matchMode}_page")
    }

    override fun onDestroy() {
        super.onDestroy()
        signalingListener?.let { SignalingManager.removeListener(it) }
    }
}
