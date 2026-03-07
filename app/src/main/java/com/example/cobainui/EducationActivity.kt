package com.example.cobainui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomsheet.BottomSheetDialog

class EducationActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_education)

        // 1. Tombol Back
        val btnBack = findViewById<ImageButton>(R.id.btn_back_education)
        btnBack.setOnClickListener {
            finish()
            overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right)
        }

        // 2. Tombol Baca 2 Menit (Featured)
        val btnRead = findViewById<Button>(R.id.btn_read_featured)
        btnRead.setOnClickListener {
            val dialog = BottomSheetDialog(this)
            val view = layoutInflater.inflate(R.layout.layout_article_detail, null)

            view.findViewById<Button>(R.id.btn_close_article).setOnClickListener {
                dialog.dismiss()
            }
            dialog.setContentView(view)
            dialog.show()
        }

        // 3. Item List: Defisit Kalori
        // PASTIKAN ID 'item_defisit_click' SUDAH ADA DI activity_education.xml
        val cardDefisit = findViewById<View>(R.id.item_defisit_click)
        cardDefisit.setOnClickListener {
            val dialog = BottomSheetDialog(this)
            val view = layoutInflater.inflate(R.layout.layout_article_defisit, null)

            view.findViewById<Button>(R.id.btn_close_defisit).setOnClickListener {
                dialog.dismiss()
            }
            dialog.setContentView(view)
            dialog.show()
        }

        // 1. Hubungkan variabel dengan ID di XML
        val cardKarbo = findViewById<View>(R.id.item_karbo_click)

// 2. Beri perintah klik
        cardKarbo.setOnClickListener {
            val dialog = BottomSheetDialog(this)
            // Pastikan inflate layout_article_karbo
            val view = layoutInflater.inflate(R.layout.layout_article_karbo, null)

            // Tombol tutup di dalam popup karbo
            view.findViewById<Button>(R.id.btn_close_karbo).setOnClickListener {
                dialog.dismiss()
            }

            dialog.setContentView(view)
            dialog.show()
        }

    }
}