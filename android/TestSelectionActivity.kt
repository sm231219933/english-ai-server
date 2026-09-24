package com.smnm.englishtrackingai

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlin.math.ceil

class TestSelectionActivity : AppCompatActivity() {

    private lateinit var levelContainer: LinearLayout
    private lateinit var recycler: RecyclerView
    private lateinit var listLabel: TextView
    private lateinit var titleTxt: TextView
    private var completedTests = mutableSetOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_test_selection)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        levelContainer = findViewById(R.id.levelButtonsContainer)
        recycler = findViewById(R.id.testListRecyclerView)
        listLabel = findViewById(R.id.testListLabel)
        titleTxt = findViewById(R.id.testSelTitle)

        findViewById<Button>(R.id.btnLevelBeg).setOnClickListener { showTests("Beginner") }
        findViewById<Button>(R.id.btnLevelAdv).setOnClickListener { showTests("Advanced") }
        
        loadProgress()
    }

    private fun loadProgress() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        FirebaseFirestore.getInstance().collection("users").document(uid).collection("progress").document("tests")
            .get().addOnSuccessListener { doc ->
                if (doc.exists()) {
                    completedTests.clear()
                    doc.data?.keys?.forEach { key ->
                        if (doc.getBoolean(key) == true) completedTests.add(key)
                    }
                    if (recycler.visibility == View.VISIBLE) {
                        recycler.adapter?.notifyDataSetChanged()
                    }
                }
            }
    }

    private fun showTests(level: String) {
        titleTxt.text = "Tests for $level"
        levelContainer.visibility = View.GONE
        listLabel.visibility = View.VISIBLE
        recycler.visibility = View.VISIBLE

        val wordCount = if (level == "Beginner") VocabData.beginnerWords.size else VocabData.advancedWords.size
        // Mathematical Ceil to ensure EVERY single word is covered
        val totalTests = ceil(wordCount.toDouble() / 10.0).toInt()

        recycler.layoutManager = LinearLayoutManager(this)
        recycler.adapter = TestListAdapter(totalTests, level, completedTests)
    }

    class TestListAdapter(private val count: Int, private val level: String, private val completed: Set<String>) : RecyclerView.Adapter<TestListAdapter.VH>() {
        class VH(v: View) : RecyclerView.ViewHolder(v) {
            val title: TextView = v.findViewById(R.id.testTitle)
            val badge: TextView = v.findViewById(R.id.completedBadge)
            val actionBtn: Button = v.findViewById(R.id.testActionBtn)
        }
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val v = LayoutInflater.from(parent.context).inflate(R.layout.item_test_list, parent, false)
            return VH(v)
        }
        override fun onBindViewHolder(holder: VH, position: Int) {
            val testKey = "test_${level}_$position"
            val isDone = completed.contains(testKey)
            holder.title.text = "Test ${position + 1}"
            
            if (isDone) {
                holder.badge.visibility = View.VISIBLE
                holder.actionBtn.text = "RETEST"
                holder.actionBtn.setBackgroundColor(android.graphics.Color.DKGRAY)
            } else {
                holder.badge.visibility = View.GONE
                holder.actionBtn.text = "START"
                holder.actionBtn.setBackgroundColor(android.graphics.Color.parseColor("#2563EB"))
            }

            holder.actionBtn.setOnClickListener {
                val intent = Intent(it.context, TestActivity::class.java)
                intent.putExtra("level", level)
                intent.putExtra("testIndex", position)
                it.context.startActivity(intent)
            }
        }
        override fun getItemCount() = count
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (levelContainer.visibility == View.GONE) {
            titleTxt.text = "Practice Tests 📝"
            levelContainer.visibility = View.VISIBLE
            listLabel.visibility = View.GONE
            recycler.visibility = View.GONE
        } else {
            super.onBackPressed()
        }
    }

    override fun onResume() {
        super.onResume()
        loadProgress()
    }
}
