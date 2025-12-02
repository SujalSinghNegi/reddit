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
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException

import com.example.reddit.R
import com.example.reddit.databinding.ActivitySignUpBinding
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

private const val WEB_CLIENT_ID = "749075529146-acn9prom81b8mvuv96ljgvcrn6e1esia.apps.googleusercontent.com"

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


        binding.googleBtn.setOnClickListener {
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

            signInWithGoogle(this)
        }

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



    fun signInWithGoogle(activity: android.app.Activity) {
        val credentialManager = CredentialManager.create(activity)

        // 2. Configure the Google Sign-In Request
        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false) // Set to true for auto-signin on return visits
            .setServerClientId(WEB_CLIENT_ID)
            .setAutoSelectEnabled(true)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        // 3. Launch the selector
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val result = credentialManager.getCredential(
                    request = request,
                    context = activity
                )
                handleSignIn(result.credential)
            } catch (e: GetCredentialException) {
                Log.e("Auth", "Google Sign-In failed: ${e.message}")
                // Handle cancellation or no credentials found
            }
        }
    }

    // 4. Exchange Token with Firebase
    private fun handleSignIn(credential: androidx.credentials.Credential) {
        if (credential is CustomCredential &&
            credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {

            try {
                // Extract the Google ID Token
                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                val googleIdToken = googleIdTokenCredential.idToken

                // Authenticate with Firebase
                val authCredential = GoogleAuthProvider.getCredential(googleIdToken, null)

                Firebase.auth.signInWithCredential(authCredential)
                    .addOnSuccessListener { authResult ->
                        Log.d("Auth", "Success! User: ${authResult.user?.email}")
                        // Navigate to your Home Activity here
                    }
                    .addOnFailureListener { e ->
                        Log.e("Auth", "Firebase sign-in failed", e)
                    }

            } catch (e: Exception) {
                Log.e("Auth", "Invalid Google Credential", e)
            }
        } else {
            Log.e("Auth", "Unexpected credential type")
        }
    }


}