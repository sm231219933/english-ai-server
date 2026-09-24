package com.smnm.englishtrackingai

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.MobileAds
import java.io.File

class HomeActivity : AppCompatActivity() {

    private lateinit var profileBtn: Button
    private lateinit var greetTxt: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_home)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        profileBtn = findViewById(R.id.homeProfileBtn)
        greetTxt = findViewById(R.id.homeGreeting)

        // --- FETCH REMOTE CONFIG ---
        ConfigManager.fetchConfig {
            runOnUiThread {
                loadBannerAds()
            }
        }

        // --- SAFE ADMOB INIT ---
        try {
            MobileAds.initialize(this) {}
        } catch (e: Exception) {}

        // --- CHECK FOR PREVIOUS CRASH REPORT ---
        checkForPreviousCrash()

        findViewById<CardView>(R.id.cardTextChat).setOnClickListener {
            val intent = Intent(this, BuddyActivity::class.java)
            intent.putExtra("matchMode", "text")
            startActivity(intent)
        }

        findViewById<CardView>(R.id.cardAudioCall).setOnClickListener {
            val intent = Intent(this, BuddyActivity::class.java)
            intent.putExtra("matchMode", "audio")
            startActivity(intent)
        }

        findViewById<CardView>(R.id.cardVideoCall).setOnClickListener {
            val intent = Intent(this, BuddyActivity::class.java)
            intent.putExtra("matchMode", "video")
            startActivity(intent)
        }

        findViewById<CardView>(R.id.cardVocab).setOnClickListener {
            startActivity(Intent(this, VocabActivity::class.java))
        }

        profileBtn.setOnClickListener {
            val prefs = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
            val email = prefs.getString("user_email", "")
            if (email.isNullOrEmpty()) {
                startActivity(Intent(this, LoginActivity::class.java))
            } else {
                startActivity(Intent(this, ProfileActivity::class.java))
            }
        }

        findViewById<Button>(R.id.homeSubBtn).apply {
            // SHOW BUTTON IF NOT VIP
            val isVip = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE).getBoolean("is_vip", false)
            visibility = if (isVip) View.GONE else View.VISIBLE
            text = "🚀 GO AD-FREE"
            setOnClickListener {
                startActivity(Intent(this@HomeActivity, VipActivity::class.java))
            }
        }

        findViewById<Button>(R.id.btnShareApp).setOnClickListener {
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, "Download Tinkl")
                putExtra(Intent.EXTRA_TEXT, "Join me on Tinkl to practice English with buddies! Download here: https://play.google.com/store/apps/details?id=$packageName")
            }
            startActivity(Intent.createChooser(shareIntent, "Share via"))
        }

        findViewById<Button>(R.id.btnTelegram).setOnClickListener {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/Tinklapp")))
        }

        findViewById<Button>(R.id.btnFeedback).setOnClickListener {
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("mailto:smnmapps@gmail.com")
                putExtra(Intent.EXTRA_SUBJECT, "Tinkl App Feedback")
            }
            try { startActivity(intent) } catch (e: Exception) { Toast.makeText(this, "No email app found", Toast.LENGTH_SHORT).show() }
        }

        checkSessionAndUpdateUI()
    }

    private fun loadBannerAds() {
        if (!ConfigManager.shouldShowBannerAds(this)) return

        try {
            val adContainerMid = findViewById<LinearLayout>(R.id.homeAdContainerMid)
            if (adContainerMid != null) {
                val adViewMid = AdView(this)
                adViewMid.adUnitId = getString(R.string.admob_banner_id)
                adViewMid.setAdSize(AdSize.BANNER)
                adContainerMid.removeAllViews()
                adContainerMid.addView(adViewMid)
                adViewMid.loadAd(AdRequest.Builder().build())
            }
        } catch (e: Exception) {}
    }

    private fun checkForPreviousCrash() {
        try {
            val file = File(filesDir, "crash_log.txt")
            if (file.exists()) {
                val errorMsg = file.readText()
                AlertDialog.Builder(this)
                    .setTitle("⚠️ App Crash Detected")
                    .setMessage("App pichli baar is wajah se band hui thi:\n\n$errorMsg")
                    .setCancelable(false)
                    .setPositiveButton("OK") { _, _ -> file.delete() }
                    .show()
            }
        } catch (e: Exception) {}
    }

    private fun checkSessionAndUpdateUI() {
        val prefs = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
        val name = prefs.getString("user_name", "Learner")
        val email = prefs.getString("user_email", "")

        if (email.isNullOrEmpty()) {
            greetTxt.text = "Welcome Guest 👋"
            profileBtn.text = "LOGIN"
        } else {
            greetTxt.text = "Hello $name 👋"
            profileBtn.text = "PROFILE"
        }
    }

    override fun onResume() {
        super.onResume()
        checkSessionAndUpdateUI()
        
        // Update VIP Button visibility on resume
        val isVip = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE).getBoolean("is_vip", false)
        findViewById<Button>(R.id.homeSubBtn).visibility = if (isVip) View.GONE else View.VISIBLE
    }
}
