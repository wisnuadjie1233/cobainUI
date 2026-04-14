package com.example.cobainui

import android.widget.ProgressBar
import android.view.View
import android.provider.MediaStore
import android.graphics.Bitmap
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import com.mikhaellopez.circularprogressbar.CircularProgressBar
import java.util.Calendar

class HomeActivity : AppCompatActivity() {

    // --- DEKLARASI VARIABEL UTAMA ---
    private lateinit var auth: FirebaseAuth
    private lateinit var greetingText: TextView
    private var backPressedOnce = false

    // Peluncur Izin Kamera
    private val requestCameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) openCamera() else Toast.makeText(this, "Izin kamera ditolak", Toast.LENGTH_SHORT).show()
    }

    // Peluncur Tangkap Hasil Foto
    private val takePictureLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val bitmap = result.data?.extras?.get("data") as? android.graphics.Bitmap
            bitmap?.let { showScanningResultSheet(it) }
        }
    }

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
        val userAvatar = findViewById<ImageView>(R.id.user_avatar)

        userAvatar.setOnClickListener {
            val currentUser = FirebaseAuth.getInstance().currentUser

            // Cek: Jika user NULL atau dia login secara Anonim (Tamu)
            if (currentUser == null || currentUser.isAnonymous) {
                showGuestProfileSheet() // Munculkan popup login yang lama
            } else {
                // JIKA SUDAH LOGIN: Langsung ke Edit Profil
                val intent = Intent(this, EditProfileActivity::class.java)
                startActivity(intent)
                overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right)
            }
        }

        // --- PANGGIL SEMUA FUNGSI SETUP ---
        setupGreeting()
        setupDateRecyclerView()
        setupCaloriesProgressBar()
        setupNutrientsProgressBar()
        setupBackButtonHandler()
        setupCustomNavigation() // <-- Memanggil fungsi navigasi baru
        checkAndResetDailyData() // <-- Memanggil fungsi reset data harian
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

        // 1. Refresh data user agar nama update
        setupGreeting()
        setupCaloriesProgressBar()
        setupNutrientsProgressBar()

        // 2. RESET TOTAL STATUS NAVIGASI (Solusi Masalahmu)
        val navHomeIcon = findViewById<ImageView>(R.id.nav_home_icon)
        val navAiIcon = findViewById<ImageView>(R.id.nav_ai_icon)
        val navScanIcon = findViewById<ImageView>(R.id.nav_scan_icon) // Ikon nomor 3
        val navHistoryIcon = findViewById<ImageView>(R.id.nav_history_icon)
        val navSettingsIcon = findViewById<ImageView>(R.id.nav_settings_icon)

        // Paksa semua false dulu
        navAiIcon.isSelected = false
        navScanIcon.isSelected = false
        navHistoryIcon.isSelected = false
        navSettingsIcon.isSelected = false

        // Paksa Home true
        navHomeIcon.isSelected = true
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

        val color = when {
            consumed <= 2000 -> android.graphics.Color.BLACK // Normal (Hitam)
            consumed <= 2300 -> android.graphics.Color.parseColor("#FBC02D") // Over dikit (Kuning)
            consumed <= 2600 -> android.graphics.Color.parseColor("#F57C00") // Over sedang (Oren)
            else -> android.graphics.Color.parseColor("#D32F2F") // Bahaya (Merah)
        }

        // Set warna bar-nya
        caloriesProgressBar.progressBarColor = color


        // 4. UPDATE TEKS (Teks tidak ikut stuck, tetap angka asli)
        // Jadi kalau sudah scan lagi dan jadi 600, teks tulis 600, tapi bar tetap di posisi 500
        tvCaloriesValue.text = consumed.toInt().toString()
    }

    private fun setupNutrientsProgressBar() {
        // Hubungkan komponen dari XML
        val carbsProgress = findViewById<ProgressBar>(R.id.carbs_progress)
        val tvCarbsValue = findViewById<TextView>(R.id.carbs_value)
        val proteinProgress = findViewById<ProgressBar>(R.id.protein_progress)
        val tvProteinValue = findViewById<TextView>(R.id.protein_value)

        val sharedPref = getSharedPreferences("UserStats", MODE_PRIVATE)

        // Ambil data nutrisi
        val consumedCarbs = sharedPref.getFloat("consumed_carbs", 0f)
        val consumedProtein = sharedPref.getFloat("consumed_protein", 0f)

        // --- LOGIKA KARBOHIDRAT (Target 300g) ---
        val targetCarbs = 300
        carbsProgress.max = targetCarbs
        // Gunakan logika 'stuck' agar tidak meluber
        carbsProgress.progress = if (consumedCarbs > targetCarbs) targetCarbs else consumedCarbs.toInt()
        tvCarbsValue.text = "${consumedCarbs.toInt()}g"

        // --- LOGIKA PROTEIN (Target 100g) ---
        val targetProtein = 100
        proteinProgress.max = targetProtein
        proteinProgress.progress = if (consumedProtein > targetProtein) targetProtein else consumedProtein.toInt()
        tvProteinValue.text = "${consumedProtein.toInt()}g"
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
                // Kita tidak perlu seleksi oranye untuk kamera, langsung buka saja
                requestCameraPermissionLauncher.launch(android.Manifest.permission.CAMERA)
        }

        navScanIcon.setOnClickListener {
            // Hapus pengecekan 'if (!it.isSelected)' khusus untuk navigasi antar halaman
            // Agar setiap kali ditekan, dia PASTI pindah tanpa peduli status sebelumnya.

            val intent = Intent(this, AnalysisHistoryActivity::class.java)
            startActivity(intent)
            overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right)
        }

        navHistoryIcon.setOnClickListener {
            if (!it.isSelected) {
                // 1. Bersihkan seleksi ikon lain
                navIcons.forEach { icon -> icon.isSelected = false }

                // 2. Tandai ikon ini sebagai terpilih (nyala oranye)
                it.isSelected = true

                // 3. PINDAH KE HALAMAN EDUKASI
                val intent = Intent(this, EducationActivity::class.java)
                startActivity(intent)

                // 4. ANIMASI SLIDE
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

    private fun checkAndResetDailyData() {
        val sharedPref = getSharedPreferences("UserStats", MODE_PRIVATE)
        val lastDate = sharedPref.getString("last_opened_date", "")

        val calendar = Calendar.getInstance()
        val currentDate = "${calendar.get(Calendar.YEAR)}-${calendar.get(Calendar.MONTH)}-${calendar.get(Calendar.DAY_OF_MONTH)}"

        if (lastDate != "" && lastDate != currentDate) {
            // --- SEBELUM DIRESET, KITA SIMPAN DATA KEMARIN KE GUDANG ---
            val editor = sharedPref.edit()

            val caloriesKemarin = sharedPref.getFloat("consumed_calories", 0f)
            val carbsKemarin = sharedPref.getFloat("consumed_carbs", 0f)
            val proteinKemarin = sharedPref.getFloat("consumed_protein", 0f)

            // Simpan ke key khusus tanggal (misal: "calories_2026-04-06")
            editor.putFloat("calories_$lastDate", caloriesKemarin)
            editor.putFloat("carbs_$lastDate", carbsKemarin)
            editor.putFloat("protein_$lastDate", proteinKemarin)

            // --- BARU SETELAH ITU RESET HOME JADI 0 ---
            editor.putFloat("consumed_calories", 0f)
            editor.putFloat("consumed_carbs", 0f)
            editor.putFloat("consumed_protein", 0f)
            editor.putFloat("consumed_sugar", 0f) // Reset Gula
            editor.putFloat("consumed_fat", 0f)   // Reset Lemak

            editor.putString("last_opened_date", currentDate)
            editor.putString("daily_food_history", "") // Reset list jadi kosong
            editor.apply()

            setupCaloriesProgressBar()
            setupNutrientsProgressBar()
        } else if (lastDate == "") {
            // Jika baru pertama kali instal
            sharedPref.edit().putString("last_opened_date", currentDate).apply()
        }
    }

    private fun openCamera() {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        takePictureLauncher.launch(intent)
    }

    private fun showScanningResultSheet(bitmap: android.graphics.Bitmap) {
        val dialog = BottomSheetDialog(this)
        val view = layoutInflater.inflate(R.layout.layout_scanning_result, null)
        val ivPreview = view.findViewById<ImageView>(R.id.iv_scan_preview)
        val tvStatus = view.findViewById<TextView>(R.id.tv_scan_status)
        val btnAdd = view.findViewById<MaterialButton>(R.id.btn_add_to_log)

        ivPreview.setImageBitmap(bitmap)

        Handler(Looper.getMainLooper()).postDelayed({
            val cal = 250f
            val addCarbs = 30f
            val addProt = 25f
            val addSugar = 5f
            val addFat = 12f

            // --- 1. UPDATE TEKS PREVIEW (PASTIKAN MUNCUL 4 BARIS) ---
            tvStatus.text = "Terdeteksi: Dada Ayam Bakar\n" +
                    "Estimasi: ${cal.toInt()} kkal\n" +
                    "Carbs: ${addCarbs.toInt()}g | Prot: ${addProt.toInt()}g\n" +
                    "Sugar: ${addSugar.toInt()}g | Fat: ${addFat.toInt()}g"

            btnAdd.visibility = View.VISIBLE

            btnAdd.setOnClickListener {
                val pref = getSharedPreferences("UserStats", MODE_PRIVATE)
                val editor = pref.edit()

                val sdf = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
                val currentTime = sdf.format(java.util.Date())

                // --- 2. UPDATE CATATAN HARIAN AGAR LENGKAP (5 NUTRISI) ---
                // Format: Nama|Jam|Kalori|Carbs|Prot|Sugar|Fat
                val foodEntry = "Dada Ayam Bakar|$currentTime|${cal.toInt()} kkal|${addCarbs.toInt()}g C|${addProt.toInt()}g P"

                val oldHistory = pref.getString("daily_food_history", "")
                val newHistory = if (oldHistory.isNullOrEmpty()) foodEntry else "$oldHistory#$foodEntry"
                editor.putString("daily_food_history", newHistory)

                // --- 3. SIMPAN ANGKA AKUMULASI ---
                editor.putFloat("consumed_calories", pref.getFloat("consumed_calories", 0f) + cal)
                editor.putFloat("consumed_carbs", pref.getFloat("consumed_carbs", 0f) + addCarbs)
                editor.putFloat("consumed_protein", pref.getFloat("consumed_protein", 0f) + addProt)
                editor.putFloat("consumed_sugar", pref.getFloat("consumed_sugar", 0f) + addSugar)
                editor.putFloat("consumed_fat", pref.getFloat("consumed_fat", 0f) + addFat)

                editor.apply()

                setupCaloriesProgressBar()
                setupNutrientsProgressBar()
                dialog.dismiss()
                showCustomToast("Berhasil dicatat jam $currentTime")
            }
        }, 2000)
        dialog.setContentView(view)
        dialog.show()
    }
}
