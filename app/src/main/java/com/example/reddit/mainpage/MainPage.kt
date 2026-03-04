package com.example.reddit.mainpage
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.reddit.OnPostClickListener
import com.example.reddit.PostAdapter
import com.example.reddit.R
import com.example.reddit.databinding.ActivityMainPageBinding
import com.example.reddit.login.LoginPage
import com.example.reddit.models.PostModel

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class MainPage : AppCompatActivity(), OnPostClickListener {

    private val binding: ActivityMainPageBinding by lazy {
        ActivityMainPageBinding.inflate(layoutInflater)
    }

    private lateinit var adapter: PostAdapter
    private val postList = ArrayList<PostModel>()
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        auth = FirebaseAuth.getInstance()

        // 1. Safety Check
        if (auth.currentUser == null) {
            goToLogin()
            return
        }

        // 2. Setup RecyclerView
        binding.recyclerView.layoutManager = LinearLayoutManager(this)

        // 3. Pass 'this' as the listener
        adapter = PostAdapter(this, postList, this)
        binding.recyclerView.adapter = adapter

        fetchPosts()

        // 4. FAB Button
        binding.fabAddPost.setOnClickListener {
            startActivity(Intent(this, AddPostActivity::class.java))
        }
    }

    // --- VOTING LOGIC ---
    override fun onUpvoteClick(post: PostModel, position: Int) {
        val db = FirebaseFirestore.getInstance()
        val currentUserId = auth.currentUser?.uid ?: return

        val postRef = db.collection("posts").document(post.postId)
        val voteRef = postRef.collection("votes").document(currentUserId)

        // 1. OPTIMISTIC UI UPDATE (Feels instant to the user)
        val previousVote = post.currentUserVote
        val previousVoteCount = post.voteCount

        when (previousVote) {
            1 -> { // Undo upvote
                post.currentUserVote = 0
                post.voteCount -= 1
            }
            -1 -> { // Switch downvote to upvote
                post.currentUserVote = 1
                post.voteCount += 2
            }
            else -> { // Fresh upvote
                post.currentUserVote = 1
                post.voteCount += 1
            }
        }
        adapter.notifyItemChanged(position)

        // 2. FIRESTORE TRANSACTION (Background sync)
        db.runTransaction { transaction ->
            val voteSnapshot = transaction.get(voteRef)
            val currentVote = if (voteSnapshot.exists()) voteSnapshot.getLong("voteType") ?: 0L else 0L

            when (currentVote) {
                1L -> { // Undo
                    transaction.delete(voteRef)
                    transaction.update(postRef, "voteCount", FieldValue.increment(-1))
                }
                -1L -> { // Switch
                    transaction.set(voteRef, mapOf("voteType" to 1L))
                    transaction.update(postRef, "voteCount", FieldValue.increment(2))
                }
                else -> { // Fresh
                    transaction.set(voteRef, mapOf("voteType" to 1L))
                    transaction.update(postRef, "voteCount", FieldValue.increment(1))
                }
            }
            null
        }.addOnFailureListener { e ->
            // Revert UI if the server update fails
            post.currentUserVote = previousVote
            post.voteCount = previousVoteCount
            adapter.notifyItemChanged(position)
            Toast.makeText(this, "Vote failed: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDownvoteClick(post: PostModel, position: Int) {
        val db = FirebaseFirestore.getInstance()
        val currentUserId = auth.currentUser?.uid ?: return

        val postRef = db.collection("posts").document(post.postId)
        val voteRef = postRef.collection("votes").document(currentUserId)

        // 1. OPTIMISTIC UI UPDATE
        val previousVote = post.currentUserVote
        val previousVoteCount = post.voteCount

        when (previousVote) {
            -1 -> { // Undo downvote
                post.currentUserVote = 0
                post.voteCount += 1
            }
            1 -> { // Switch upvote to downvote
                post.currentUserVote = -1
                post.voteCount -= 2
            }
            else -> { // Fresh downvote
                post.currentUserVote = -1
                post.voteCount -= 1
            }
        }
        adapter.notifyItemChanged(position)

        // 2. FIRESTORE TRANSACTION
        db.runTransaction { transaction ->
            val voteSnapshot = transaction.get(voteRef)
            val currentVote = if (voteSnapshot.exists()) voteSnapshot.getLong("voteType") ?: 0L else 0L

            when (currentVote) {
                -1L -> { // Undo
                    transaction.delete(voteRef)
                    transaction.update(postRef, "voteCount", FieldValue.increment(1))
                }
                1L -> { // Switch
                    transaction.set(voteRef, mapOf("voteType" to -1L))
                    transaction.update(postRef, "voteCount", FieldValue.increment(-2))
                }
                else -> { // Fresh
                    transaction.set(voteRef, mapOf("voteType" to -1L))
                    transaction.update(postRef, "voteCount", FieldValue.increment(-1))
                }
            }
            null
        }.addOnFailureListener { e ->
            // Revert UI if the server update fails
            post.currentUserVote = previousVote
            post.voteCount = previousVoteCount
            adapter.notifyItemChanged(position)
            Toast.makeText(this, "Vote failed: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCommentClick(post: PostModel) {
        Toast.makeText(this, "Comment clicked", Toast.LENGTH_SHORT).show()
        // TODO: Open Comment Activity
    }

    // --- DATA FETCHING ---
    private fun fetchPosts() {
        val db = FirebaseFirestore.getInstance()
        val currentUserId = auth.currentUser?.uid ?: return

        db.collection("posts")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { value, error ->
                if (error != null) {
                    Toast.makeText(this, "Error: ${error.message}", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }

                if (value != null) {
                    // Use Coroutines to fetch the subcollection data asynchronously
                    lifecycleScope.launch(Dispatchers.IO) {
                        val tempPostList = ArrayList<PostModel>()

                        for (document in value.documents) {
                            val post = document.toObject(PostModel::class.java)
                            if (post != null) {
                                // Fetch the specific user's vote for this post
                                try {
                                    val voteDoc = db.collection("posts").document(post.postId)
                                        .collection("votes").document(currentUserId)
                                        .get().await() // Suspends until data is fetched

                                    if (voteDoc.exists()) {
                                        post.currentUserVote = (voteDoc.getLong("voteType") ?: 0L).toInt()
                                    }
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                                tempPostList.add(post)
                            }
                        }

                        // Switch back to the Main thread to update the UI
                        withContext(Dispatchers.Main) {
                            postList.clear()
                            postList.addAll(tempPostList)
                            adapter.notifyDataSetChanged()
                        }
                    }
                }
            }
    }

    // --- Menu Logic ---
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.action_logout) {
            signOutAndStartOver()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun signOutAndStartOver() {
        auth.signOut()
        val credentialManager = CredentialManager.create(this)

        lifecycleScope.launch {
            try {
                credentialManager.clearCredentialState(ClearCredentialStateRequest())
            } catch (e: Exception) {
                e.printStackTrace()
            }
            Toast.makeText(this@MainPage, "Logged Out Successfully", Toast.LENGTH_SHORT).show()
            goToLogin()
        }
    }

    private fun goToLogin() {
        val intent = Intent(this, LoginPage::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}