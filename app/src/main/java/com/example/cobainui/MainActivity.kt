package com.example.cobainui
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val titleTextView = findViewById<TextView>(R.id.nutriscan_title)
        val fullText = "NutriScan"
        val spannable = SpannableString(fullText)
        spannable.setSpan(
            ForegroundColorSpan(Color.parseColor("#FF5A16")),
            5, // start index of "Scan"
            fullText.length, // end index
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        titleTextView.text = spannable

        // Handler to post a delayed action
        Handler(Looper.getMainLooper()).postDelayed({
            // Create an Intent to start OnboardingActivity
            val intent = Intent(this, OnboardingActivity::class.java)
            startActivity(intent)
            // Finish MainActivity so the user can't go back to it
            finish()
        }, 3000) // 3000 milliseconds = 3 seconds
    }
}
