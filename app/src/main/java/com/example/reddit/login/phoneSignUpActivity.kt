package com.example.reddit.login

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.reddit.databinding.ActivityPhoneSignUpBinding
import android.util.Log
import com.example.reddit.mainpage.MainPage
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import java.util.concurrent.TimeUnit

class phoneSignUpActivity : AppCompatActivity() {

    private val binding: ActivityPhoneSignUpBinding by lazy {
        ActivityPhoneSignUpBinding.inflate(layoutInflater)
    }

    // 1. Declare Firebase Auth and variables to hold IDs
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

        // 2. Initialize Firebase Auth
        auth = FirebaseAuth.getInstance()

        // 3. Setup the Listener (Callback) for the SMS response
        // This tells the app what to do when Firebase sends the SMS
        setupCallbacks()

        // 4. Handle "Send OTP" Click
        binding.sendOtp.setOnClickListener {
            val rawPhone = binding.phoneNumber.text.toString().trim()

            // Firebase requires E.164 format (e.g., +919999999999)
            // If user enters 10 digits, we assume it's a local number and add country code
            // CHANGE "+91" to your specific country code if needed
            if (rawPhone.length == 10) {
                val fullPhoneNumber = "+91$rawPhone"
                startPhoneNumberVerification(fullPhoneNumber)

                // UX: Disable button so they don't click twice
                binding.sendOtp.isEnabled = false
                binding.sendOtp.text = "Sending..."
            } else {
                Toast.makeText(this, "Enter valid 10 digit number", Toast.LENGTH_SHORT).show()
            }
        }

        // 5. Handle "Verify" Click
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
            .setPhoneNumber(phoneNumber)       // Phone number to verify
            .setTimeout(60L, TimeUnit.SECONDS) // Timeout and unit
            .setActivity(this)                 // Activity (for callback binding)
            .setCallbacks(callbacks)           // OnVerificationStateChangedCallbacks
            .build()
        PhoneAuthProvider.verifyPhoneNumber(options)
    }

    private fun setupCallbacks() {
        callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {

            // Called when verification is done automatically (rare but possible)
            override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                Log.d("PhoneAuth", "onVerificationCompleted:$credential")
                signInWithPhoneAuthCredential(credential)
            }

            // Called when verification fails (e.g. invalid number, quota exceeded)
            override fun onVerificationFailed(e: FirebaseException) {
                Log.w("PhoneAuth", "onVerificationFailed", e)
                Toast.makeText(baseContext, "Verification Failed: ${e.message}", Toast.LENGTH_LONG).show()

                // Re-enable the button so they can try again
                binding.sendOtp.isEnabled = true
                binding.sendOtp.text = "Send Otp"
            }

            // Called when the code is actually sent to the phone
            override fun onCodeSent(
                verificationId: String,
                token: PhoneAuthProvider.ForceResendingToken
            ) {
                Log.d("PhoneAuth", "onCodeSent:$verificationId")

                // Save ID and token for later use in the verify step
                storedVerificationId = verificationId
                resendToken = token

                Toast.makeText(baseContext, "OTP Sent Successfully", Toast.LENGTH_SHORT).show()

                // Update UI to show we are waiting for code
                binding.sendOtp.text = "OTP Sent"
            }
        }
    }

    private fun signInWithPhoneAuthCredential(credential: PhoneAuthCredential) {
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Sign In Success
                    val user = task.result?.user
                    Toast.makeText(this, "Success! Logged in as ${user?.phoneNumber}", Toast.LENGTH_SHORT).show()

                    // Navigate to Home Activity
                     val intent = Intent(this@phoneSignUpActivity, MainPage::class.java)
                     startActivity(intent)
                     finish()
                } else {
                    // Sign In Failed (Usually wrong OTP)
                    Toast.makeText(this, "Verification failed. Invalid OTP.", Toast.LENGTH_SHORT).show()
                }
            }
    }
}