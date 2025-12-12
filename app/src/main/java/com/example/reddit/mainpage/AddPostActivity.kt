package com.example.reddit.mainpage

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

import android.net.Uri

import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts

import com.example.reddit.databinding.ActivityAddPostBinding
import com.example.reddit.models.PostModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

import com.google.firebase.storage.FirebaseStorage
import java.util.UUID

class AddPostActivity : AppCompatActivity() {

    private val binding: ActivityAddPostBinding by lazy {
        ActivityAddPostBinding.inflate(layoutInflater)
    }

    private var imageUri: Uri? = null // To store the selected image temporarily

    // Image Picker Launcher
    private val pickImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            imageUri = uri
            binding.imgPreview.setImageURI(uri) // Show the selected image
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        // 1. Click Image to Open Gallery
        binding.imgPreview.setOnClickListener {
            pickImage.launch("image/*")
        }

        // 2. Click Post Button
        binding.btnPost.setOnClickListener {
            val caption = binding.etCaption.text.toString()

            if (imageUri != null) {
                uploadImageToStorage(caption)
            } else {
                Toast.makeText(this, "Please select an image first", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun uploadImageToStorage(caption: String) {
        // Show Loading
        binding.progressBar.visibility = View.VISIBLE
        binding.btnPost.isEnabled = false

        // Create a unique filename using UUID
        val fileName = UUID.randomUUID().toString() + ".jpg"

        // Reference to where we want to save the image in Storage
        val ref = FirebaseStorage.getInstance().reference.child("post_images/$fileName")

        // Upload the file
        ref.putFile(imageUri!!)
            .addOnSuccessListener {
                // If upload success, get the Download URL
                ref.downloadUrl.addOnSuccessListener { uri ->
                    savePostToFirestore(uri.toString(), caption)
                }
            }
            .addOnFailureListener {
                binding.progressBar.visibility = View.GONE
                binding.btnPost.isEnabled = true
                Toast.makeText(this, "Upload failed: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }
    private fun savePostToFirestore(imageUrl: String, caption: String) {
        val db = FirebaseFirestore.getInstance()
        val auth = FirebaseAuth.getInstance()
        val currentUser = auth.currentUser

        if (currentUser == null) return

        // STEP 1: Fetch the User's Name from the 'users' collection first
        db.collection("users").document(currentUser.uid).get()
            .addOnSuccessListener { document ->

                // Get name from DB, or fallback to "Anonymous" if missing
                val nameFromDb = document.getString("name") ?: "Anonymous Reddit User"

                // STEP 2: Now create the post with the correct name
                val postRef = db.collection("posts").document()
                val newPostId = postRef.id

                val post = PostModel(
                    postId = newPostId,
                    authorId = currentUser.uid,
                    userName = nameFromDb, // <--- USING THE FETCHED NAME
                    imageUrl = imageUrl,
                    caption = caption,
                    voteCount = 0,
                    timestamp = System.currentTimeMillis()
                )

                // STEP 3: Save the post
                postRef.set(post)
                    .addOnSuccessListener {
                        binding.progressBar.visibility = View.GONE
                        Toast.makeText(this, "Post Uploaded!", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                    .addOnFailureListener {
                        binding.progressBar.visibility = View.GONE
                        binding.btnPost.isEnabled = true
                        Toast.makeText(this, "Error: ${it.message}", Toast.LENGTH_SHORT).show()
                    }
            }
            .addOnFailureListener {
                // If we couldn't fetch the user details, fail gracefully
                binding.progressBar.visibility = View.GONE
                binding.btnPost.isEnabled = true
                Toast.makeText(this, "Failed to fetch user details", Toast.LENGTH_SHORT).show()
            }
    }
}