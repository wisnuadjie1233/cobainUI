package com.example.cobainui

import android.widget.ProgressBar
import android.view.View
import android.provider.MediaStore
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.content.Intent
import android.net.Uri
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
    private lateinit var foodClassifier: FoodClassifier

    // Peluncur Izin Kamera
    private val requestCameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) openCamera() else Toast.makeText(this, "Izin kamera ditolak", Toast.LENGTH_SHORT).show()
    }

    // Peluncur Tangkap Hasil Foto Kamera
    private val takePictureLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val bitmap = result.data?.extras?.get("data") as? Bitmap
            bitmap?.let { showScanningResultSheet(it) }
        }
    }

    // Peluncur Pilih Gambar dari Galeri
    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            try {
                val inputStream = contentResolver.openInputStream(it)
                val bitmap = BitmapFactory.decodeStream(inputStream)
                bitmap?.let { b -> showScanningResultSheet(b) }
            } catch (e: Exception) {
                Toast.makeText(this, "Gagal memuat gambar", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        val notificationIcon = findViewById<ImageView>(R.id.notification_icon)

        notificationIcon.setOnClickListener {
            val intent = Intent(this, NotificationActivity::class.java)
            startActivity(intent)
        }

        auth = FirebaseAuth.getInstance()
        greetingText = findViewById(R.id.greeting_text)
        foodClassifier = FoodClassifier(this)

        val userAvatar = findViewById<ImageView>(R.id.user_avatar)

        userAvatar.setOnClickListener {
            val currentUser = FirebaseAuth.getInstance().currentUser
            if (currentUser == null || currentUser.isAnonymous) {
                showGuestProfileSheet()
            } else {
                val intent = Intent(this, EditProfileActivity::class.java)
                startActivity(intent)
                overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right)
            }
        }

        setupGreeting()
        setupDateRecyclerView()
        setupCaloriesProgressBar()
        setupNutrientsProgressBar()
        setupBackButtonHandler()
        setupCustomNavigation()
        checkAndResetDailyData()
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
        setupGreeting()
        setupCaloriesProgressBar()
        setupNutrientsProgressBar()

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
            val userName = currentUser.displayName ?: "User"
            greetingText.text = "$greeting $userName"
        } else {
            greetingText.text = "$greeting Tamu"
        }
    }

    private fun setupDateRecyclerView() {
        val dateRecyclerView = findViewById<RecyclerView>(R.id.date_recycler_view)
        dateRecyclerView.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)

        val dates = getWeekDates()
        val adapter = DateAdapter(dates)
        dateRecyclerView.adapter = adapter

        val todayCalendar = Calendar.getInstance()
        val todayPosition = dates.indexOfFirst {
            it.date.get(Calendar.YEAR) == todayCalendar.get(Calendar.YEAR) &&
                    it.date.get(Calendar.DAY_OF_YEAR) == todayCalendar.get(Calendar.DAY_OF_YEAR)
        }

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
        val tvCaloriesValue = findViewById<TextView>(R.id.calories_text)
        val tvMaxLabel = findViewById<TextView>(R.id.tv_max_calories)

        val sharedPref = getSharedPreferences("UserStats", MODE_PRIVATE)
        val consumed = sharedPref.getFloat("consumed_calories", 0f)
        val targetUser = sharedPref.getFloat("daily_target_calories", 2000f)
        tvMaxLabel.text = targetUser.toInt().toString()
        val batasMacet = targetUser
        val targetFull = (targetUser + (targetUser * 0.3f)).toFloat()

        caloriesProgressBar.progressMax = targetFull
        if (consumed >= batasMacet) {
            caloriesProgressBar.progress = batasMacet
        } else {
            caloriesProgressBar.progress = consumed
        }

        val color = when {
            consumed <= targetUser -> android.graphics.Color.BLACK
            consumed <= targetUser + 300 -> android.graphics.Color.parseColor("#FBC02D")
            consumed <= targetUser + 600 -> android.graphics.Color.parseColor("#F57C00")
            else -> android.graphics.Color.parseColor("#D32F2F")
        }
        caloriesProgressBar.progressBarColor = color
        tvCaloriesValue.text = consumed.toInt().toString()
    }

    private fun setupNutrientsProgressBar() {
        val carbsProgress = findViewById<ProgressBar>(R.id.carbs_progress)
        val tvCarbsValue = findViewById<TextView>(R.id.carbs_value)
        val proteinProgress = findViewById<ProgressBar>(R.id.protein_progress)
        val tvProteinValue = findViewById<TextView>(R.id.protein_value)

        val sharedPref = getSharedPreferences("UserStats", MODE_PRIVATE)
        val consumedCarbs = sharedPref.getFloat("consumed_carbs", 0f)
        val consumedProtein = sharedPref.getFloat("consumed_protein", 0f)

        val targetCarbs = 300
        carbsProgress.max = targetCarbs
        carbsProgress.progress = if (consumedCarbs > targetCarbs) targetCarbs else consumedCarbs.toInt()
        tvCarbsValue.text = "${consumedCarbs.toInt()}g"

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
        val navHomeIcon = findViewById<ImageView>(R.id.nav_home_icon)
        val navAiIcon = findViewById<ImageView>(R.id.nav_ai_icon)
        val navScanIcon = findViewById<ImageView>(R.id.nav_scan_icon)
        val navHistoryIcon = findViewById<ImageView>(R.id.nav_history_icon)
        val navSettingsIcon = findViewById<ImageView>(R.id.nav_settings_icon)

        val navIcons = listOf(navHomeIcon, navAiIcon, navScanIcon, navHistoryIcon, navSettingsIcon)

        fun clearSelection() {
            navIcons.forEach { it.isSelected = false }
        }

        navHomeIcon.isSelected = true

        navHomeIcon.setOnClickListener {
            if (!it.isSelected) {
                clearSelection()
                it.isSelected = true
            }
        }

        navAiIcon.setOnClickListener {
            showImageSourceOptions()
        }

        navScanIcon.setOnClickListener {
            val intent = Intent(this, AnalysisHistoryActivity::class.java)
            startActivity(intent)
            overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right)
        }

        navHistoryIcon.setOnClickListener {
            if (!it.isSelected) {
                navIcons.forEach { icon -> icon.isSelected = false }
                it.isSelected = true
                val intent = Intent(this, EducationActivity::class.java)
                startActivity(intent)
                overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right)
            }
        }

        navSettingsIcon.setOnClickListener {
            if (!it.isSelected) {
                clearSelection()
                it.isSelected = true
                val intent = Intent(this, SettingsActivity::class.java)
                startActivity(intent)
                overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right)
            }
        }
    }

    private fun showImageSourceOptions() {
        val dialog = BottomSheetDialog(this)
        val view = layoutInflater.inflate(R.layout.layout_image_source_selection, null)

        val btnCamera = view.findViewById<View>(R.id.option_camera)
        val btnGallery = view.findViewById<View>(R.id.option_gallery)

        btnCamera?.setOnClickListener {
            dialog.dismiss()
            requestCameraPermissionLauncher.launch(android.Manifest.permission.CAMERA)
        }

        btnGallery?.setOnClickListener {
            dialog.dismiss()
            openGallery()
        }

        dialog.setContentView(view)
        dialog.show()
    }

    private fun openGallery() {
        pickImageLauncher.launch("image/*")
    }

    private fun checkAndResetDailyData() {
        val sharedPref = getSharedPreferences("UserStats", MODE_PRIVATE)
        val calendar = Calendar.getInstance()
        val currentWeek = calendar.get(Calendar.WEEK_OF_YEAR)
        val currentYear = calendar.get(Calendar.YEAR)
        val currentDateStr = "${currentYear}-${calendar.get(Calendar.MONTH)}-${calendar.get(Calendar.DAY_OF_MONTH)}"

        val lastWeek = sharedPref.getInt("last_opened_week", -1)
        val lastYear = sharedPref.getInt("last_opened_year", -1)
        val lastDateStr = sharedPref.getString("last_opened_date", "")

        val editor = sharedPref.edit()

        if (currentWeek != lastWeek || currentYear != lastYear) {
            val days = arrayOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
            for (day in days) {
                editor.putFloat("history_cal_$day", 0f)
            }
            editor.putInt("last_opened_week", currentWeek)
            editor.putInt("last_opened_year", currentYear)
            editor.putString("daily_food_history", "")
        }

        if (lastDateStr != "" && lastDateStr != currentDateStr) {
            val yesterdayCal = Calendar.getInstance()
            yesterdayCal.add(Calendar.DATE, -1)
            val dayName = java.text.SimpleDateFormat("EEE", java.util.Locale.US).format(yesterdayCal.time)
            val consumedYesterday = sharedPref.getFloat("consumed_calories", 0f)
            editor.putFloat("history_cal_$dayName", consumedYesterday)

            editor.putFloat("consumed_calories", 0f)
            editor.putFloat("consumed_carbs", 0f)
            editor.putFloat("consumed_protein", 0f)
            editor.putFloat("consumed_sugar", 0f)
            editor.putFloat("consumed_fat", 0f)
            editor.putString("daily_food_history", "")
            editor.putString("last_opened_date", currentDateStr)
        } else if (lastDateStr == "") {
            editor.putString("last_opened_date", currentDateStr)
        }
        editor.apply()
        setupCaloriesProgressBar()
        setupNutrientsProgressBar()
    }

    private fun openCamera() {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        takePictureLauncher.launch(intent)
    }

    private fun showScanningResultSheet(bitmap: Bitmap) {
        val dialog = BottomSheetDialog(this)
        val view = layoutInflater.inflate(R.layout.layout_scanning_result, null)
        val ivPreview = view.findViewById<ImageView>(R.id.iv_scan_preview)
        val tvStatus = view.findViewById<TextView>(R.id.tv_scan_status)
        val btnAdd = view.findViewById<MaterialButton>(R.id.btn_add_to_log)

        ivPreview.setImageBitmap(bitmap)

        Handler(Looper.getMainLooper()).postDelayed({
            // 1. Klasifikasikan gambar menggunakan AI
            val detectedFood = foodClassifier.classify(bitmap)

            val cal = 200f
            val addCarbs = 30f
            val addProt = 25f
            val addSugar = 5f
            val addFat = 12f

            // 2. Tampilkan Nama Makanan hasil AI
            tvStatus.text = "Terdeteksi: $detectedFood\n" +
                    "Estimasi: ${cal.toInt()} kkal\n" +
                    "Carbs: ${addCarbs.toInt()}g | Prot: ${addProt.toInt()}g\n" +
                    "Sugar: ${addSugar.toInt()}g | Fat: ${addFat.toInt()}g"

            btnAdd.visibility = View.VISIBLE

            btnAdd.setOnClickListener {
                val pref = getSharedPreferences("UserStats", MODE_PRIVATE)
                val editor = pref.edit()
                val sdf = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
                val currentTime = sdf.format(java.util.Date())
                val foodEntry = "$detectedFood|$currentTime|${cal.toInt()} kkal|${addCarbs.toInt()}g C|${addProt.toInt()}g P"
                val oldHistory = pref.getString("daily_food_history", "")
                val newHistory = if (oldHistory.isNullOrEmpty()) foodEntry else "$oldHistory#$foodEntry"
                editor.putString("daily_food_history", newHistory)
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
