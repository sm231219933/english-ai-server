package com.smnm.englishtrackingai

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class OnlineUserAdapter(private var users: List<Map<String, String>>) :
    RecyclerView.Adapter<OnlineUserAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val initial: TextView = view.findViewById(R.id.userInitial)
        val name: TextView = view.findViewById(R.id.userShortName)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_online_user, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val user = users[position]
        val fullName = user["name"] ?: "Learner"
        holder.name.text = if (fullName.length > 8) fullName.take(7) + ".." else fullName
        holder.initial.text = fullName.take(1).uppercase()
    }

    override fun getItemCount() = users.size

    fun updateList(newList: List<Map<String, String>>) {
        users = newList
        notifyDataSetChanged()
    }
}
