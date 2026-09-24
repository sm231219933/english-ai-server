package com.smnm.englishtrackingai

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions

class TestActivity : AppCompatActivity() {

    private lateinit var testContainer: LinearLayout
    private val mcqGroups = mutableListOf<RadioGroup>()
    private val spinners = mutableListOf<Spinner>()
    private val correctMcqAnswers = mutableListOf<String>()
    private val correctMatchAnswers = mutableListOf<String>()
    private val questionSummaries = mutableListOf<String>()

    private var level = "Beginner"
    private var testIndex = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_test)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        testContainer = findViewById(R.id.testContainer)
        level = intent.getStringExtra("level") ?: "Beginner"
        testIndex = intent.getIntExtra("testIndex", 0)
        
        generateTest(level, testIndex)

        findViewById<Button>(R.id.submitTestButton).setOnClickListener {
            showScorecard()
        }
    }

    private fun generateTest(level: String, index: Int) {
        val allWords = if (level == "Beginner") VocabData.beginnerWords else VocabData.advancedWords
        val start = index * 10
        val end = minOf(start + 10, allWords.size)
        
        if (start >= allWords.size) {
            Toast.makeText(this, "End of tests!", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val testWords = allWords.subList(start, end)

        // Part 1: Hard Correct Usage (All 4 options have the same word)
        addSectionHeader("Part 1: Master Usage (Pick the only CORRECT sentence)")
        testWords.take(5).forEachIndexed { i, vocab ->
            val layout = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(0, 10, 0, 40)
            }
            
            val wordTitle = TextView(this).apply {
                text = "${i + 1}. Which sentence uses the word '${vocab.word.uppercase()}' correctly?"
                textSize = 17f
                setTextColor(android.graphics.Color.BLACK)
                setTypeface(null, android.graphics.Typeface.BOLD)
                setPadding(0, 0, 0, 12)
            }
            
            val rg = RadioGroup(this)
            val options = mutableListOf<String>()
            
            // 1. The Real Correct Sentence
            options.add(vocab.sentence)

            // 2, 3, 4. Trick Sentences (Same word, but put into other random word's sentences)
            val otherPool = allWords.filter { it.word != vocab.word }.shuffled()
            for (j in 0 until 3) {
                if (j < otherPool.size) {
                    val fakeVocab = otherPool[j]
                    // Replace the original word in that sentence with our TARGET word to create a context error
                    val fakeSentence = fakeVocab.sentence.replace(fakeVocab.word, vocab.word, ignoreCase = true)
                        .replace(fakeVocab.word.lowercase(), vocab.word.lowercase(), ignoreCase = true)
                    options.add(fakeSentence)
                }
            }
            
            options.shuffle()

            options.forEach { optText ->
                val rb = RadioButton(this).apply {
                    text = optText
                    textSize = 14f
                    setPadding(10, 12, 10, 12)
                    setTextColor(android.graphics.Color.DKGRAY)
                }
                rg.addView(rb)
            }
            
            layout.addView(wordTitle)
            layout.addView(rg)
            testContainer.addView(layout)
            mcqGroups.add(rg)
            correctMcqAnswers.add(vocab.sentence)
            questionSummaries.add("Word: ${vocab.word}")
        }

        // Part 2: Match Meaning (Jodi Banao)
        addSectionHeader("Part 2: Match the Meaning (Jodi Banao)")
        val shuffledMeanings = testWords.shuffled()
        testWords.forEach { vocab ->
            val layout = LinearLayout(this).apply { 
                orientation = LinearLayout.HORIZONTAL
                setPadding(0, 15, 0, 15)
                gravity = android.view.Gravity.CENTER_VERTICAL
            }
            val wordTxt = TextView(this).apply { 
                text = vocab.word
                layoutParams = LinearLayout.LayoutParams(0, -2, 1f)
                setTypeface(null, android.graphics.Typeface.BOLD)
                setTextColor(android.graphics.Color.BLUE)
            }
            val spinner = Spinner(this).apply {
                layoutParams = LinearLayout.LayoutParams(0, -2, 2f)
                val opts = mutableListOf("Select Meaning")
                opts.addAll(shuffledMeanings.map { it.meaning })
                adapter = ArrayAdapter(this@TestActivity, android.R.layout.simple_spinner_dropdown_item, opts)
            }
            layout.addView(wordTxt)
            layout.addView(spinner)
            testContainer.addView(layout)
            spinners.add(spinner)
            correctMatchAnswers.add(vocab.meaning)
        }
    }

    private fun addSectionHeader(title: String) {
        val h = TextView(this).apply { 
            text = title
            textSize = 19f
            setPadding(0, 40, 0, 15)
            setTextColor(android.graphics.Color.parseColor("#2563EB"))
            setTypeface(null, android.graphics.Typeface.BOLD)
        }
        testContainer.addView(h)
    }

    private fun showScorecard() {
        var score = 0
        val reviewReport = StringBuilder()
        
        mcqGroups.forEachIndexed { i, rg ->
            val selectedId = rg.checkedRadioButtonId
            if (selectedId != -1) {
                val selectedRb = findViewById<RadioButton>(selectedId)
                if (selectedRb.text == correctMcqAnswers[i]) {
                    score++
                } else {
                    reviewReport.append("❌ ${questionSummaries[i]}: WRONG USAGE\n   Correct: ${correctMcqAnswers[i]}\n\n")
                }
            } else {
                reviewReport.append("⚠️ ${questionSummaries[i]} was skipped.\n\n")
            }
        }
        
        spinners.forEachIndexed { i, s ->
            val ans = s.selectedItem.toString()
            if (ans == correctMatchAnswers[i]) {
                score++
            } else {
                val wordView = (s.parent as LinearLayout).getChildAt(0) as TextView
                reviewReport.append("❌ Match '${wordView.text}': WRONG MEANING\n   Correct: ${correctMatchAnswers[i]}\n\n")
            }
        }

        val total = mcqGroups.size + spinners.size
        val percentage = (score * 100) / total

        val finalMsg = if (reviewReport.isEmpty()) "🎉 Perfect! You have mastered these words."
                       else "Review and Learn from Mistakes:\n\n$reviewReport"

        AlertDialog.Builder(this)
            .setTitle("Score: $score / $total ($percentage%) 🏆")
            .setMessage(finalMsg)
            .setPositiveButton("FINISH") { _, _ ->
                saveTestCompletion()
                finish() 
            }
            .setCancelable(false)
            .show()
    }

    private fun saveTestCompletion() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        FirebaseFirestore.getInstance().collection("users").document(uid).collection("progress").document("tests")
            .set(mapOf("test_${level}_$testIndex" to true), SetOptions.merge())
    }
}
