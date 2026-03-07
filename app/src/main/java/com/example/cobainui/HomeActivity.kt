package com.example.cobainui

import androidx.activity.result.contract.ActivityResultContracts
import android.Manifest
import android.provider.MediaStore
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import com.mikhaellopez.circularprogressbar.CircularProgressBar
import java.util.Calendar

class HomeActivity : AppCompatActivity() {

    private val requestCameraPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            openCamera()
        } else {
            showCustomToast("Izin kamera ditolak. Fitur scan tidak bisa digunakan.")
        }
    }

    // 1. Peluncur untuk menangkap hasil foto
    private val takePictureLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            // Foto berhasil diambil!
            // Kita ambil datanya dalam bentuk Bitmap (gambar kecil/thumbnail)
            val imageBitmap = result.data?.extras?.get("data") as? android.graphics.Bitmap

            if (imageBitmap != null) {
                // LANJUT: Munculkan hasil analisis
                showScanningResultSheet(imageBitmap)
            }
        }
    }

    // 2. Ubah fungsi openCamera agar menggunakan peluncur di atas
    private fun openCamera() {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        takePictureLauncher.launch(intent) // Ganti dari startActivity(intent)
    }

    // --- DEKLARASI VARIABEL UTAMA ---
    private lateinit var auth: FirebaseAuth
    private lateinit var greetingText: TextView
    private var backPressedOnce = false

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        // Pastikan ID 'notification_icon' sesuai dengan yang ada di activity_home.xml kamu
        val notificationIcon = findViewById<ImageView>(R.id.notification_icon)

        notificationIcon.setOnClickListener {
            // Intent untuk pindah halaman dari Home ke Notification
            val intent = Intent(this, NotificationActivity::class.java)
            startActivity(intent)
        }

        // --- INISIALISASI & HUBUNGKAN KOMPONEN ---
        auth = FirebaseAuth.getInstance()
        // Pastikan ID di activity_home.xml Anda sesuai
        greetingText = findViewById(R.id.greeting_text)

        // Logika Klik Avatar untuk Tamu
        // Logika Klik Avatar (Ganti baris 53-73 dengan ini)
        val userAvatar = findViewById<ImageView>(R.id.user_avatar)

        userAvatar.setOnClickListener {
            val currentUser = FirebaseAuth.getInstance().currentUser

            // 1. Cek apakah dia benar-benar Tamu
            if (currentUser == null || currentUser.isAnonymous) {

                // JIKA TAMU: Panggil fungsi yang sudah lengkap dengan logika tombolnya
                showGuestProfileSheet()

            } else {
                // JIKA LOGIN: Langsung bawa ke halaman Edit Profil
                val intent = Intent(this, EditProfileActivity::class.java)
                startActivity(intent)

                // Beri animasi transisi agar keren
                overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right)
            }
        }

        // --- PANGGIL SEMUA FUNGSI SETUP ---
        setupGreeting()
        setupDateRecyclerView()
        setupCaloriesProgressBar()
        setupBackButtonHandler()
        setupCustomNavigation() // <-- Memanggil fungsi navigasi baru
    }

    private fun showGuestProfileSheet() {
        val dialog = BottomSheetDialog(this)
        val view = layoutInflater.inflate(R.layout.layout_guest_profile_sheet, null)

        val btnLoginNow = view.findViewById<MaterialButton>(R.id.btn_login_now)
        val btnMaybeLater = view.findViewById<TextView>(R.id.btn_maybe_later)

        btnLoginNow.setOnClickListener {
            dialog.dismiss()
            val intent = Intent(this, Page4Activity::class.java)
            startActivity(intent)
        }

        btnMaybeLater.setOnClickListener {
            dialog.dismiss()
        }

        dialog.setContentView(view)
        dialog.show()
    }

    override fun onResume() {
        super.onResume()
        resetNavigationToHome()

        val navHomeIcon = findViewById<ImageView>(R.id.nav_home_icon)
        val navHistoryIcon = findViewById<ImageView>(R.id.nav_history_icon)

        navHistoryIcon.isSelected = false
        navHomeIcon.isSelected = true
        setupGreeting()
    }

    private fun resetNavigationToHome() {
        val navHomeIcon = findViewById<ImageView>(R.id.nav_home_icon)
        val navAiIcon = findViewById<ImageView>(R.id.nav_ai_icon)
        val navScanIcon = findViewById<ImageView>(R.id.nav_scan_icon)
        val navHistoryIcon = findViewById<ImageView>(R.id.nav_history_icon)
        val navSettingsIcon = findViewById<ImageView>(R.id.nav_settings_icon)


        navAiIcon.isSelected = false
        navScanIcon.isSelected = false
        navHistoryIcon.isSelected = false
        navSettingsIcon.isSelected = false

        navHomeIcon.isSelected = true
    }

    private fun setupGreeting() {

        auth.currentUser?.reload()?.addOnCompleteListener {
            val currentUser = auth.currentUser
            val calendar = Calendar.getInstance()
            val greeting = when (calendar.get(Calendar.HOUR_OF_DAY)) {
                in 0..11 -> "Selamat Pagi"
                in 12..15 -> "Selamat Siang"
                in 16..18 -> "Selamat Sore"
                else -> "Selamat Malam"
            }

            if (currentUser != null) {
                // Jika user LOGIN
                val userName = currentUser.displayName ?: "User"
                greetingText.text = "$greeting $userName"
            } else {
                // Jika user TEKAN LEWATI (Tamu)
                greetingText.text = "$greeting Tamu" // <--- Pastikan cuma ini
            }
        }
    }

    // Di dalam HomeActivity.kt
    private fun setupDateRecyclerView() {
        val dateRecyclerView = findViewById<RecyclerView>(R.id.date_recycler_view)
        dateRecyclerView.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)

        val dates = getWeekDates()
        val adapter = DateAdapter(dates)
        dateRecyclerView.adapter = adapter

        // Otomatis tandai dan scroll ke tanggal hari ini
        val todayCalendar = Calendar.getInstance()
        val todayPosition = dates.indexOfFirst {
            it.date.get(Calendar.YEAR) == todayCalendar.get(Calendar.YEAR) &&
                    it.date.get(Calendar.DAY_OF_YEAR) == todayCalendar.get(Calendar.DAY_OF_YEAR)
        }

        if (todayPosition != -1) {
            dates[todayPosition].isSelected = true
            adapter.notifyItemChanged(todayPosition)
            // Scroll ke posisi hari ini agar terlihat di layar
            dateRecyclerView.scrollToPosition(todayPosition)
        }
    }


    // Di dalam HomeActivity.kt+
    private fun getWeekDates(): List<DateItem> {
        val dates = mutableListOf<DateItem>()
        val calendar = Calendar.getInstance() // Mulai dari tanggal hari ini

        // Atur kalender ke hari pertama minggu ini (misalnya, Senin)
        calendar.firstDayOfWeek = Calendar.MONDAY
        calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)

        // Buat daftar 7 hari dari Senin hingga Minggu
        for (i in 0..6) {
            dates.add(DateItem(calendar.clone() as Calendar))
            calendar.add(Calendar.DAY_OF_MONTH, 1)
        }
        return dates
    }

    private fun setupCaloriesProgressBar() {
        val caloriesProgressBar = findViewById<CircularProgressBar>(R.id.calories_progress_bar)
        val tvCaloriesValue = findViewById<TextView>(R.id.calories_text)

        val sharedPref = getSharedPreferences("UserStats", MODE_PRIVATE)
        val consumed = sharedPref.getFloat("consumed_calories", 0f)

        // 1. Tentukan batas maksimal visual (Batas Macet)
        val batasMacet = 2000f
        val targetFull = 4000f // Kapasitas wadah grafik

        // 2. Atur wadah grafik
        caloriesProgressBar.progressMax = targetFull

        // 3. LOGIKA BIAR STUCK:
        // Jika kalori yang dimakan sudah mencapai atau melewati 500
        if (consumed >= batasMacet) {
            // PAKSA bar berhenti di angka 500 (diem/stuck)
            caloriesProgressBar.progress = batasMacet
        } else {
            // Jika masih di bawah 500, gerakkan bar secara normal
            caloriesProgressBar.progress = consumed
        }

        // 4. UPDATE TEKS (Teks tidak ikut stuck, tetap angka asli)
        // Jadi kalau sudah scan lagi dan jadi 600, teks tulis 600, tapi bar tetap di posisi 500
        tvCaloriesValue.text = consumed.toInt().toString()
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
            if (!it.isSelected) { // Hw anya jalankan jika belum dipilih-
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
                requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }

        navScanIcon.setOnClickListener {
            if (!it.isSelected) {
                clearSelection()
                it.isSelected = true
                // TODO: Tampilkan Fragment atau konten untuk Scan
                val intent = Intent(this, AnalysisHistoryActivity::class.java)
                startActivity(intent)

                // Beri animasi geser samping agar konsisten dengan Settings
                overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right)
            }
        }

        navHistoryIcon.setOnClickListener {
            if (!it.isSelected) {
                clearSelection()
                it.isSelected = true
                // TODO: Tampilkan Fragment atau konten untuk History
                val intent = Intent(this, EducationActivity::class.java)
                startActivity(intent)

                // Beri animasi swipe biar konsisten dengan menu lainnya
                overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right)
            }
        }

        navSettingsIcon.setOnClickListener {
            if (!it.isSelected) {
                clearSelection()
                it.isSelected = true
                // PINDAH HALAMAN KE SETTINGS
                val intent = Intent(this, SettingsActivity::class.java)
                startActivity(intent)
                // ANIMASI SLIDE
                overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right)
            }
        }

    }

    private fun showScanningResultSheet(bitmap: android.graphics.Bitmap) {
        val dialog = BottomSheetDialog(this)
        val view = layoutInflater.inflate(R.layout.layout_scanning_result, null)

        val ivPreview = view.findViewById<ImageView>(R.id.iv_scan_preview)
        val tvStatus = view.findViewById<TextView>(R.id.tv_scan_status)
        val btnAdd = view.findViewById<MaterialButton>(R.id.btn_add_to_log)

        // Tampilkan foto yang baru diambil ke popup
        ivPreview.setImageBitmap(bitmap)

        // Simulasi Loading AI (Biar terlihat pintar)
        // Di dalam Handler showScanningResultSheet...
        Handler(Looper.getMainLooper()).postDelayed({
            val detectedCalories = 250f // Angka simulasi
            tvStatus.text = "Terdeteksi: Dada Ayam Bakar\nEstimasi: ${detectedCalories.toInt()} kkal"
            btnAdd.visibility = View.VISIBLE

            // LOGIKA KLIK TOMBOL TAMBAHKAN
            btnAdd.setOnClickListener {
                // 1. Ambil data lama
                val sharedPref = getSharedPreferences("UserStats", MODE_PRIVATE)
                val currentConsumed = sharedPref.getFloat("consumed_calories", 0f)

                // 2. Tambahkan dengan kalori baru
                val newTotal = currentConsumed + detectedCalories

                // 3. Simpan kembali
                sharedPref.edit().putFloat("consumed_calories", newTotal).apply()

                // 4. Update tampilan lingkaran di Home secara langsung
                setupCaloriesProgressBar()

                // 5. Tutup popup dan beri pesan
                dialog.dismiss()
                showCustomToast("Berhasil ditambahkan!")
            }
        }, 2000)
        dialog.setContentView(view)
        dialog.show()
    }
}
