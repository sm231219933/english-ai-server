package com.smnm.englishtrackingai

import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.util.*

data class Lesson(val title: String, val content: String)

class LessonsActivity : AppCompatActivity(), TextToSpeech.OnInitListener {

    private lateinit var tts: TextToSpeech
    private val lessonList = listOf(
        Lesson("At the Doctor 🏥", "A: Good morning Doctor.\nB: Good morning. How can I help you?\nA: I have a severe headache and fever.\nB: Let me check your temperature."),
        Lesson("Ordering Pizza 🍕", "A: Hello, I'd like to order a large Pepperoni pizza.\nB: Sure, would you like any extra toppings?\nA: Yes, extra cheese please.\nB: That will be 15 dollars."),
        Lesson("Job Interview 💼", "A: Tell me about yourself.\nB: I am a hard-working person with 3 years of experience.\nA: Why do you want this job?\nB: Because I want to grow my skills."),
        Lesson("Booking a Hotel 🏨", "A: I want to book a room for two nights.\nB: Single or double bed?\nA: A double bed with a sea view please.\nB: Your room number is 402.")
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_lessons)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        tts = TextToSpeech(this, this)

        val recycler = findViewById<RecyclerView>(R.id.lessonsRecycler)
        recycler.layoutManager = LinearLayoutManager(this)
        recycler.adapter = LessonAdapter(lessonList) { content ->
            tts.speak(content, TextToSpeech.QUEUE_FLUSH, null, "")
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) tts.language = Locale.US
    }

    class LessonAdapter(private val list: List<Lesson>, val onClick: (String) -> Unit) : RecyclerView.Adapter<LessonAdapter.VH>() {
        class VH(v: View) : RecyclerView.ViewHolder(v) {
            val title: TextView = v.findViewById(android.R.id.text1)
            val content: TextView = v.findViewById(android.R.id.text2)
        }
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val v = LayoutInflater.from(parent.context).inflate(android.R.layout.simple_list_item_2, parent, false)
            return VH(v)
        }
        override fun onBindViewHolder(holder: VH, position: Int) {
            val item = list[position]
            holder.title.text = item.title
            holder.content.text = item.content
            holder.itemView.setOnClickListener { onClick(item.content) }
        }
        override fun getItemCount() = list.size
    }

    override fun onDestroy() {
        super.onDestroy()
        tts.stop(); tts.shutdown()
    }
}
