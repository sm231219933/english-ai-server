package com.smnm.englishtrackingai

import android.Manifest
import android.app.DatePickerDialog
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var sharedPrefs: SharedPreferences

    private lateinit var userNameTextView: TextView
    private lateinit var userStatusBadge: TextView
    private lateinit var profileButton: ImageView
    private lateinit var connectBuddyButton: Button
    private lateinit var loginNavigateButton: Button
    private lateinit var levelButton: Button
    private lateinit var historyButton: Button
    private lateinit var subscriptionButton: Button
    private lateinit var vocabButton: Button
    private lateinit var testButton: Button

    private var isPaidUser = false
    private var userPlan = "Free"
    private var userLevel = "Beginner"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        sharedPrefs = getSharedPreferences("UserPrefs", MODE_PRIVATE)

        userNameTextView = findViewById(R.id.userNameTextView)
        userStatusBadge = findViewById(R.id.userStatusBadge)
        profileButton = findViewById(R.id.profileButton)
        connectBuddyButton = findViewById(R.id.connectBuddyButton)
        loginNavigateButton = findViewById(R.id.loginNavigateButton)
        levelButton = findViewById(R.id.levelButton)
        historyButton = findViewById(R.id.historyButton)
        subscriptionButton = findViewById(R.id.subscriptionButton)
        vocabButton = findViewById(R.id.vocabButton)
        testButton = findViewById(R.id.testButton)

        setupUserProfile()

        loginNavigateButton.setOnClickListener { startActivity(Intent(this, LoginActivity::class.java)) }
        profileButton.setOnClickListener { startActivity(Intent(this, ProfileActivity::class.java)) }
        levelButton.setOnClickListener { showLevelSelectionDialog() }
        historyButton.setOnClickListener { showDatePicker() }
        subscriptionButton.visibility = View.GONE
        subscriptionButton.setOnClickListener { showPremiumOptions() }
        vocabButton.setOnClickListener { startVocabActivity() }
        testButton.setOnClickListener { startTestActivity() }
        connectBuddyButton.setOnClickListener { handleBuddyCall() }
    }

    private fun setupUserProfile() {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            // Logged in via Firebase
            loginNavigateButton.visibility = View.GONE
            profileButton.visibility = View.VISIBLE
            
            // First show name from local prefs for immediate UI update
            val localName = sharedPrefs.getString("user_name", "Learner")
            userNameTextView.text = "Hello $localName 👋"

            // Fetch latest profile from Firestore
            db.collection("users").document(currentUser.uid).get().addOnSuccessListener { doc ->
                if (doc.exists()) {
                    val fireName = doc.getString("name") ?: "Learner"
                    val fireAge = doc.getString("age") ?: ""
                    val fireGender = doc.getString("gender") ?: ""
                    isPaidUser = doc.getBoolean("isPaid") ?: false
                    userPlan = doc.getString("plan") ?: "Free"
                    userLevel = doc.getString("level") ?: "Beginner"
                    
                    userNameTextView.text = "Hello $fireName 👋"
                    levelButton.text = "Level: $userLevel"
                    
                    // Sync local prefs
                    sharedPrefs.edit().apply {
                        putString("user_name", fireName)
                        putString("user_age", fireAge)
                        putString("user_gender", fireGender)
                        putString("user_plan", userPlan)
                        putString("user_level", userLevel)
                        apply()
                    }
                }
            }
        } else {
            // Guest Mode
            loginNavigateButton.visibility = View.VISIBLE
            profileButton.visibility = View.GONE
            userNameTextView.text = "Welcome Guest 👋"
        }
    }

    private fun startVocabActivity() {
        val intent = Intent(this, VocabActivity::class.java)
        intent.putExtra("level", userLevel)
        startActivity(intent)
    }

    private fun startTestActivity() {
        if (auth.currentUser == null) {
            showMandatoryLoginDialog("Login to take tests!")
            return
        }
        val intent = Intent(this, TestSelectionActivity::class.java)
        intent.putExtra("level", userLevel)
        startActivity(intent)
    }

    private fun showDatePicker() {
        if (auth.currentUser == null) { showMandatoryLoginDialog("Login first!"); return }
        val cal = Calendar.getInstance()
        DatePickerDialog(this, { _, y, m, d -> loadHistoryForDate(String.format("%04d-%02d-%02d", y, m + 1, d)) }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
    }

    private fun loadHistoryForDate(date: String) {
        // History was mainly for AI chat, but keeping the UI structure if needed later
        val uid = auth.currentUser?.uid ?: return
        db.collection("users").document(uid).collection("history").document(date).get().addOnSuccessListener { doc ->
            if (doc.exists()) {
                Toast.makeText(this, "History loaded for $date", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun handleBuddyCall() {
        val intent = Intent(this, BuddyActivity::class.java)
        startActivity(intent)
    }

    private fun showLevelSelectionDialog() {
        val levels = arrayOf("Beginner", "Intermediate", "Professional")
        AlertDialog.Builder(this).setTitle("Select Level").setItems(levels) { _, which ->
            userLevel = levels[which]
            auth.currentUser?.let { db.collection("users").document(it.uid).update("level", userLevel) }
            levelButton.text = "Level: $userLevel"
            setupUserProfile()
        }.show()
    }

    private fun showPremiumOptions(customMsg: String? = null) {
        val plans = arrayOf("Basic: ₹29/mo", "Saver: ₹79 (3-Mo)", "Ultra Pro: ₹99/mo")
        AlertDialog.Builder(this).setTitle("Upgrade 👑").setMessage(customMsg ?: "VIP Features!").setItems(plans) { _, _ -> }.setNegativeButton("Later", null).show()
    }

    private fun showMandatoryLoginDialog(msg: String) { AlertDialog.Builder(this).setTitle("Login Required").setMessage(msg).setPositiveButton("Login") { _, _ -> startActivity(Intent(this, LoginActivity::class.java)) }.show() }

    override fun onResume() { super.onResume(); setupUserProfile() }
}
