package com.example.reddit.splash

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.reddit.R
import com.example.reddit.login.LoginPage
import com.example.reddit.mainpage.MainPage
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.lang.Thread.sleep

class SplashScreen : AppCompatActivity() {
    private lateinit var auth : FirebaseAuth
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_splash_screen)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        auth = FirebaseAuth.getInstance()
       // val loggedIN:Boolean = false
       lifecycleScope.launch {
           delay(500)

           if(auth.currentUser != null){
               val intent= Intent(this@SplashScreen, MainPage::class.java)
               startActivity(intent)
               finish()
           }else{
               val intent= Intent(this@SplashScreen, LoginPage::class.java)
               startActivity(intent)
               finish()
           }
       }

    }
}