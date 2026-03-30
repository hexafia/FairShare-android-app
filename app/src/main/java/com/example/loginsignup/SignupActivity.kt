package com.example.loginsignup

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton

class SignupActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_signup)

        val btnCreate = findViewById<MaterialButton>(R.id.btnCreate)
        val tvLoginLink = findViewById<TextView>(R.id.tvLoginLink)

        // Navigate to Login Screen
        tvLoginLink.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish()
        }

        // Mock Registration: Navigate to Main Dashboard
        btnCreate.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish() // Prevents going back to signup screen
        }
    }
}