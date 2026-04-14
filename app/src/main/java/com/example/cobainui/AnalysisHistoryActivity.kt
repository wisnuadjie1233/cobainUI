package com.example.cobainui

import android.os.Bundle
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.util.*

class AnalysisHistoryActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_analysis_history)

        // 1. Tombol Kembali
        val btnBack = findViewById<ImageButton>(R.id.btn_back_history)
        btnBack?.setOnClickListener {
            finish()
            overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right)
        }

        // 2. Ambil Data dari SharedPreferences (Cukup deklarasi 1x di sini)
        val sharedPref = getSharedPreferences("UserStats", MODE_PRIVATE)
        val consumedCal = sharedPref.getFloat("consumed_calories", 0f)
        val consumedCarbs = sharedPref.getFloat("consumed_carbs", 0f)
        val consumedProtein = sharedPref.getFloat("consumed_protein", 0f)

        // 3. Logika Rata-rata Mingguan (Senin + Selasa)
        val seninCal = 1200f
        val selasaCal = consumedCal
        val avgCal = (seninCal + selasaCal) / 2

        val tvAverageTotal = findViewById<TextView>(R.id.tv_average_total)
        tvAverageTotal?.text = String.format(Locale.US, "%,.0f kkal", avgCal)

        // 4. Logika Deteksi Hari Otomatis
        val calendar = Calendar.getInstance()
        val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)

        // --- SETUP GRAFIK MINGGUAN ---
        setupBar(R.id.bar_senin, R.id.tv_val_senin, seninCal, dayOfWeek >= Calendar.MONDAY)

        val selasaVal = if (dayOfWeek == Calendar.TUESDAY) consumedCal else if (dayOfWeek > Calendar.TUESDAY) 1500f else 0f
        setupBar(R.id.bar_selasa, R.id.tv_val_selasa, selasaVal, dayOfWeek >= Calendar.TUESDAY)

        setupBar(R.id.bar_rabu, R.id.tv_val_rabu, 0f, dayOfWeek >= Calendar.WEDNESDAY)
        setupBar(R.id.bar_kamis, R.id.tv_val_kamis, 0f, dayOfWeek >= Calendar.THURSDAY)
        setupBar(R.id.bar_jumat, R.id.tv_val_jumat, 0f, dayOfWeek >= Calendar.FRIDAY)
        setupBar(R.id.bar_sabtu, R.id.tv_val_sabtu, 0f, dayOfWeek >= Calendar.SATURDAY)
        setupBar(R.id.bar_minggu, R.id.tv_val_minggu, 0f, dayOfWeek == Calendar.SUNDAY)

        // 5. TAMPILKAN CATATAN HARIAN (LIST MAKANAN)
        val tvFoodList = findViewById<TextView>(R.id.tv_food_list)
        val historyData = sharedPref.getString("daily_food_history", "")

        if (!historyData.isNullOrEmpty()) {
            val items = historyData.split("#")
            val buildTeks = StringBuilder()

            items.forEach { item ->
                val detail = item.split("|")
                if (detail.size == 3) {
                    // Format: "Jam   Nama Makanan   Kalori"
                    buildTeks.append("${detail[1]}      ${detail[0]}      ${detail[2]}\n\n")
                }
            }
            tvFoodList?.text = buildTeks.toString()
        } else {
            tvFoodList?.text = "Belum ada catatan makan hari ini."
        }

        // 6. Update Kartu Nutrisi (Protein & Karbo)
        findViewById<TextView>(R.id.tv_protein_avg)?.text = "${consumedProtein.toInt()}g avg"
        findViewById<TextView>(R.id.tv_carbs_avg)?.text = "${consumedCarbs.toInt()}g avg"
    }

    private fun setupBar(barId: Int, textId: Int, value: Float, isPastOrCurrentDay: Boolean) {
        val bar = findViewById<ProgressBar>(barId)
        val text = findViewById<TextView>(textId)
        val targetLimit = 2000f

        if (isPastOrCurrentDay && value > 0) {
            bar?.let {
                it.max = targetLimit.toInt()
                it.progress = if (value > targetLimit) targetLimit.toInt() else value.toInt()
                it.alpha = 1.0f
            }
            if (value >= 1000) {
                text?.text = String.format(Locale.US, "%.2fk", value / 1000)
            } else {
                text?.text = value.toInt().toString()
            }
            text?.visibility = android.view.View.VISIBLE
        } else {
            bar?.progress = 0
            bar?.alpha = 0.3f
            text?.visibility = android.view.View.INVISIBLE
        }
    }
}