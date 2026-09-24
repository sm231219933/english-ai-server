package com.smnm.englishtrackingai

import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.util.*

class ConversationDetailActivity : AppCompatActivity(), TextToSpeech.OnInitListener {

    private lateinit var tts: TextToSpeech
    private val dialogueList = ArrayList<Pair<String, String>>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_conversation_detail)

        val title = intent.getStringExtra("title") ?: "Conversation"
        findViewById<TextView>(R.id.detailTitle).text = title

        val rawDialogue = intent.getStringArrayExtra("dialogue") ?: arrayOf()
        for (item in rawDialogue) {
            val parts = item.split("|")
            if (parts.size == 2) dialogueList.add(parts[0] to parts[1])
        }

        tts = TextToSpeech(this, this)

        val recycler = findViewById<RecyclerView>(R.id.dialogueRecycler)
        recycler.layoutManager = LinearLayoutManager(this)
        recycler.adapter = DialogueAdapter(dialogueList) { text ->
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "")
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) tts.language = Locale.US
    }

    class DialogueAdapter(private val list: List<Pair<String, String>>, val onListen: (String) -> Unit) : RecyclerView.Adapter<DialogueAdapter.VH>() {
        class VH(v: View) : RecyclerView.ViewHolder(v) {
            val person: TextView = v.findViewById(R.id.nodePerson)
            val text: TextView = v.findViewById(R.id.nodeText)
            val btnListen: ImageButton = v.findViewById(R.id.btnListen)
        }
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val v = LayoutInflater.from(parent.context).inflate(R.layout.item_conversation_node, parent, false)
            return VH(v)
        }
        override fun onBindViewHolder(holder: VH, position: Int) {
            val item = list[position]
            holder.person.text = item.first.take(1).uppercase()
            holder.text.text = item.second
            holder.btnListen.setOnClickListener { onListen(item.second) }
        }
        override fun getItemCount() = list.size
    }

    override fun onDestroy() {
        super.onDestroy()
        tts.stop(); tts.shutdown()
    }
}
