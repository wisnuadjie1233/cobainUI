package com.example.cobainui

import android.content.Intent
import android.os.Bundle
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity

class OnboardingActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_onboarding)

        val nextButton = findViewById<FrameLayout>(R.id.next_button)
        nextButton.setOnClickListener {
            val intent = Intent(this, Page3Activity::class.java)
            startActivity(intent)
        }
    }
}
