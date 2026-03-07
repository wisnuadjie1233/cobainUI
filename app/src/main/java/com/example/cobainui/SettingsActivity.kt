package com.example.cobainui

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.ImageButton
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.firebase.auth.FirebaseAuth
import android.widget.TextView

class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        val currentUser = FirebaseAuth.getInstance().currentUser
        val menuProfile = findViewById<LinearLayout>(R.id.menu_profile)
        val btnLogout = findViewById<LinearLayout>(R.id.btn_logout)
        val tvLogout = findViewById<TextView>(R.id.tv_logout_text)

        // GUNAKAN PENGECEKAN INI
        if (currentUser == null || currentUser.isAnonymous) {
            // JIKA TAMU: Matikan total
            btnLogout.isEnabled = false
            btnLogout.isClickable = false // TAMBAHKAN INI
            btnLogout.alpha = 0.3f

            menuProfile.isEnabled = false
            menuProfile.isClickable = false // TAMBAHKAN INI
            menuProfile.alpha = 0.3f

        } else {
            // JIKA LOGIN: Beri fungsi klik
            btnLogout.setOnClickListener {
                FirebaseAuth.getInstance().signOut()
                val intent = Intent(this, Page4Activity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            }

            menuProfile.setOnClickListener {
                val intent = Intent(this, EditProfileActivity::class.java)
                startActivity(intent)
                // Animasi geser ke kiri (masuk)
                overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right)
            }
        }

        // 1. Logika Tombol Back
        val btnBack = findViewById<ImageButton>(R.id.btn_back_settings)
        btnBack.setOnClickListener {
            finish()
            overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right)
        }


        // ============================================================
        // === BAGIAN PENYIMPANAN DATA (SHAREDPREFERENCES) ===
        // ============================================================

        // Membuka file catatan "SettingsPref"
        val sharedPref = getSharedPreferences("SettingsPref", MODE_PRIVATE)

        // --- A. LOGIKA TEMA GELAP ---
        val switchTheme = findViewById<MaterialSwitch>(R.id.switch_theme)
        switchTheme.isChecked = sharedPref.getBoolean("isDarkMode", false)
        switchTheme.setOnCheckedChangeListener { _, isChecked ->
            sharedPref.edit().putBoolean("isDarkMode", isChecked).apply()

            Handler(Looper.getMainLooper()).postDelayed({
                if (isChecked) {
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                } else {
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                }
            }, 250)
        }

        // --- B. LOGIKA PENGINGAT MAKAN ---
        val switchReminders = findViewById<MaterialSwitch>(R.id.switch_reminders)
        // Ambil data lama, default true
        switchReminders.isChecked = sharedPref.getBoolean("isRemindersActive", true)
        switchReminders.setOnCheckedChangeListener { _, isChecked ->
            // Simpan perubahan setiap kali diklik
            sharedPref.edit().putBoolean("isRemindersActive", isChecked).apply()
        }

        // --- C. LOGIKA LAPORAN HARIAN ---
        val switchDaily = findViewById<MaterialSwitch>(R.id.switch_daily_report)
        // Ambil data lama, default false
        switchDaily.isChecked = sharedPref.getBoolean("isDailyReportActive", false)
        switchDaily.setOnCheckedChangeListener { _, isChecked ->
            // Simpan perubahan setiap kali diklik
            sharedPref.edit().putBoolean("isDailyReportActive", isChecked).apply()
        }

    }
}