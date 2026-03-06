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

import android.graphics.Bitmap
import androidx.lifecycle.lifecycleScope
import com.example.reddit.LoadingDialog
import id.zelory.compressor.Compressor
import id.zelory.compressor.constraint.format
import id.zelory.compressor.constraint.quality
import id.zelory.compressor.constraint.resolution
import id.zelory.compressor.constraint.size
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream


class AddPostActivity : AppCompatActivity() {

    private val binding: ActivityAddPostBinding by lazy {
        ActivityAddPostBinding.inflate(layoutInflater)
    }
    private lateinit var loadingDialog: LoadingDialog
    private var imageUri: Uri? = null

    private val pickImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            imageUri = uri
            binding.imgPreview.setImageURI(uri)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        loadingDialog = LoadingDialog(this)
        binding.imgPreview.setOnClickListener {
            pickImage.launch("image/*")
        }

        binding.btnPost.setOnClickListener {
            val caption = binding.etCaption.text.toString()

            if (imageUri != null && caption != "") {
                loadingDialog.startLoading()
                compressAndUploadImage(caption)
            } else if(caption == ""){
                Toast.makeText(this, "Please enter a caption", Toast.LENGTH_SHORT).show()
            }else {
                Toast.makeText(this, "Please select an image first", Toast.LENGTH_SHORT).show()
            }

        }

    }

    private fun compressAndUploadImage(caption: String) {
        // 1. Show Loading UI immediately
        binding.btnPost.isEnabled = false

        // 2. Launch a Coroutine tied to the Activity's lifecycle
        lifecycleScope.launch {
            try {
                // STEP A: Safely convert the Uri to a File in the background
                val originalFile = getFileFromUri(imageUri!!)

                if (originalFile == null) {
                    showErrorAndResetUI("Failed to process image file")
                    return@launch
                }

                // STEP B: Compress the image
                // Compressor automatically handles background threading so the UI won't freeze
                val compressedFile = Compressor.compress(this@AddPostActivity, originalFile) {
                    resolution(1280, 720)
                    quality(80)
                    format(Bitmap.CompressFormat.WEBP) // WEBP is smaller and faster
                    size(1_048_576) // Max 1MB
                }

                // STEP C: Convert the compressed File back to a Uri for Firebase
                val compressedUri = Uri.fromFile(compressedFile)

                // STEP D: Upload to Firebase
                uploadToFirebaseStorage(compressedUri, caption)

            } catch (e: Exception) {
                e.printStackTrace()
                showErrorAndResetUI("Compression failed: ${e.message}")
            }

        }
    }

    // Extracted the Firebase logic to keep it clean.
    // Notice we changed the file extension to .webp!
    private fun uploadToFirebaseStorage(compressedUri: Uri, caption: String) {
        val fileName = UUID.randomUUID().toString() + ".webp" // Changed to .webp
        val ref = FirebaseStorage.getInstance().reference.child("post_images/$fileName")

        ref.putFile(compressedUri)
            .addOnSuccessListener {
                ref.downloadUrl.addOnSuccessListener { uri ->
                    savePostToFirestore(uri.toString(), caption)
                }
            }
            .addOnFailureListener {
                showErrorAndResetUI("Upload failed: ${it.message}")
            }
    }

    private fun savePostToFirestore(imageUrl: String, caption: String) {
        val db = FirebaseFirestore.getInstance()
        val auth = FirebaseAuth.getInstance()
        val currentUser = auth.currentUser

        if (currentUser == null) {
            showErrorAndResetUI("Session Expired. Please Login again.")
            return
        }

        db.collection("users").document(currentUser.uid).get()
            .addOnSuccessListener { document ->
                val nameFromDb = document.getString("name") ?: "Anonymous Reddit User"
                val postRef = db.collection("posts").document()
                val newPostId = postRef.id

                val post = PostModel(
                    postId = newPostId,
                    authorId = currentUser.uid,
                    userName = nameFromDb,
                    imageUrl = imageUrl,
                    caption = caption,
                    voteCount = 0,
                    timestamp = System.currentTimeMillis()
                )

                postRef.set(post)
                    .addOnSuccessListener {
                        loadingDialog.dismissDialog()
                        Toast.makeText(this, "Post Uploaded!", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                    .addOnFailureListener {
                        showErrorAndResetUI("Error: ${it.message}")
                    }
            }
            .addOnFailureListener {
                showErrorAndResetUI("Failed to fetch user details")
            }
    }

    // --- HELPER FUNCTIONS ---

    /**
     * Safely copies the selected Uri to a temporary file in the app's cache.
     * Runs on the IO thread to prevent StrictMode violations or UI stutter.
     */
    private suspend fun getFileFromUri(uri: Uri): File? = withContext(Dispatchers.IO) {
        return@withContext try {
            val inputStream = contentResolver.openInputStream(uri)
            // Create a temporary file in the cache directory
            val tempFile = File.createTempFile("temp_image", ".jpg", cacheDir)
            val outputStream = FileOutputStream(tempFile)

            // Copy data
            inputStream?.copyTo(outputStream)

            // Clean up streams
            inputStream?.close()
            outputStream.close()

            tempFile
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Helper to reset the UI if anything fails
     */
    private fun showErrorAndResetUI(errorMessage: String) {
        loadingDialog.dismissDialog()
        binding.btnPost.isEnabled = true
        Toast.makeText(this@AddPostActivity, errorMessage, Toast.LENGTH_SHORT).show()
    }
}