package com.example.reddit

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.reddit.databinding.ItemPostBinding
import com.example.reddit.models.PostModel


// 1. Define an Interface for clicks
interface OnPostClickListener {
    fun onUpvoteClick(post: PostModel)
    fun onDownvoteClick(post: PostModel)
    fun onCommentClick(post: PostModel)
}

class PostAdapter(
    private val context: Context,
    private val postList: List<PostModel>,
    private val listener: OnPostClickListener // 2. Pass listener here
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

        // 3. Set Click Listeners
        holder.binding.imgUpvote.setOnClickListener {
            listener.onUpvoteClick(post)
        }

        holder.binding.imgDownvote.setOnClickListener {
            listener.onDownvoteClick(post)
        }

        holder.binding.imgComment.setOnClickListener {
            listener.onCommentClick(post)
        }
    }

    override fun getItemCount() = postList.size
}