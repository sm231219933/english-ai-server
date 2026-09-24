package com.smnm.englishtrackingai

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.firestore.FirebaseFirestore
import org.json.JSONObject

class LoginActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    private lateinit var loginTitle: TextView
    private lateinit var nameEditText: EditText
    private lateinit var emailEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var ageEditText: EditText
    private lateinit var genderGroup: RadioGroup
    private lateinit var actionButton: Button
    private lateinit var toggleText: TextView
    private lateinit var authProgress: ProgressBar
    private lateinit var successOverlay: View
    private lateinit var termsCheckbox: CheckBox
    private lateinit var termsLink: TextView
    
    private var isLoginMode = false 
    private var isCheckingUser = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_login)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        loginTitle = findViewById(R.id.loginTitle)
        nameEditText = findViewById(R.id.nameEditText)
        emailEditText = findViewById(R.id.emailEditText)
        passwordEditText = findViewById(R.id.passwordEditText)
        ageEditText = findViewById(R.id.ageEditText)
        genderGroup = findViewById(R.id.genderGroup)
        actionButton = findViewById(R.id.actionButton)
        toggleText = findViewById(R.id.toggleText)
        authProgress = findViewById(R.id.authProgress)
        successOverlay = findViewById(R.id.successOverlay)
        termsCheckbox = findViewById(R.id.termsCheckbox)
        termsLink = findViewById(R.id.termsLink)

        setupEmailAutoFetch()
        updateUI()

        toggleText.setOnClickListener { isLoginMode = !isLoginMode; updateUI() }
        actionButton.setOnClickListener { validateAndProceed() }
        
        termsLink.setOnClickListener {
            startActivity(Intent(this, TermsActivity::class.java))
        }
    }

    private fun setupEmailAutoFetch() {
        emailEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val email = s.toString().trim()
                if (email.contains("@") && email.contains(".") && email.length > 5) {
                    if (!isCheckingUser) checkUserAndAutoFill(email)
                }
            }
        })
    }

    private fun checkUserAndAutoFill(email: String) {
        isCheckingUser = true
        db.collection("users")
            .whereEqualTo("email", email)
            .get()
            .addOnSuccessListener { documents ->
                isCheckingUser = false
                if (!documents.isEmpty) {
                    val user = documents.documents[0]
                    nameEditText.setText(user.getString("name"))
                    ageEditText.setText(user.getString("age"))
                    val gender = user.getString("gender")
                    if (gender == "Male") {
                        findViewById<RadioButton>(R.id.rbMale).isChecked = true
                    } else if (gender == "Female") {
                        findViewById<RadioButton>(R.id.rbFemale).isChecked = true
                    }

                    // PROFESSIONAL: Lock fields so user knows they are recognized
                    nameEditText.isEnabled = false
                    ageEditText.isEnabled = false
                    findViewById<RadioButton>(R.id.rbMale).isEnabled = false
                    findViewById<RadioButton>(R.id.rbFemale).isEnabled = false
                    
                    // Automatically switch to Login mode
                    if (!isLoginMode) {
                        isLoginMode = true
                        updateUI()
                        Toast.makeText(this@LoginActivity, "Welcome back! Please enter your password.", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .addOnFailureListener {
                isCheckingUser = false
            }
    }

    private fun updateUI() {
        if (isLoginMode) {
            loginTitle.text = "Welcome Back 👋"
            findViewById<View>(R.id.nameInputLayout).visibility = View.GONE
            findViewById<View>(R.id.extraDetailsLayout).visibility = View.GONE 
            findViewById<View>(R.id.termsCheckbox).visibility = View.GONE
            findViewById<View>(R.id.termsLink).visibility = View.GONE
            actionButton.text = "Login"
            toggleText.text = "New User? Create Account"
        } else {
            loginTitle.text = "Start Journey 🎓"
            findViewById<View>(R.id.nameInputLayout).visibility = View.VISIBLE
            findViewById<View>(R.id.extraDetailsLayout).visibility = View.VISIBLE
            findViewById<View>(R.id.ageInputLayout).visibility = View.VISIBLE 
            findViewById<View>(R.id.termsCheckbox).visibility = View.VISIBLE
            findViewById<View>(R.id.termsLink).visibility = View.VISIBLE
            actionButton.text = "Register"
            toggleText.text = "Already a member? Sign in"
            
            // RESET FIELDS if switching to Register
            nameEditText.isEnabled = true
            ageEditText.isEnabled = true
            findViewById<RadioButton>(R.id.rbMale).isEnabled = true
            findViewById<RadioButton>(R.id.rbFemale).isEnabled = true
        }
    }

    private fun validateAndProceed() {
        val email = emailEditText.text.toString().trim()
        val password = passwordEditText.text.toString().trim()

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Email/Password required", Toast.LENGTH_SHORT).show()
            return
        }

        if (!isLoginMode && !termsCheckbox.isChecked) {
            Toast.makeText(this, "Please accept Terms of Service", Toast.LENGTH_SHORT).show()
            return
        }

        setLoading(true)
        if (isLoginMode) performFirebaseLogin(email, password) else performFirebaseSignup(email, password)
    }

    private fun performFirebaseSignup(email: String, password: String) {
        val name = nameEditText.text.toString().trim()
        val age = ageEditText.text.toString().trim()
        val gender = if (genderGroup.checkedRadioButtonId == R.id.rbMale) "Male" else "Female"

        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val uid = auth.currentUser?.uid ?: ""
                    val userMap = hashMapOf(
                        "uid" to uid,
                        "name" to name,
                        "email" to email,
                        "age" to age,
                        "gender" to gender,
                        "level" to "Beginner",
                        "isPaid" to false,
                        "plan" to "Free",
                        "messageCount" to 0
                    )
                    db.collection("users").document(uid).set(userMap)
                        .addOnSuccessListener {
                            saveSessionLocally(name, email, age, gender, "Free")
                            showSuccess()
                        }
                        .addOnFailureListener { e ->
                            setLoading(false)
                            Toast.makeText(this, "Profile save failed: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                } else {
                    setLoading(false)
                    if (task.exception is FirebaseAuthUserCollisionException) {
                        Toast.makeText(this, "Account already exists! Please Login.", Toast.LENGTH_LONG).show()
                        isLoginMode = true
                        updateUI()
                    } else {
                        Toast.makeText(this, "Signup failed: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }
    }

    private fun performFirebaseLogin(email: String, password: String) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val uid = auth.currentUser?.uid ?: ""
                    db.collection("users").document(uid).get()
                        .addOnSuccessListener { document ->
                            if (document.exists()) {
                                val name = document.getString("name") ?: "Learner"
                                val age = document.getString("age") ?: ""
                                val gender = document.getString("gender") ?: "Any"
                                val plan = document.getString("plan") ?: "Free"
                                saveSessionLocally(name, email, age, gender, plan)
                                showSuccess()
                            } else {
                                // If Auth exists but no Firestore doc (rare)
                                saveSessionLocally("Learner", email, "", "Any", "Free")
                                showSuccess()
                            }
                        }
                        .addOnFailureListener {
                            setLoading(false)
                            Toast.makeText(this, "Login successful but profile fetch failed.", Toast.LENGTH_SHORT).show()
                        }
                } else {
                    setLoading(false)
                    Toast.makeText(this, "Login failed: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                }
            }
    }

    private fun saveSessionLocally(name: String, email: String, age: String, gender: String, plan: String) {
        getSharedPreferences("UserPrefs", Context.MODE_PRIVATE).edit().apply {
            putString("user_name", name)
            putString("user_email", email)
            putString("user_age", age)
            putString("user_gender", gender)
            putString("user_plan", plan)
            apply()
        }
    }

    private fun setLoading(isLoading: Boolean) {
        actionButton.isEnabled = !isLoading
        authProgress.visibility = if (isLoading) View.VISIBLE else View.GONE
        actionButton.text = if (isLoading) "" else (if (isLoginMode) "Login" else "Register")
    }

    private fun showSuccess() {
        successOverlay.visibility = View.VISIBLE
        Handler(Looper.getMainLooper()).postDelayed({
            val intent = Intent(this, HomeActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }, 1500)
    }
}
