package com.nikhilkhairnar.chatbuddies.adapters

import android.view.Gravity
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.nikhilkhairnar.chatbuddies.model.ChatMessage
import com.nikhilkhairnar.chatbuddies.R
import com.nikhilkhairnar.chatbuddies.databinding.ItemChatMessageBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ChatAdapter(private val currentUserId: String) :
    RecyclerView.Adapter<ChatAdapter.ChatViewHolder>() {

    private var messageList = mutableListOf<ChatMessage>()

    inner class ChatViewHolder(val binding: ItemChatMessageBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatViewHolder {
        val binding =
            ItemChatMessageBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ChatViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ChatViewHolder, position: Int) {
        val message = messageList[position]
        val isSentByCurrentUser = message.senderId == currentUserId

        holder.binding.messageText.text = message.message
        holder.binding.messageTimestamp.text = SimpleDateFormat("hh:mm a", Locale.getDefault())
            .format(Date(message.timestamp))

        val layoutParams = holder.binding.messageContainer.layoutParams as LinearLayout.LayoutParams
        if (isSentByCurrentUser) {
            layoutParams.gravity = Gravity.END
            holder.binding.messageText.background = ContextCompat.getDrawable(
                holder.itemView.context,
                R.drawable.ic_chat_bubble_outgoing
            )
        } else {
            layoutParams.gravity = Gravity.START
            holder.binding.messageText.background = ContextCompat.getDrawable(
                holder.itemView.context,
                R.drawable.ic_chat_bubble_incoming
            )
        }
        holder.binding.messageContainer.layoutParams = layoutParams
    }

    override fun getItemCount(): Int = messageList.size

    fun submitList(list: List<ChatMessage>) {
        messageList.clear()
        messageList.addAll(list)
        notifyDataSetChanged()
    }


}