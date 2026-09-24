package com.smnm.englishtrackingai

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ChatAdapter(private val messageList: List<ChatMessage>) :
    RecyclerView.Adapter<ChatAdapter.ChatViewHolder>() {

    class ChatViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val layoutAI: LinearLayout = itemView.findViewById(R.id.layoutAI)
        val textAI: TextView = itemView.findViewById(R.id.textAI)
        val layoutUser: LinearLayout = itemView.findViewById(R.id.layoutUser)
        val textUser: TextView = itemView.findViewById(R.id.textUser)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_chat_bubble, parent, false)
        return ChatViewHolder(view)
    }

    override fun onBindViewHolder(holder: ChatViewHolder, position: Int) {
        val chatMessage = messageList[position]
        if (chatMessage.isUser) {
            holder.layoutUser.visibility = View.VISIBLE
            holder.layoutAI.visibility = View.GONE
            holder.textUser.text = chatMessage.message
        } else {
            holder.layoutAI.visibility = View.VISIBLE
            holder.layoutUser.visibility = View.GONE
            holder.textAI.text = chatMessage.message
        }
    }

    override fun getItemCount(): Int = messageList.size
}
