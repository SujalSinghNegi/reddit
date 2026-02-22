package com.example.reddit.login

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.GetCredentialException
import androidx.lifecycle.lifecycleScope
import com.example.reddit.R
import com.example.reddit.databinding.ActivityLoginPageBinding
import com.example.reddit.mainpage.MainPage
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch


class LoginPage : AppCompatActivity() {
    private val binding: ActivityLoginPageBinding by lazy {
        ActivityLoginPageBinding.inflate(layoutInflater)
    }
    private lateinit var auth: FirebaseAuth
    private lateinit var credentialManager: CredentialManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

//        binding.loginBtn.setOnClickListener {
//            val intent= Intent(this@LoginPage, MainPage::class.java)
//            startActivity(intent)
//            finish()
//        }
        var twiceClick : Int = 0
        binding.signUpBtn.setOnClickListener {
            val intent = Intent(this@LoginPage, SignUpActivity::class.java)
            startActivity(intent)
          if(twiceClick>0)  finish()
            twiceClick++
        }
        binding.emailBtn.setOnClickListener {
            val intent = Intent(this@LoginPage, EmailLoginPage::class.java)
            startActivity(intent)
            finish()
        }
        binding.phoneBtn.setOnClickListener {
            val intent = Intent(this@LoginPage, PhoneSignUpActivity::class.java)
            startActivity(intent)
            finish()
        }

        //  Initialize Firebase & Credential Manager
        auth = FirebaseAuth.getInstance()
        credentialManager = CredentialManager.create(this)

        binding.googleBtn.setOnClickListener {
            signInWithGoogle()
        }

    }

    override fun onStart() {
        super.onStart()
        val currentUser = auth.currentUser
        if (currentUser != null) {
            // Don't just update UI, check the DB to see if they have a profile
            checkUserDatabase(currentUser)
        }
    }

private fun signInWithGoogle(){
        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false) // Set true to only show accounts already signed in to the app
            .setServerClientId(getString(R.string.default_web_client_id))
            .setAutoSelectEnabled(true) // Auto-select if only one account exists
            .build()

        // 3. Build the Request
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        // 4. Launch the request using Coroutines
        lifecycleScope.launch {
            try {
                val result = credentialManager.getCredential(
                    request = request,
                    context = this@LoginPage
                )
                handleSignIn(result)
            } catch (e: GetCredentialException) {
                Log.e("CredMan", "GetCredential failed", e)
                Toast.makeText(baseContext, "Sign In Cancelled/Failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    private fun handleSignIn(result: GetCredentialResponse) {
        val credential = result.credential

        // 5. Check if the credential is of type Google ID
        if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
            try {
                // 6. Extract the ID Token
                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                val idToken = googleIdTokenCredential.idToken

                // 7. Authenticate with Firebase
                firebaseAuthWithGoogle(idToken)

            } catch (e: GoogleIdTokenParsingException) {
                Log.e("CredMan", "Received an invalid google id token response", e)
            }
        } else {
            Log.e("CredMan", "Unexpected type of credential")
        }
    }
    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)

        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    if (user != null) {
                        // Success! Now let the database logic decide next steps
                        checkUserDatabase(user)
                    }
                } else {
                    Toast.makeText(baseContext, "Authentication Failed.", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun handleNewUserSignup(user: FirebaseUser?) {
        Toast.makeText(this, "Account Created! Setting up profile...", Toast.LENGTH_LONG).show()

        // Example: Save basic info to Firestore
        // val db = FirebaseFirestore.getInstance()
        // val userData = hashMapOf("name" to user?.displayName, "email" to user?.email)
        // db.collection("users").document(user!!.uid).set(userData)

        updateUI(user)
    }

    private fun updateUI(user: FirebaseUser?) {
        if (user != null) {
            Toast.makeText(this, "Welcome ${user.displayName}", Toast.LENGTH_LONG).show()
            startActivity(Intent(this, MainPage::class.java))
            finish()
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
                    // Scenario 1: User is already in DB -> Go to Home
                    Log.d("Auth", "User found in Firestore, going to Main Page")
                    goToMainPage()
                } else {
                    // Scenario 2: User is Authenticated but NOT in DB
                    // This happens for new users or if the previous save failed

                    if (user.displayName != null && user.displayName!!.isNotEmpty()) {
                        // If it's a Google User, we have their name! Auto-save them.
                        saveGoogleUserToFirestore(user)
                    } else {
                        // If it's a Phone/Email user with no name yet -> Go to Profile Setup
                        goToProfileSetup()
                    }
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error checking database: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    // ---------------------------------------------------------
    // HELPER: Auto-Save Google Data to Firestore
    // ---------------------------------------------------------
    private fun saveGoogleUserToFirestore(user: FirebaseUser) {
        val db = FirebaseFirestore.getInstance()

        // Prepare the data using your UserModel
        // Make sure you created the UserModel.kt file as discussed previously!
        val userMap = hashMapOf(
            "uid" to user.uid,
            "name" to (user.displayName ?: "No Name"),
            "email" to (user.email ?: ""),
            "phoneNumber" to (user.phoneNumber ?: ""),
            "profileImage" to (user.photoUrl?.toString() ?: "")
        )

        db.collection("users").document(user.uid).set(userMap)
            .addOnSuccessListener {
                Log.d("Auth", "Google User saved to Firestore!")
                goToMainPage()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to save profile: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }

    // Navigation Helpers to keep code clean
    private fun goToMainPage() {
        startActivity(Intent(this, MainPage::class.java))
        finish()
    }

    private fun goToProfileSetup() {
        startActivity(Intent(this, ProfileSetupActivity::class.java))
        finish()
    }
}