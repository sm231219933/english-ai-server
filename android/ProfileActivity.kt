package com.smnm.englishtrackingai

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ProfileActivity : AppCompatActivity() {

    private lateinit var nameEt: EditText
    private lateinit var ageEt: EditText
    private lateinit var genderGroup: RadioGroup
    private lateinit var saveBtn: Button
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        
        val prefs = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
        val email = prefs.getString("user_email", "")
        
        if (email.isNullOrEmpty() && auth.currentUser == null) {
            Toast.makeText(this, "Please Login First", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        setContentView(R.layout.activity_profile)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        nameEt = findViewById(R.id.profName)
        ageEt = findViewById(R.id.profAge)
        genderGroup = findViewById(R.id.profGenderGroup)
        saveBtn = findViewById(R.id.saveProfileBtn)

        // Pre-fill from Prefs first for speed
        nameEt.setText(prefs.getString("user_name", ""))
        ageEt.setText(prefs.getString("user_age", ""))
        val gender = prefs.getString("user_gender", "")
        if (gender == "Male") findViewById<RadioButton>(R.id.profMale).isChecked = true
        else if (gender == "Female") findViewById<RadioButton>(R.id.profFemale).isChecked = true

        // Then fetch fresh data from Firestore to ensure Age is correct
        val uid = auth.currentUser?.uid
        if (uid != null) {
            db.collection("users").document(uid).get().addOnSuccessListener { doc ->
                if (doc.exists()) {
                    val fireName = doc.getString("name") ?: ""
                    val fireAge = doc.getString("age") ?: ""
                    val fireGender = doc.getString("gender") ?: ""
                    
                    nameEt.setText(fireName)
                    ageEt.setText(fireAge)
                    if (fireGender == "Male") findViewById<RadioButton>(R.id.profMale).isChecked = true
                    else if (fireGender == "Female") findViewById<RadioButton>(R.id.profFemale).isChecked = true
                    
                    // Sync local prefs if they were outdated
                    prefs.edit().apply {
                        putString("user_name", fireName)
                        putString("user_age", fireAge)
                        putString("user_gender", fireGender)
                        apply()
                    }
                }
            }
        }

        saveBtn.setOnClickListener {
            val newName = nameEt.text.toString().trim()
            val newAge = ageEt.text.toString().trim()
            val genderId = genderGroup.checkedRadioButtonId
            
            if (newName.isNotEmpty() && newAge.isNotEmpty() && genderId != -1) {
                val newGender = if (genderId == R.id.profMale) "Male" else "Female"
                
                // 1. Save locally
                prefs.edit().apply {
                    putString("user_name", newName)
                    putString("user_age", newAge)
                    putString("user_gender", newGender)
                    apply()
                }

                // 2. Save to Firebase Firestore
                val currentUserUid = auth.currentUser?.uid
                if (currentUserUid != null) {
                    val updates = mapOf(
                        "name" to newName,
                        "age" to newAge,
                        "gender" to newGender
                    )
                    db.collection("users").document(currentUserUid).update(updates)
                        .addOnSuccessListener {
                            Toast.makeText(this, "Profile Updated!✅", Toast.LENGTH_SHORT).show()
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(this, "Cloud Sync Failed: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                } else {
                    Toast.makeText(this, "Profile Updated Locally!✅", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Please fill all details", Toast.LENGTH_SHORT).show()
            }
        }

        findViewById<Button>(R.id.logoutButton).setOnClickListener {
            logoutUser()
        }

        findViewById<Button>(R.id.deleteAccountBtn).setOnClickListener {
            showDeleteConfirmation()
        }

        loadProfileAd()
    }

    private fun showDeleteConfirmation() {
        AlertDialog.Builder(this)
            .setTitle("Delete Account?")
            .setMessage("This will permanently remove all your progress and account details. This action cannot be undone.")
            .setPositiveButton("Delete") { _, _ -> deleteAccount() }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteAccount() {
        val user = auth.currentUser
        val uid = user?.uid
        
        if (uid != null) {
            // 1. Delete from Firestore
            db.collection("users").document(uid).delete()
                .addOnSuccessListener {
                    // 2. Delete Auth Account
                    user.delete().addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            Toast.makeText(this, "Account Deleted Successfully", Toast.LENGTH_LONG).show()
                            logoutUser() // Clear local data and redirect
                        } else {
                            Toast.makeText(this, "Error: ${task.exception?.message}. Please re-login and try again.", Toast.LENGTH_LONG).show()
                        }
                    }
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Failed to delete user data", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun loadProfileAd() {
        if (!ConfigManager.shouldShowBannerAds(this)) return
        val container = findViewById<LinearLayout>(R.id.profAdContainer) ?: return
        val adView = AdView(this)
        adView.adUnitId = getString(R.string.admob_banner_id)
        adView.setAdSize(AdSize.BANNER)
        container.removeAllViews()
        container.addView(adView)
        adView.loadAd(AdRequest.Builder().build())
    }

    private fun logoutUser() {
        // Clear Firebase session
        try {
            auth.signOut()
        } catch (e: Exception) {}
        
        // Clear local data
        getSharedPreferences("UserPrefs", Context.MODE_PRIVATE).edit().clear().apply()
        getSharedPreferences("DeviceUsagePrefs", Context.MODE_PRIVATE).edit().clear().apply()
        
        Toast.makeText(this, "Logged Out Successfully", Toast.LENGTH_SHORT).show()
        
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    fun finishProfile(view: View) {
        finish()
    }
}
