package com.nikhilkhairnar.chatbuddies.model

data class User(
    var userId: String = "",
    val username: String = "",
    var userEmail: String = "",
    val profileImageUrl: String = "",
    var lastMessage: String = "",
    var timestamp: Long? = null,
    var isUnread: Boolean = false
)


