package com.example.cobainui

import android.os.Bundle
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity

class NotificationActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_notification)

        // 1. Hubungkan ID btn_back dari XML ke variabel Kotlin
        val btnBack = findViewById<ImageButton>(R.id.btn_back)

        // 2. Tambahkan aksi klik untuk kembali
        btnBack.setOnClickListener {
            // finish() akan menutup activity ini dan otomatis kembali ke activity sebelumnya (Home)
            finish()

            // Opsional: Tambahkan animasi transisi keluar (biar smooth)
            overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right)
        }
    }
}