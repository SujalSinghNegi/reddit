package com.example.reddit

import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.reddit.databinding.ItemPostBinding
import com.example.reddit.models.PostModel



import android.os.SystemClock

import android.widget.Toast

interface OnPostClickListener {
    fun onUpvoteClick(post: PostModel, position: Int)
    fun onDownvoteClick(post: PostModel, position: Int)
    fun onCommentClick(post: PostModel)
}

class PostAdapter(
    private val context: Context,
    private val postList: List<PostModel>,
    private val listener: OnPostClickListener
) : RecyclerView.Adapter<PostAdapter.PostViewHolder>() {

    // --- NEW: Tracking Variables ---
    // Maps the postId to the exact time (in milliseconds) when it will unlock
    private val lockedPosts = mutableMapOf<String, Long>()

    // Tracks the last time the "Locked" toast was shown to prevent spam
    private var lastToastTime = 0L

    class PostViewHolder(val binding: ItemPostBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        val binding = ItemPostBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PostViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        val post = postList[position]

        holder.binding.tvUserName.text = post.userName
        holder.binding.tvCaption.text = post.caption
        holder.binding.tvVoteCount.text = post.voteCount.toString()

        Glide.with(context).load(post.imageUrl).into(holder.binding.imgPost)

        // Visual UI Logic
        val upvoteColor = Color.parseColor("#FF4500")
        val downvoteColor = Color.parseColor("#7193FF")
        val defaultColor = Color.parseColor("#808080")

        when (post.currentUserVote) {
            1 -> {
                holder.binding.imgUpvote.setColorFilter(upvoteColor)
                holder.binding.imgDownvote.setColorFilter(defaultColor)
            }
            -1 -> {
                holder.binding.imgUpvote.setColorFilter(defaultColor)
                holder.binding.imgDownvote.setColorFilter(downvoteColor)
            }
            else -> {
                holder.binding.imgUpvote.setColorFilter(defaultColor)
                holder.binding.imgDownvote.setColorFilter(defaultColor)
            }
        }

        // --- NEW: Click Listeners with Lockout Logic ---
        holder.binding.imgUpvote.setOnClickListener {
            handleVoteAction(post, position) {
                listener.onUpvoteClick(post, position)
            }
        }

        holder.binding.imgDownvote.setOnClickListener {
            handleVoteAction(post, position) {
                listener.onDownvoteClick(post, position)
            }
        }

        holder.binding.imgComment.setOnClickListener {
            listener.onCommentClick(post)
        }
    }

    // --- NEW: The Central Logic Function ---
    private fun handleVoteAction(post: PostModel, position: Int, action: () -> Unit) {
        val currentTime = SystemClock.elapsedRealtime()
        val unlockTime = lockedPosts[post.postId] ?: 0L

        // 1. Check if the post is currently locked
        if (currentTime < unlockTime) {

            // 2. It is locked. Check if 1 second has passed since the last Toast
            if (currentTime - lastToastTime > 1000L) {
                val remainingSeconds = ((unlockTime - currentTime) / 1000).toInt()
                Toast.makeText(context, "Action locked. Try again in $remainingSeconds seconds", Toast.LENGTH_SHORT).show()
                lastToastTime = currentTime // Update the toast timer
            }
            return // Exit early, do not trigger the vote
        }

        // 3. Not locked! Lock it for exactly 10 seconds (10,000 ms) from now
        lockedPosts[post.postId] = currentTime + 10000L

        // 4. Execute the actual vote function in MainPage
        action()
    }

    override fun getItemCount() = postList.size
}