package com.example.reddit.models


import com.google.firebase.firestore.Exclude

data class PostModel(
    val postId: String = "",
    val authorId: String = "",
    val userName: String = "",
    val imageUrl: String = "",
    val caption: String = "",
    var voteCount: Int = 0, // This is the total score saved on the server
    val timestamp: Long = 0L,

    // Local UI state only. Firestore will ignore this field when reading/writing the post document.
    @get:Exclude var currentUserVote: Int = 0
)