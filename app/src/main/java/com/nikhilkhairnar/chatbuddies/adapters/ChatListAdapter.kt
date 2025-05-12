package com.nikhilkhairnar.chatbuddies.adapters

import android.graphics.Color
import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.nikhilkhairnar.chatbuddies.R
import com.nikhilkhairnar.chatbuddies.model.User
import com.nikhilkhairnar.chatbuddies.databinding.ItemChatBinding
import java.text.SimpleDateFormat
import java.util.*

class ChatListAdapter(
    private var userList: List<User>,
    private val onUserClick: (User) -> Unit
) : RecyclerView.Adapter<ChatListAdapter.ChatListViewHolder>() {

    inner class ChatListViewHolder(val binding: ItemChatBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatListViewHolder {
        val binding = ItemChatBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ChatListViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ChatListViewHolder, position: Int) {
        val user = userList[position]
        holder.binding.chatName.text = user.username
        holder.binding.lastMessage.text = user.lastMessage

        if (user.isUnread) {
            holder.binding.lastMessage.setTypeface(null, Typeface.BOLD)
            holder.binding.lastMessage.setTextColor(Color.parseColor("#FF5722")) // Orange
        } else {
            holder.binding.lastMessage.setTypeface(null, Typeface.NORMAL)
            holder.binding.lastMessage.setTextColor(Color.GRAY)
        }
        Glide.with(holder.itemView.context)
            .load(user.profileImageUrl.ifEmpty { R.drawable.ic_profile })
            .circleCrop()
            .into(holder.binding.chatProfileImage)

        holder.binding.timestamp.text = user.timestamp?.let { formatTime(it) } ?: ""

        holder.itemView.setOnClickListener {
            onUserClick(user)
        }
    }

    override fun getItemCount(): Int = userList.size

    fun updateList(newList: List<User>) {
        userList = newList
        notifyDataSetChanged()
    }

    private fun formatTime(timestamp: Long): String {
        val date = Date(timestamp)
        val format = SimpleDateFormat("hh:mm a", Locale.getDefault())
        return format.format(date)
    }
}
