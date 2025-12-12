package com.example.reddit.models

data class PostModel(
    val postId: String = "",
    val authorId: String = "",
    val userName: String = "",
    val imageUrl: String = "",
    val caption: String = "",
    val voteCount: Int = 0,
    val timestamp: Long = 0L
)