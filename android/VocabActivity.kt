package com.smnm.englishtrackingai

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.ads.*
import java.util.*

class VocabActivity : AppCompatActivity() {

    private lateinit var vocabRecyclerView: RecyclerView
    private lateinit var searchEditText: EditText
    private lateinit var vocabTitle: TextView
    private lateinit var vocabLevelGroup: RadioGroup
    private val fullList = ArrayList<Any>()
    private val filteredList = ArrayList<Any>()
    private lateinit var adapter: VocabAdapter
    private var currentLevel = "Beginner"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_vocab)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        vocabTitle = findViewById(R.id.vocabTitle)
        vocabRecyclerView = findViewById(R.id.vocabRecyclerView)
        searchEditText = findViewById(R.id.searchEditText)
        vocabLevelGroup = findViewById(R.id.vocabLevelGroup)

        adapter = VocabAdapter(filteredList)
        vocabRecyclerView.layoutManager = LinearLayoutManager(this)
        vocabRecyclerView.adapter = adapter

        currentLevel = intent.getStringExtra("level") ?: "Beginner"
        loadWords(currentLevel)

        vocabLevelGroup.setOnCheckedChangeListener { _, checkedId ->
            currentLevel = when (checkedId) {
                R.id.rbVocabAdvanced -> "Advanced"
                else -> "Beginner"
            }
            loadWords(currentLevel)
        }

        searchEditText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) { filter(s.toString()) }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
        
        findViewById<ImageButton>(R.id.backButton).setOnClickListener { finish() }
    }

    private fun loadWords(level: String) {
        val rawList: List<VocabWord> = if (level == "Advanced") {
            AdvancedWordsPart1.list // Or merge both parts if needed
        } else {
            BeginnerWords.list
        }
        
        fullList.clear()
        // Inject ads every 7 words
        rawList.forEachIndexed { index, word ->
            fullList.add(word)
            if ((index + 1) % 7 == 0) {
                fullList.add("AD_MARKER")
            }
        }
        
        filter(searchEditText.text.toString())
    }

    private fun filter(query: String) {
        filteredList.clear()
        if (query.isEmpty()) {
            filteredList.addAll(fullList)
        } else {
            val q = query.lowercase(Locale.getDefault())
            for (item in fullList) {
                if (item is VocabWord) {
                    if (item.word.lowercase(Locale.getDefault()).contains(q)) {
                        filteredList.add(item)
                    }
                }
            }
        }
        adapter.notifyDataSetChanged()
    }

    inner class VocabAdapter(private val items: List<Any>) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
        
        private val TYPE_WORD = 0
        private val TYPE_AD = 1

        inner class WordViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val wordTxt: TextView = view.findViewById(R.id.wordTextView)
            val meaningTxt: TextView = view.findViewById(R.id.meaningTextView)
            val sentenceTxt: TextView = view.findViewById(R.id.sentenceTextView)
            val eye: ImageView = view.findViewById(R.id.eyeIcon)
            val details: View = view.findViewById(R.id.detailsContainer)
        }

        inner class AdViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val adContainer: LinearLayout = view.findViewById(R.id.adContainer)
        }

        override fun getItemViewType(position: Int): Int {
            return if (items[position] is String && items[position] == "AD_MARKER") TYPE_AD else TYPE_WORD
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            return if (viewType == TYPE_AD) {
                val v = LayoutInflater.from(parent.context).inflate(R.layout.item_vocab_ad, parent, false)
                AdViewHolder(v)
            } else {
                val v = LayoutInflater.from(parent.context).inflate(R.layout.item_vocab, parent, false)
                WordViewHolder(v)
            }
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            if (holder is WordViewHolder) {
                val word = items[position] as VocabWord
                holder.wordTxt.text = word.word
                holder.meaningTxt.text = word.meaning
                holder.sentenceTxt.text = "Ex: ${word.sentence}"
                holder.details.visibility = if (word.isVisible) View.VISIBLE else View.GONE
                holder.eye.setImageResource(if (word.isVisible) android.R.drawable.ic_menu_close_clear_cancel else android.R.drawable.ic_menu_view)
                holder.itemView.setOnClickListener { word.isVisible = !word.isVisible; notifyItemChanged(position) }
            } else if (holder is AdViewHolder) {
                if (ConfigManager.shouldShowBannerAds(holder.itemView.context)) {
                    holder.adContainer.removeAllViews()
                    val adView = AdView(holder.itemView.context)
                    adView.adUnitId = holder.itemView.context.getString(R.string.admob_banner_id)
                    adView.setAdSize(AdSize.BANNER)
                    holder.adContainer.addView(adView)
                    adView.loadAd(AdRequest.Builder().build())
                    holder.itemView.visibility = View.VISIBLE
                } else {
                    holder.itemView.visibility = View.GONE
                    holder.adContainer.removeAllViews()
                }
            }
        }

        override fun getItemCount(): Int = items.size
    }
}
