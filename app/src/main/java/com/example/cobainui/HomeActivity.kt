package com.example.cobainui

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.mikhaellopez.circularprogressbar.CircularProgressBar
import java.util.Calendar

class HomeActivity : AppCompatActivity() {

    // --- DEKLARASI VARIABEL UTAMA ---
    private lateinit var auth: FirebaseAuth
    private lateinit var greetingText: TextView
    private var backPressedOnce = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        // --- INISIALISASI & HUBUNGKAN KOMPONEN ---
        auth = FirebaseAuth.getInstance()
        // Pastikan ID di activity_home.xml Anda sesuai
        greetingText = findViewById(R.id.greeting_text)

        // --- PANGGIL SEMUA FUNGSI SETUP ---
        setupGreeting()
        setupDateRecyclerView()
        setupCaloriesProgressBar()
        setupBackButtonHandler()
        setupCustomNavigation() // <-- Memanggil fungsi navigasi baru
    }

    private fun setupGreeting() {
        val currentUser = auth.currentUser
        val calendar = Calendar.getInstance()
        val greeting = when (calendar.get(Calendar.HOUR_OF_DAY)) {
            in 0..11 -> "Selamat Pagi,"
            in 12..15 -> "Selamat Siang,"
            in 16..18 -> "Selamat Sore,"
            else -> "Selamat Malam,"
        }

        if (currentUser != null) {
            // Logika untuk pengguna yang sudah login
            val userName = currentUser.displayName?.ifEmpty { null } ?: currentUser.email?.split('@')?.first() ?: "Pengguna"
            val formattedName = userName.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
            greetingText.text = "$greeting $formattedName"
        } else {
            // Logika untuk pengguna tamu (mode lewati)
            greetingText.text = "$greeting Tamu"
        }
    }

    private fun setupDateRecyclerView() {
        val dateRecyclerView = findViewById<RecyclerView>(R.id.date_recycler_view)
        dateRecyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        val dates = getWeekDates()
        val adapter = DateAdapter(dates)
        dateRecyclerView.adapter = adapter
        val today = Calendar.getInstance().get(Calendar.DAY_OF_YEAR)
        val todayPosition = dates.indexOfFirst { it.date.get(Calendar.DAY_OF_YEAR) == today }
        if (todayPosition != -1) {
            dates[todayPosition].isSelected = true
            adapter.notifyItemChanged(todayPosition)
            dateRecyclerView.scrollToPosition(todayPosition)
        }
    }

    private fun getWeekDates(): List<DateItem> {
        val dates = mutableListOf<DateItem>()
        val calendar = Calendar.getInstance()
        calendar.firstDayOfWeek = Calendar.MONDAY
        calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        for (i in 0..6) {
            dates.add(DateItem(calendar.clone() as Calendar))
            calendar.add(Calendar.DAY_OF_MONTH, 1)
        }
        return dates
    }

    private fun setupCaloriesProgressBar() {
        val caloriesProgressBar = findViewById<CircularProgressBar>(R.id.calories_progress_bar)
        caloriesProgressBar.progress = 65f
    }

    private fun setupBackButtonHandler() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (backPressedOnce) {
                    finishAffinity()
                    return
                }
                backPressedOnce = true
                showCustomToast("Tekan lagi untuk keluar")
                Handler(Looper.getMainLooper()).postDelayed({ backPressedOnce = false }, 2000)
            }
        })
    }

    private fun showCustomToast(message: String) {
        val inflater = LayoutInflater.from(this)
        val layout = inflater.inflate(R.layout.custom_toast_layout, null)
        val textView: TextView = layout.findViewById(R.id.toast_text)
        textView.text = message
        with(Toast(applicationContext)) {
            duration = Toast.LENGTH_SHORT
            view = layout
            show()
        }
    }

    private fun setupCustomNavigation() {
        // 1. Hubungkan semua ImageView ikon dari layout ke variabel
        val navHomeIcon = findViewById<ImageView>(R.id.nav_home_icon)
        val navAiIcon = findViewById<ImageView>(R.id.nav_ai_icon)
        val navScanIcon = findViewById<ImageView>(R.id.nav_scan_icon)
        val navHistoryIcon = findViewById<ImageView>(R.id.nav_history_icon)
        val navSettingsIcon = findViewById<ImageView>(R.id.nav_settings_icon)

        // Jadikan semua ikon dalam sebuah list agar mudah dikelola
        val navIcons = listOf(navHomeIcon, navAiIcon, navScanIcon, navHistoryIcon, navSettingsIcon)

        // Fungsi bantuan untuk menonaktifkan semua ikon
        fun clearSelection() {
            navIcons.forEach { it.isSelected = false }
        }

        // 2. Set item Home sebagai yang aktif saat pertama kali halaman dibuka
        navHomeIcon.isSelected = true

        // 3. Tambahkan OnClickListener untuk setiap ikon
        navHomeIcon.setOnClickListener {
            if (!it.isSelected) { // Hanya jalankan jika belum dipilih
                clearSelection()
                it.isSelected = true
                // TODO: Tampilkan Fragment atau konten untuk Home
            }
        }

        navAiIcon.setOnClickListener {
            if (!it.isSelected) {
                clearSelection()
                it.isSelected = true
                // TODO: Tampilkan Fragment atau konten untuk AI
            }
        }

        navScanIcon.setOnClickListener {
            if (!it.isSelected) {
                clearSelection()
                it.isSelected = true
                // TODO: Tampilkan Fragment atau konten untuk Scan
            }
        }

        navHistoryIcon.setOnClickListener {
            if (!it.isSelected) {
                clearSelection()
                it.isSelected = true
                // TODO: Tampilkan Fragment atau konten untuk History
            }
        }

        navSettingsIcon.setOnClickListener {
            if (!it.isSelected) {
                clearSelection()
                it.isSelected = true
                // TODO: Tampilkan Fragment atau konten untuk Settings
            }
        }
    }
}
