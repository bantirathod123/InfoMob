package com.charyatani.InfoMob

import android.app.Application
import com.google.firebase.FirebaseApp

class MyApp : Application() {
    override fun onCreate() {
        super.onCreate()

        // Initialize Firebase only once in your Application class
        FirebaseApp.initializeApp(this)
    }
}