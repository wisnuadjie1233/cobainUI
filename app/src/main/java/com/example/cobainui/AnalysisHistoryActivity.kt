package com.example.cobainui

import android.os.Bundle
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity

class AnalysisHistoryActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Hubungkan dengan file XML yang baru kita buat
        setContentView(R.layout.activity_analysis_history)

        // Tombol Back untuk kembali ke Home
        val btnBack = findViewById<ImageButton>(R.id.btn_back_history)
        btnBack.setOnClickListener {
            finish()
            overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right)
        }
    }
}