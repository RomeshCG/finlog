package com.example.finlog

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        // Delay for 3 seconds, then redirect to NavigationActivity
        Handler(Looper.getMainLooper()).postDelayed({
            val intent = Intent(this, NavigationActivity::class.java)
            startActivity(intent)
            finish() // Close MainActivity so the user can't go back to it
        }, 3000) // 3000 milliseconds = 3 seconds
    }
}