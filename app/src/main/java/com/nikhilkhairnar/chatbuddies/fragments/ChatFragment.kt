package com.nikhilkhairnar.chatbuddies.fragments

import android.content.ContentValues.TAG
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.nikhilkhairnar.chatbuddies.adapters.ChatAdapter
import com.nikhilkhairnar.chatbuddies.model.ChatMessage
import com.nikhilkhairnar.chatbuddies.R
import com.nikhilkhairnar.chatbuddies.databinding.FragmentChatBinding

class ChatFragment : Fragment() {

    private lateinit var binding: FragmentChatBinding
    private lateinit var adapter: ChatAdapter
    private val firestore = FirebaseFirestore.getInstance()
    private lateinit var currentUserId: String
    private lateinit var receiverId: String
    private lateinit var receiverName: String
    private lateinit var receiverProfileUrl: String

    private var listenerRegistration: ListenerRegistration? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentChatBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.backButton.setOnClickListener {
            requireActivity().supportFragmentManager.popBackStack()
        }


        currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
        receiverId = arguments?.getString("receiverId") ?: ""
        receiverName = arguments?.getString("username") ?: "Unknown"
        receiverProfileUrl = arguments?.getString("profileImageUrl") ?: ""

        setupProfile()
        setupRecyclerView()
        fetchMessages()
        fetchSenderDetailsAndSetupSendMessage()
    }

    private fun setupProfile() {
        binding.userName.text = receiverName

        Glide.with(this)
            .load(receiverProfileUrl.ifEmpty { R.drawable.ic_profile })
            .circleCrop()
            .into(binding.profileImage)
    }

    private fun setupRecyclerView() {
        adapter = ChatAdapter(currentUserId)
        binding.chatRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.chatRecyclerView.adapter = adapter
    }

    private fun fetchSenderDetailsAndSetupSendMessage() {
        firestore.collection("users").document(currentUserId).get()
            .addOnSuccessListener { document ->
                val senderName = document.getString("username") ?: "Unknown"
                val senderProfileUrl = document.getString("profileImageUrl") ?: ""

                setupSendMessage(senderName, senderProfileUrl)
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Failed to fetch user details", Toast.LENGTH_SHORT).show()
            }
    }

    private fun setupSendMessage(senderName: String, senderProfileUrl: String) {
        binding.sendIcon.setOnClickListener {
            val messageText = binding.messageEditText.text.toString().trim()

            if (messageText.isNotEmpty()) {

                if (receiverId.isEmpty()) {
                    Toast.makeText(requireContext(), "Unable to send message. Invalid receiver.", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                val messageId = firestore.collection("Chats").document().id
                val timestamp = System.currentTimeMillis()

                val message = ChatMessage(
                    senderId = currentUserId,
                    receiverId = receiverId,
                    message = messageText,
                    timestamp = timestamp,
                    isRead = false
                )

                firestore.collection("Chats").document(messageId)
                    .set(message)
                    .addOnSuccessListener {
                        binding.messageEditText.setText("")

                        val senderChatMap = mapOf(
                            "userId" to receiverId,
                            "username" to receiverName,
                            "profileImageUrl" to receiverProfileUrl,
                            "lastMessage" to messageText,
                            "timestamp" to timestamp,
                            "isRead" to false
                        )

                        val receiverChatMap = mapOf(
                            "userId" to currentUserId,
                            "username" to senderName,
                            "profileImageUrl" to senderProfileUrl,
                            "lastMessage" to messageText,
                            "timestamp" to timestamp,
                            "isRead" to false

                        )

                        firestore.collection("chatList")
                            .document(currentUserId)
                            .collection("chats")
                            .document(receiverId)
                            .set(senderChatMap)
                            .addOnSuccessListener {
                                Log.d("Firestore", "Sender chat updated")
                            }
                            .addOnFailureListener { e ->
                                Log.e("Firestore", "Sender chat update failed", e)
                            }


                        firestore.collection("chatList")
                            .document(receiverId)
                            .collection("chats")
                            .document(currentUserId)
                            .set(receiverChatMap)
                            .addOnSuccessListener {
                                Log.d("Firestore", "Receiver chat updated")
                            }
                            .addOnFailureListener { e ->
                                Log.e("Firestore", "Receiver chat update failed", e)
                            }

                    }
                    .addOnFailureListener {
                        Toast.makeText(requireContext(), "Failed to send", Toast.LENGTH_SHORT).show()
                    }
            }
        }
    }

    private fun fetchMessages() {
        listenerRegistration = firestore.collection("Chats")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    Log.e(TAG, "Listen failed", e)
                    return@addSnapshotListener
                }

                val messages = mutableListOf<ChatMessage>()
                snapshots?.documents?.forEach { doc ->
                    val message = doc.toObject(ChatMessage::class.java)
                    if (message != null) {
                        val isCurrentToReceiver = message.senderId == currentUserId && message.receiverId == receiverId
                        val isReceiverToCurrent = message.senderId == receiverId && message.receiverId == currentUserId

                        if (isCurrentToReceiver || isReceiverToCurrent) {
                            messages.add(message)
                        }
                    }
                }

                adapter.submitList(messages)
                binding.chatRecyclerView.scrollToPosition(messages.size - 1)
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        listenerRegistration?.remove()
    }

    companion object{
        private const val ARG_USERNAME = "username"

        fun newInstance(username: String) = ChatFragment().apply {
            arguments = Bundle().apply {
                putString(ARG_USERNAME, username)
            }
        }
    }
}
