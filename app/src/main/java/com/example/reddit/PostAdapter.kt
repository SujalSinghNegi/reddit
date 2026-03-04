package com.example.reddit

import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.reddit.databinding.ItemPostBinding
import com.example.reddit.models.PostModel




// 1. Define an Interface for clicks
interface OnPostClickListener {
    fun onUpvoteClick(post: PostModel, position: Int)   // Added position for localized UI updates
    fun onDownvoteClick(post: PostModel, position: Int) // Added position for localized UI updates
    fun onCommentClick(post: PostModel)
}

class PostAdapter(
    private val context: Context,
    private val postList: List<PostModel>,
    private val listener: OnPostClickListener
) : RecyclerView.Adapter<PostAdapter.PostViewHolder>() {

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

        // --- NEW VISUAL LOGIC FOR VOTING ---
        val upvoteColor = Color.parseColor("#FF4500")   // Reddit Orange
        val downvoteColor = Color.parseColor("#7193FF") // Reddit Blue
        val defaultColor = Color.parseColor("#808080")  // Default Gray

        when (post.currentUserVote) {
            1 -> {
                // User upvoted: Color the up arrow orange, default the down arrow
                holder.binding.imgUpvote.setColorFilter(upvoteColor)
                holder.binding.imgDownvote.setColorFilter(defaultColor)
            }
            -1 -> {
                // User downvoted: Color the down arrow blue, default the up arrow
                holder.binding.imgUpvote.setColorFilter(defaultColor)
                holder.binding.imgDownvote.setColorFilter(downvoteColor)
            }
            else -> {
                // No vote: Default both arrows to gray
                holder.binding.imgUpvote.setColorFilter(defaultColor)
                holder.binding.imgDownvote.setColorFilter(defaultColor)
            }
        }

        // 3. Set Click Listeners (Passing the position so we can notifyItemChanged later)
        holder.binding.imgUpvote.setOnClickListener {
            listener.onUpvoteClick(post, position)
        }

        holder.binding.imgDownvote.setOnClickListener {
            listener.onDownvoteClick(post, position)
        }

        holder.binding.imgComment.setOnClickListener {
            listener.onCommentClick(post)
        }
    }

    override fun getItemCount() = postList.size
}