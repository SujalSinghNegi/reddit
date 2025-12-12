package com.example.reddit.login

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.reddit.databinding.ActivityPhoneSignUpBinding
import com.example.reddit.mainpage.MainPage
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import java.util.concurrent.TimeUnit

class PhoneSignUpActivity : AppCompatActivity() {

    private val binding: ActivityPhoneSignUpBinding by lazy {
        ActivityPhoneSignUpBinding.inflate(layoutInflater)
    }

    private lateinit var auth: FirebaseAuth
    private var storedVerificationId: String = ""
    private lateinit var resendToken: PhoneAuthProvider.ForceResendingToken
    private lateinit var callbacks: PhoneAuthProvider.OnVerificationStateChangedCallbacks

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        auth = FirebaseAuth.getInstance()

        setupCallbacks()

        // Handle "Send OTP"
        binding.sendOtp.setOnClickListener {
            val rawPhone = binding.phoneNumber.text.toString().trim()

            if (rawPhone.length == 10) {
                // Assuming +91 for India. Change if needed.
                val fullPhoneNumber = "+91$rawPhone"
                startPhoneNumberVerification(fullPhoneNumber)

                binding.sendOtp.isEnabled = false
                binding.sendOtp.text = "Sending..."
            } else {
                Toast.makeText(this, "Enter valid 10 digit number", Toast.LENGTH_SHORT).show()
            }
        }

        // Handle "Verify"
        binding.verify.setOnClickListener {
            val code = binding.otp.text.toString().trim()
            if (code.isEmpty() || code.length < 6) {
                Toast.makeText(this, "Please enter valid OTP", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (storedVerificationId.isNotEmpty()) {
                val credential = PhoneAuthProvider.getCredential(storedVerificationId, code)
                signInWithPhoneAuthCredential(credential)
            } else {
                Toast.makeText(this, "Error: No Verification ID found", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // --- Helper Functions ---

    private fun startPhoneNumberVerification(phoneNumber: String) {
        val options = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber(phoneNumber)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(this)
            .setCallbacks(callbacks)
            .build()
        PhoneAuthProvider.verifyPhoneNumber(options)
    }

    private fun setupCallbacks() {
        callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                Log.d("PhoneAuth", "onVerificationCompleted:$credential")
                signInWithPhoneAuthCredential(credential)
            }

            override fun onVerificationFailed(e: FirebaseException) {
                Log.w("PhoneAuth", "onVerificationFailed", e)
                Toast.makeText(baseContext, "Verification Failed: ${e.message}", Toast.LENGTH_LONG).show()
                binding.sendOtp.isEnabled = true
                binding.sendOtp.text = "Send Otp"
            }

            override fun onCodeSent(
                verificationId: String,
                token: PhoneAuthProvider.ForceResendingToken
            ) {
                Log.d("PhoneAuth", "onCodeSent:$verificationId")
                storedVerificationId = verificationId
                resendToken = token
                Toast.makeText(baseContext, "OTP Sent Successfully", Toast.LENGTH_SHORT).show()
                binding.sendOtp.text = "OTP Sent"
            }
        }
    }

    private fun signInWithPhoneAuthCredential(credential: PhoneAuthCredential) {
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val user = task.result?.user
                    if (user != null) {
                        Toast.makeText(this, "Verified! Checking profile...", Toast.LENGTH_SHORT).show()
                        // Instead of going straight to Home, we check the DB first
                        checkUserDatabase(user)
                    }
                } else {
                    Toast.makeText(this, "Verification failed. Invalid OTP.", Toast.LENGTH_SHORT).show()
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
                    // Returning user -> Go to Home
                    goToMainPage()
                } else {
                    // New Phone User -> Definitely needs a Name -> Go to Profile Setup
                    goToProfileSetup()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun goToMainPage() {
        val intent = Intent(this, MainPage::class.java)
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