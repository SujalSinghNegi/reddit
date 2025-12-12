package com.example.reddit.models

data class UserModel(
    val uid: String = "",
    val name: String = "",
    val email: String = "",
    val phoneNumber: String = "",
    val profileImage: String = "" // URL to their photo
)