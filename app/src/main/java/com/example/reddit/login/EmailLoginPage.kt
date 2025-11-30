package com.example.reddit.login

import android.content.Intent
import android.os.Bundle
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

        binding.loginBtn.setOnClickListener {
            val email = binding.emailId.text.toString()
            val password = binding.password.text.toString()
            if(!email.isEmpty() && !password.isEmpty()){
                auth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener { task ->
                        if(task.isSuccessful){
                            Toast.makeText(this, "Login Successful", Toast.LENGTH_SHORT).show()
                            startActivity((Intent(this, MainPage::class.java)))
                            finish()
                        }else{
                            Toast.makeText(this, "Error : ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                        }
                        }
            }
        }

    }
}