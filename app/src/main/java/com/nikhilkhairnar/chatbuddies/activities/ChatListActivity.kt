package com.nikhilkhairnar.chatbuddies.activities

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.nikhilkhairnar.chatbuddies.fragments.ChatFragment
import com.nikhilkhairnar.chatbuddies.adapters.ChatListAdapter
import com.nikhilkhairnar.chatbuddies.R
import com.nikhilkhairnar.chatbuddies.model.User
import com.nikhilkhairnar.chatbuddies.databinding.ActivityChatListBinding

class ChatListActivity : AppCompatActivity() {

    private lateinit var binding: ActivityChatListBinding
    private lateinit var adapter: ChatListAdapter
    private val userList = mutableListOf<User>()
    private val allUsers = mutableListOf<User>()
    private val db = FirebaseFirestore.getInstance()
    private val currentUserId = FirebaseAuth.getInstance().currentUser?.uid.orEmpty()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChatListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecyclerView()
        setupSearchListener()
        fetchUsersAndLastMessages()
    }

    private fun setupRecyclerView() {
        adapter = ChatListAdapter(userList) { user ->
            markMessageAsRead(user.userId)
            openChatFragment(user.userId, user.username, user.profileImageUrl)
        }
        binding.chatListRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.chatListRecyclerView.adapter = adapter
    }

    private fun setupSearchListener() {
        binding.searchEditText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                filterUserList(s.toString())
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
    }

    private fun filterUserList(query: String) {
        val filteredList = if (query.isBlank()) {
            allUsers
        } else {
            allUsers.filter {
                it.username.contains(query, ignoreCase = true) ||
                        it.userEmail.contains(query, ignoreCase = true)
            }
        }

        userList.clear()
        userList.addAll(filteredList.sortedByDescending { it.timestamp ?: 0L })
        adapter.notifyDataSetChanged()
    }

    private fun markMessageAsRead(senderId: String) {
        db.collection("chats")
            .whereEqualTo("senderId", senderId)
            .whereEqualTo("receiverId", currentUserId)
            .whereEqualTo("isRead", false)
            .get()
            .addOnSuccessListener { snapShot ->
                for (doc in snapShot.documents) {
                    doc.reference.update("isRead", true)
                }
            }
    }

    private fun fetchUsersAndLastMessages() {
        db.collection("users").get().addOnSuccessListener { snapshot ->
            val tempList = mutableListOf<User>()
            snapshot.documents.forEach { doc ->
                val user = doc.toObject(User::class.java)
                if (user != null && user.userId != currentUserId) {
                    user.userId = doc.id
                    tempList.add(user)
                }
            }
            listenToLastMessages(tempList)
        }
    }

    private fun listenToLastMessages(users: List<User>) {
        val userMap = users.associateBy { it.userId }.toMutableMap()

        db.collection("chatList")
            .document(currentUserId)
            .collection("chats")
            .addSnapshotListener { snapshots, error ->
                if (error != null) {
                    return@addSnapshotListener
                }

                snapshots?.documents?.forEach { document ->
                    val userId = document.id
                    val lastMessage = document.getString("lastMessage") ?: ""
                    val timestamp = document.getLong("timestamp") ?: 0L
                    val isUnread = !(document.getBoolean("isRead") ?: true)

                    val user = userMap[userId]
                    if (user != null) {
                        user.lastMessage = lastMessage
                        user.timestamp = timestamp
                        user.isUnread = isUnread
                    }
                }

                val updatedUsers = userMap.values
                    .filter { it.userId != currentUserId }
                    .sortedByDescending { it.timestamp ?: 0L }

                allUsers.clear()
                allUsers.addAll(updatedUsers)

                userList.clear()
                userList.addAll(updatedUsers)
                adapter.notifyDataSetChanged()
            }
    }

    private fun openChatFragment(receiverId: String, username: String, profileImageUrl: String) {

        val fragmentContainer = findViewById<FrameLayout>(R.id.chat_fragment_container)
        fragmentContainer.visibility = View.VISIBLE

        val fragment = ChatFragment().apply {
            arguments = Bundle().apply {
                putString("receiverId", receiverId)
                putString("username", username)
                putString("profileImageUrl", profileImageUrl)
            }
        }

        supportFragmentManager.beginTransaction()
            .replace(R.id.chat_fragment_container, fragment)
            .addToBackStack(null)
            .commit()
    }
}
