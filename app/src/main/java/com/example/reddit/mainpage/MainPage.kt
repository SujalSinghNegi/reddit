package com.example.reddit.mainpage

import android.content.Intent

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem

import android.widget.Toast

import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat.startActivity

import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.reddit.PostAdapter
import com.example.reddit.R
import com.example.reddit.databinding.ActivityMainPageBinding

import com.example.reddit.login.LoginPage
import com.example.reddit.models.PostModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth

import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainPage : AppCompatActivity(), com.example.reddit.OnPostClickListener {

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

        // 2. Pass 'this' as the listener
        adapter = PostAdapter(this, postList, this)
        binding.recyclerView.adapter = adapter

        fetchPosts()

        // 4. FAB Button
        binding.fabAddPost.setOnClickListener {
            startActivity(Intent(this, AddPostActivity::class.java))
        }
    }
    override fun onUpvoteClick(post: PostModel) {
        Toast.makeText(this, "Upvoted: ${post.caption}", Toast.LENGTH_SHORT).show()
        // TODO: Add Firestore Logic for Upvote
    }

    override fun onDownvoteClick(post: PostModel) {
        Toast.makeText(this, "Downvoted", Toast.LENGTH_SHORT).show()
        // TODO: Add Firestore Logic for Downvote
    }

    override fun onCommentClick(post: PostModel) {
        Toast.makeText(this, "Comment clicked", Toast.LENGTH_SHORT).show()
        // TODO: Open Comment Activity
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

    // --- Improved Sign Out Function ---
    private fun signOutAndStartOver() {
        // 1. Sign out from Firebase
        auth.signOut()

        // 2. Clear Credential Manager State (Google Session)
        val credentialManager = CredentialManager.create(this)

        lifecycleScope.launch {
            try {
                credentialManager.clearCredentialState(ClearCredentialStateRequest())
            } catch (e: Exception) {
                e.printStackTrace()
            }

            // 3. Navigate after clearing state
            Toast.makeText(this@MainPage, "Logged Out Successfully", Toast.LENGTH_SHORT).show()
            goToLogin()
        }
    }

    private fun goToLogin() {
        val intent = Intent(this, LoginPage::class.java)
        // Clear Back Stack so user cannot press "Back" to return
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun fetchPosts() {
        val db = FirebaseFirestore.getInstance()
        db.collection("posts")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { value, error ->
                if (error != null) {
                    Toast.makeText(this, "Error: ${error.message}", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }

                if (value != null) {
                    postList.clear()
                    for (document in value.documents) {
                        val post = document.toObject(PostModel::class.java)
                        if (post != null) {
                            postList.add(post)
                        }
                    }
                    adapter.notifyDataSetChanged()
                }
            }
    }
}