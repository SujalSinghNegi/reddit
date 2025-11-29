package com.example.reddit.login

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.reddit.R
import com.example.reddit.databinding.ActivityEmailSignUpBinding
import com.google.firebase.auth.FirebaseAuth

class emailSignUpActivity : AppCompatActivity() {
    private val binding: ActivityEmailSignUpBinding by lazy {
        ActivityEmailSignUpBinding.inflate(layoutInflater)
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
        binding.signUpBtn.setOnClickListener {
            val email = binding.emailId.text.toString()
            val password = binding.password.text.toString()
            if(!email.isEmpty() && !password.isEmpty()){
                auth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener { task ->
                        if(task.isSuccessful){
                            Toast.makeText(this, "Account Created Successfully", Toast.LENGTH_SHORT).show()
                            startActivity((Intent(this, EmailLoginPage::class.java)))
                            finish()
                        }else{
                            Toast.makeText(this, "Some Error Occurred", Toast.LENGTH_SHORT).show()
                        }
                    }
            }
        }

    }
}