package com.smnm.englishtrackingai

import android.content.Intent
import android.os.Bundle
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

data class Conversation(val id: Int, val title: String, val dialogue: List<Pair<String, String>>)

class ConversationsActivity : AppCompatActivity() {

    private val conversations = listOf(
        Conversation(1, "A Visit to Amit's House 🏠", listOf(
            "Rahul" to "Hello Amit! I finally came to your house today. The blue color of your house looks beautiful!",
            "Amit" to "Welcome, Rahul! Please come in. Yes, I really like my house.",
            "Rahul" to "It looks so airy. How many doors and windows do you have?",
            "Amit" to "There are two doors and four windows. Oh, let me introduce you. My father's name is Mr. Sharma and my mother's name is Mrs. Sharma.",
            "Rahul" to "Nice to meet you uncle and aunty! Amit, I saw a big tree and a road near your house, but is there any garden in front?",
            "Amit" to "No, there is no garden here. Come inside, please sit on this chair.",
            "Rahul" to "Thanks! Wow, your study desk is very organized. You have your pen, pencil, and notebook kept perfectly.",
            "Amit" to "Thank you! I just got that new calendar and mobile. Oh, wait, I left my handkerchief on the desk too.",
            "Rahul" to "By the way, your birthday is coming up on the 10th of October, right? What are we doing that day?",
            "Amit" to "Yes! We will go outside and play games. You know I love playing Kabaddi, Kho-Kho, and Cricket.",
            "Rahul" to "That sounds like fun! Can we also watch some cartoon films later? I really like watching them.",
            "Amit" to "Definitely! But we must ask our parents first. We should always obey our elders.",
            "Rahul" to "You are right. Hey, I was reading our science book today. Can I ask you a quick question?",
            "Amit" to "Sure, ask me.",
            "Rahul" to "Tell me, how many legs do animals like dogs, cats, horses, pigs, and elephants have?",
            "Amit" to "That is very easy! All of them have four legs. A rabbit also has four legs.",
            "Rahul" to "Very smart! And what about birds like a parrot or a peacock?",
            "Amit" to "They only have two legs.",
            "Rahul" to "Perfect! Your general knowledge is great."
        )),
        Conversation(2, "Morning Walk & Nature 🌳", listOf(
            "Rahul" to "Good morning, Amit! What do you say to someone when you meet them in the afternoon or evening?",
            "Amit" to "I say 'Good Afternoon' or 'Good Evening'. And when I leave for the day, I say 'Good Bye' or 'Have a good day'.",
            "Rahul" to "That is nice! Look at the sky. The sun always shines so brightly in the morning. Did you see the stars last night?",
            "Amit" to "Yes, I have seen the stars. The moon and the stars shine at night in the sky.",
            "Rahul" to "Look up, there are dark clouds in the sky today.",
            "Amit" to "Yes, the clouds bring rain. And after the rain, we can see a beautiful rainbow!",
            "Rahul" to "Oh, look at that girl over there. Is she riding a bicycle or reading a book?",
            "Amit" to "No, she is not watching television either. She is throwing waste in the dustbin.",
            "Rahul" to "And what is that boy doing? Is he eating a mango or going to school?",
            "Amit" to "No, he is not combing his hair. He is writing on a notebook."
        )),
        Conversation(3, "Sunday Plans & Market 🛒", listOf(
            "Rahul" to "Today is Sunday, so we finally have a holiday! Does your father go to the office today?",
            "Amit" to "No, my father does not go to the office on Sundays. He cooks food, and my mother watches TV.",
            "Rahul" to "That sounds fun. What do your grandparents do?",
            "Amit" to "My grandfather plays table tennis, and my grandmother plays with us. I really like to play cricket on Sundays!",
            "Rahul" to "I am going to the market. It is very near to my house.",
            "Amit" to "Will you buy toffees and ice cream? There is a toffee shop and an ice cream shop there.",
            "Rahul" to "No, those are not good for our teeth. I will buy fresh fruits and greens from the market instead."
        )),
        Conversation(4, "Good Manners & Opposites 🤝", listOf(
            "Amit" to "Eating fruits is a very good habit. I also make sure to take my meals in time and take a bath every day.",
            "Rahul" to "Yes, and we should brush our teeth twice a day. I always go to sleep early in the night and get up early in the morning.",
            "Amit" to "That is great. We should also help needy persons and never fight with our friends.",
            "Rahul" to "Exactly. I always say 'thank you' when somebody helps me, and I keep my classroom clean.",
            "Amit" to "Let's play a game of opposite words! What is the opposite of 'fat' and 'tall'?",
            "Rahul" to "The opposite of fat is 'thin', and for tall, it is 'short'.",
            "Amit" to "What about big, bad, and dirty?",
            "Rahul" to "Small, good, and clean!",
            "Amit" to "Last ones: hot, dry, and happy!",
            "Rahul" to "Cold, wet, and sad. That was easy!"
        )),
        Conversation(5, "Our School & Country 🇮🇳", listOf(
            "Rahul" to "I really miss our school building today. Do you remember how many classrooms we have?",
            "Amit" to "Yes, there are fifteen classrooms and ten teachers in our school. We also have a big playground.",
            "Rahul" to "Shri Arvindbhai Patel is our school principal, right?",
            "Amit" to "Yes, and our class teacher is Smt. Vandana Patel. I like my school very much.",
            "Rahul" to "In class, we learned about our country, India. I have seen the map and the flag of our country.",
            "Amit" to "Yes, I love my country very much too. There are three colours in our flag: orange, white, and green.",
            "Rahul" to "By the way, which language do you and your friends speak at home?",
            "Amit" to "My friend and I both speak the Gujarati language."
        )),
        Conversation(6, "Body Parts & Fruits 🍎", listOf(
            "Rahul" to "In science class, we learned about our bodies. We all have one nose and one tongue.",
            "Amit" to "Yes, and we have two eyes, two ears, two hands, and two legs!",
            "Rahul" to "Do not forget our hands! We have eight fingers and two thumbs.",
            "Amit" to "Hey, do you want to play the fruit basket game? I have kept all the fruits in the basket.",
            "Rahul" to "Okay! Is there a mango and are there oranges in the basket?",
            "Amit" to "Yes, there is a mango and there are oranges.",
            "Rahul" to "Are there black grapes too?",
            "Amit" to "Yes, the grapes are black in colour.",
            "Rahul" to "How many bananas and apples are there in the basket?",
            "Amit" to "There are five bananas and three apples in the basket!"
        ))
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_conversations)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val recycler = findViewById<RecyclerView>(R.id.conversationsRecycler)
        recycler.layoutManager = LinearLayoutManager(this)
        recycler.adapter = ConvAdapter(conversations) { conv ->
            val intent = Intent(this, ConversationDetailActivity::class.java)
            intent.putExtra("title", conv.title)
            intent.putExtra("dialogue", conv.dialogue.map { "${it.first}|${it.second}" }.toTypedArray())
            startActivity(intent)
        }
    }

    class ConvAdapter(private val list: List<Conversation>, val onClick: (Conversation) -> Unit) : RecyclerView.Adapter<ConvAdapter.VH>() {
        class VH(v: View) : RecyclerView.ViewHolder(v) {
            val title: TextView = v.findViewById(android.R.id.text1)
        }
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val v = LayoutInflater.from(parent.context).inflate(android.R.layout.simple_list_item_1, parent, false)
            return VH(v)
        }
        override fun onBindViewHolder(holder: VH, position: Int) {
            holder.title.text = list[position].title
            holder.itemView.setOnClickListener { onClick(list[position]) }
        }
        override fun getItemCount() = list.size
    }
}
