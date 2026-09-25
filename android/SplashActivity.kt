package com.smnm.englishtrackingai

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.VideoView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class SplashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        
        
        // SYSTEM THEME SUPPORT: Removed forced light mode to follow system settings.

        setContentView(R.layout.activity_splash)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val videoView = findViewById<VideoView>(R.id.splashVideoView)
        
        // SAFETY FALLBACK: If video fails or takes too long, move to Home automatically
        val safetyHandler = Handler(Looper.getMainLooper())
        val safetyRunnable = Runnable { moveToHome() }
        safetyHandler.postDelayed(safetyRunnable, 6000) // 6 second max wait

        try {
            // Path to your 5-second video file (res/raw/splash_video.mp4)
            val path = "android.resource://" + packageName + "/" + R.raw.splash_video
            videoView.setVideoURI(Uri.parse(path))

            videoView.setOnErrorListener { _, _, _ ->
                safetyHandler.removeCallbacks(safetyRunnable)
                moveToHome()
                true
            }

            videoView.setOnCompletionListener {
                safetyHandler.removeCallbacks(safetyRunnable)
                moveToHome()
            }

            videoView.start()
        } catch (e: Exception) {
            safetyHandler.removeCallbacks(safetyRunnable)
            moveToHome()
        }
    }

    private fun moveToHome() {
        val user = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
        val target = if (user != null) HomeActivity::class.java else LoginActivity::class.java
        startActivity(Intent(this, target))
        finish()
    }
}