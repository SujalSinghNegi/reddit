package com.example.reddit.login

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.reddit.databinding.ActivityEmailSignUpBinding
import com.example.reddit.mainpage.MainPage
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore

class EmailSignUpActivity : AppCompatActivity() {

    private val binding: ActivityEmailSignUpBinding by lazy {
        ActivityEmailSignUpBinding.inflate(layoutInflater)
    }
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState) // Initialize super first

        auth = FirebaseAuth.getInstance()

        enableEdgeToEdge()
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        binding.signUpBtn.setOnClickListener {
            val email = binding.emailId.text.toString().trim()
            val password = binding.password.text.toString().trim()

            if (email.isNotEmpty() && password.isNotEmpty()) {
                // Create the user
                auth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            Toast.makeText(this, "Account Created Successfully", Toast.LENGTH_SHORT).show()

                            // User is now logged in automatically.
                            // Check database to see where to send them (likely Profile Setup)
                            val user = auth.currentUser
                            if (user != null) {
                                checkUserDatabase(user)
                            }
                        } else {
                            Toast.makeText(this, "Error: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
            } else {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // ---------------------------------------------------------
    // CORE LOGIC: Check if User Exists in Database
    // ---------------------------------------------------------
    private fun checkUserDatabase(user: FirebaseUser) {
        val db = FirebaseFirestore.getInstance()

        db.collection("users").document(user.uid).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    // This is rare for a brand new signup, but good safety check
                    goToMainPage()
                } else {
                    // New Email User -> Definitely needs to set a Name
                    // because Email Sign Up doesn't ask for a name by default.
                    goToProfileSetup()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun goToMainPage() {
        val intent = Intent(this, MainPage::class.java)
        // Clear back stack so they can't go back to signup
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun goToProfileSetup() {
        val intent = Intent(this, ProfileSetupActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}