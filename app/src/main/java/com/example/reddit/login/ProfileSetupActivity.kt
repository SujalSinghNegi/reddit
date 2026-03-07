package com.example.reddit.login

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

import android.content.Intent
import android.widget.Toast
import com.example.reddit.LoadingDialog
import com.example.reddit.databinding.ActivityProfileSetupBinding
import com.example.reddit.mainpage.MainPage
import com.example.reddit.models.UserModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ProfileSetupActivity : AppCompatActivity() {

    private val binding: ActivityProfileSetupBinding by lazy {
        ActivityProfileSetupBinding.inflate(layoutInflater)
    }
    private lateinit var loadingDialog: LoadingDialog
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        loadingDialog = LoadingDialog(this)
        val auth = FirebaseAuth.getInstance()
        val db = FirebaseFirestore.getInstance()

        binding.btnSubmit.setOnClickListener {
            val name = binding.etName.text.toString().trim()

            if (name.isEmpty()) {
                Toast.makeText(this, "Please enter your name", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            loadingDialog.startLoading()
            // Create the User Data object
            val user = UserModel(
                uid = auth.currentUser!!.uid,
                name = name,
                email = auth.currentUser!!.email ?: "", // Might be empty for phone users
                phoneNumber = auth.currentUser!!.phoneNumber ?: "", // Might be empty for email users
                profileImage = "" // You can add image upload logic here later
            )

            // Save to Firestore: users/UID
            db.collection("users").document(user.uid).set(user)
                .addOnSuccessListener {
                    // Update the Auth Profile locally too (Optional but good for performance)
                    // ... (Code to update FirebaseUserProfileChangeRequest if needed)
                    Toast.makeText(this, "Profile Saved!", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this, MainPage::class.java))
                    loadingDialog.dismissDialog()
                    finish()
                }
                .addOnFailureListener {
                    loadingDialog.dismissDialog()
                    Toast.makeText(this, "Error: ${it.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }
}