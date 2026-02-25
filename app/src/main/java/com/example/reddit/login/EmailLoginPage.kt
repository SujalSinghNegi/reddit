package com.example.reddit.login

import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.reddit.MainActivity
import com.example.reddit.R
import com.example.reddit.databinding.ActivityEmailLoginPageBinding
import com.example.reddit.mainpage.MainPage
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException

class EmailLoginPage : AppCompatActivity() {
    private val binding: ActivityEmailLoginPageBinding by lazy {
        ActivityEmailLoginPageBinding.inflate(layoutInflater)
    }
    private lateinit var auth: FirebaseAuth
    override fun onCreate(savedInstanceState: Bundle?) {
        auth = FirebaseAuth.getInstance()

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

//        binding.loginBtn.setOnClickListener {
//            val email = binding.emailId.text.toString()
//            val password = binding.password.text.toString()
//            if(!email.isEmpty() && !password.isEmpty()){
//                auth.signInWithEmailAndPassword(email, password)
//                    .addOnCompleteListener { task ->
//                        if(task.isSuccessful){
//                            Toast.makeText(this, "Login Successful", Toast.LENGTH_SHORT).show()
//                            startActivity((Intent(this, MainPage::class.java)))
//                            finish()
//                        }else{
//                            Toast.makeText(this, "Error : ${task.exception?.message}", Toast.LENGTH_SHORT).show()
//                        }
//                        }
//            }
//        }
        binding.loginBtn.setOnClickListener {
            // Added .trim() to prevent invisible trailing spaces from breaking the login
            val email = binding.emailId.text.toString().trim()
            val password = binding.password.text.toString().trim()

            if (email.isNotEmpty() && password.isNotEmpty()) {

                // Optional: Disable button while loading to prevent double-clicks
                binding.loginBtn.isEnabled = false

                auth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener { task ->
                        // Re-enable the button once the task completes
                        binding.loginBtn.isEnabled = true

                        if (task.isSuccessful) {
                            Toast.makeText(this, "Welcome back!", Toast.LENGTH_SHORT).show()
                            startActivity(Intent(this, MainPage::class.java))
                            finish()
                        } else {
                            // Route the specific Firebase exception to the right UI response
                            when (val exception = task.exception) {
                                is FirebaseAuthInvalidUserException -> {
                                    // Triggered when the email is not registered at all
                                    binding.emailId.error = "No account found with this email"
                                    binding.emailId.requestFocus()
                                    Toast.makeText(this, "User not found. Please sign up.", Toast.LENGTH_SHORT).show()
                                }
                                is FirebaseAuthInvalidCredentialsException -> {
                                    // Triggered when the password is wrong or email format is bad
                                    binding.passwordInputLayout.error = "Incorrect password"
                                    binding.password.requestFocus()
                                    Toast.makeText(this, "Incorrect password.", Toast.LENGTH_SHORT).show()
                                }
                                else -> {
                                    // Triggered by network timeouts, blocked devices, etc.
                                    Toast.makeText(this, "Login Error: ${exception?.message}", Toast.LENGTH_LONG).show()
                                }
                            }
                        }
                    }
            } else {
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
            }
        }


                binding.forgotPasswordText.setOnClickListener {
                    val email = binding.emailId.text.toString().trim()

                    // 1. Check for empty email
                    if (email.isEmpty()) {
                        binding.emailInputLayout.error = "Enter your email to reset password"
                        binding.emailId.requestFocus()
                        return@setOnClickListener
                    }

                    // 2. Immediately disable the button
                    binding.forgotPasswordText.isEnabled = false
// ----------------------- THIS WILL NOT WORK FOR NOW , FOR THE SAFETY PURPOSES, I HAVE INTENTION DIDNT TURN OF THE EMAIL ENUMERATION EVEN IF THE OPTION IS THERE OR NOT -------------------

                    // 3. The Hack: Try to log in with a fake password
                    auth.signInWithEmailAndPassword(email, "000000")
                        .addOnCompleteListener { task ->

                            if (task.isSuccessful) {
                                // This will basically never happen unless their actual password is "000000"
                                Toast.makeText(this, "Wait, is your password actually 000000?", Toast.LENGTH_SHORT).show()
                                binding.forgotPasswordText.isEnabled = true
                            } else {
                                // Route the specific Firebase exception
                                when (val exception = task.exception) {
                                    is FirebaseAuthInvalidUserException -> {
                                        // SUCCESS! The hack caught the unregistered user!
                                        binding.forgotPasswordText.isEnabled = true
                                        binding.emailInputLayout.error = "No account found with this email"
                                        binding.emailId.requestFocus()
                                        Toast.makeText(this, "User not found. Please sign up.", Toast.LENGTH_SHORT).show()
                                    }
                                    is FirebaseAuthInvalidCredentialsException -> {
                                        // The user exists, but the dummy password failed. Send the reset link!
                                        auth.sendPasswordResetEmail(email)
                                            .addOnCompleteListener { resetTask ->
                                                if (resetTask.isSuccessful) {
                                                    // Check the Spam Folder
                                                    Toast.makeText(this, "Reset link sent! Check your inbox (Spam).", Toast.LENGTH_LONG).show()

                                                    // Start the 60-second timer
                                                    object : CountDownTimer(60000, 1000) {
                                                        override fun onTick(millisUntilFinished: Long) {
                                                            val secondsRemaining = millisUntilFinished / 1000
                                                            binding.forgotPasswordText.text = "Resend in ${secondsRemaining}s"
                                                        }

                                                        override fun onFinish() {
                                                            binding.forgotPasswordText.isEnabled = true
                                                            binding.forgotPasswordText.text = "Forgot password?"
                                                        }
                                                    }.start()

                                                } else {
                                                    binding.forgotPasswordText.isEnabled = true
                                                    binding.forgotPasswordText.text = "Forgot password?"
                                                    Toast.makeText(this, "Reset Error: ${resetTask.exception?.message}", Toast.LENGTH_LONG).show()
                                                }
                                            }
                                    }
                                    else -> {
                                        // Network error
                                        binding.forgotPasswordText.isEnabled = true
                                        Toast.makeText(this, "Internet Error: ${exception?.message}", Toast.LENGTH_LONG).show()
                                    }
                                }
                            }
                        }
  // -------------------------------------------------- DISABLED AUTOMATICALLY FOR SAFETY PURPOSES BY THE FIREBASE ----------------------------------------------------------------
                }




    }
}