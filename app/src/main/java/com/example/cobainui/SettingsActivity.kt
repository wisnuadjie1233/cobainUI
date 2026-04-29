package com.example.cobainui

import android.content.Intent
import android.os.Bundle
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.firebase.auth.FirebaseAuth

class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        val currentUser = FirebaseAuth.getInstance().currentUser
        val menuProfile = findViewById<LinearLayout>(R.id.menu_profile)
        val btnLogout = findViewById<LinearLayout>(R.id.btn_logout)
        val tvLogout = findViewById<TextView>(R.id.tv_logout_text)

        // 1. PENGECEKAN USER GUEST
        if (currentUser == null || currentUser.isAnonymous) {
            btnLogout.isEnabled = false
            btnLogout.isClickable = false
            btnLogout.alpha = 0.3f

            menuProfile.isEnabled = false
            menuProfile.isClickable = false
            menuProfile.alpha = 0.3f
        } else {
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
                overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right)
            }
        }

        // 2. Tombol Back
        val btnBack = findViewById<ImageButton>(R.id.btn_back_settings)
        btnBack.setOnClickListener {
            finish()
            overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right)
        }

        // 3. LOGIKA TEMA GELAP (DARK MODE)
        val switchTheme = findViewById<MaterialSwitch>(R.id.switch_theme)
        val themePref = getSharedPreferences("ThemePrefs", MODE_PRIVATE)
        val isDarkMode = themePref.getBoolean("isDarkMode", false)

        // Set status switch saat ini
        switchTheme.isChecked = isDarkMode

        switchTheme.setOnCheckedChangeListener { _, isChecked ->
            // Simpan pilihan ke memori
            themePref.edit().putBoolean("isDarkMode", isChecked).apply()

            if (isChecked) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                Toast.makeText(this, "Mode Gelap Aktif", Toast.LENGTH_SHORT).show()
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                Toast.makeText(this, "Mode Terang Aktif", Toast.LENGTH_SHORT).show()
            }
        }

        // 4. LOGIKA PENGINGAT MAKAN
        val sharedPref = getSharedPreferences("SettingsPref", MODE_PRIVATE)
        val switchReminders = findViewById<MaterialSwitch>(R.id.switch_reminders)
        switchReminders.isChecked = sharedPref.getBoolean("isRemindersActive", true)
        switchReminders.setOnCheckedChangeListener { _, isChecked ->
            sharedPref.edit().putBoolean("isRemindersActive", isChecked).apply()
        }

        // 5. LOGIKA LAPORAN HARIAN
        val switchDaily = findViewById<MaterialSwitch>(R.id.switch_daily_report)
        switchDaily.isChecked = sharedPref.getBoolean("isDailyReportActive", false)
        switchDaily.setOnCheckedChangeListener { _, isChecked ->
            sharedPref.edit().putBoolean("isDailyReportActive", isChecked).apply()
        }
    }
}
