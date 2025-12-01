package com.example.reddit.login

import android.content.ContentValues.TAG
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.credentials.Credential
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest

import com.example.reddit.R
import com.example.reddit.databinding.ActivitySignUpBinding
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth


class SignUpActivity : AppCompatActivity() {
    private val binding: ActivitySignUpBinding by lazy {
        ActivitySignUpBinding.inflate(layoutInflater)
    }
    private lateinit var auth :FirebaseAuth
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        auth = Firebase.auth
        binding.loginBtn.setOnClickListener {
            val intent = Intent(this@SignUpActivity, LoginPage::class.java )
            startActivity(intent)
            finish()
        }
        binding.emailBtn.setOnClickListener {
            val intent = Intent(this@SignUpActivity, emailSignUpActivity::class.java )
            startActivity(intent)
            finish()
        }


//        binding.googleBtn.setOnClickListener {
//            // google signin builder
//
//            // Instantiate a Google sign-in request
//            val googleIdOption = GetGoogleIdOption.Builder()
//                // Your server's client ID, not your Android client ID.
//                .setServerClientId(getString(R.string.default_web_client_id))
//                // Only show accounts previously used to sign in.
//                .setFilterByAuthorizedAccounts(true)
//                .build()
//
//// Create the Credential Manager request
//            val request = GetCredentialRequest.Builder()
//                .addCredentialOption(googleIdOption)
//                .build()
//
//        }

    }


//    private fun firebaseAuthWithGoogle(idToken: String) {
//        val credential = GoogleAuthProvider.getCredential(idToken, null)
//        auth.signInWithCredential(credential)
//            .addOnCompleteListener(this) { task ->
//                if (task.isSuccessful) {
//                    // Sign in success, update UI with the signed-in user's information
//                    Log.d(TAG, "signInWithCredential:success")
//                    val user = auth.currentUser
//
//                } else {
//                    // If sign in fails, display a message to the user
//                    Log.w(TAG, "signInWithCredential:failure", task.exception)
//
//                }
//            }
//    }
//    private fun handleSignIn(credential: Credential) {
//        // Check if credential is of type Google ID
//        if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
//            // Create Google ID Token
//            val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
//
//            // Sign in to Firebase with using the token
//            firebaseAuthWithGoogle(googleIdTokenCredential.idToken)
//        } else {
//            Log.w(TAG, "Credential is not of type Google ID!")
//        }
//    }
}