package com.example.reddit.login

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.reddit.R
import com.example.reddit.databinding.ActivityLoginPageBinding
import com.example.reddit.databinding.ActivityMainBinding
import com.example.reddit.mainpage.MainPage


class LoginPage : AppCompatActivity() {
    private val binding: ActivityLoginPageBinding by lazy {
        ActivityLoginPageBinding.inflate(layoutInflater)
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        binding.loginBtn.setOnClickListener {
            val intent= Intent(this@LoginPage, MainPage::class.java)
            startActivity(intent)
            finish()
        }
        binding.signUpBtn.setOnClickListener {
            val intent = Intent(this@LoginPage, SignUpActivity::class.java)
            startActivity(intent)
            finish()
        }
        binding.emailBtn.setOnClickListener {
            val intent = Intent(this@LoginPage, EmailLoginPage::class.java)
            startActivity(intent)
            finish()
        }
    }
}