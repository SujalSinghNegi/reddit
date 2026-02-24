package com.example.reddit

import android.app.Application
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory

class RedditCloneApp : Application() {
    override fun onCreate() {
        super.onCreate()

        // 1. Initialize Firebase first
        FirebaseApp.initializeApp(this)

        // 2. Install App Check globally for the whole app
        val firebaseAppCheck = FirebaseAppCheck.getInstance()
        firebaseAppCheck.installAppCheckProviderFactory(
            PlayIntegrityAppCheckProviderFactory.getInstance()
        )
    }
}