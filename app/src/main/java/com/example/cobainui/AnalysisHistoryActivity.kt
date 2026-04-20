package com.example.cobainui

import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.util.*

class AnalysisHistoryActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_analysis_history)

        findViewById<ImageButton>(R.id.btn_back_history)?.setOnClickListener {
            finish()
            overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right)
        }

        val sharedPref = getSharedPreferences("UserStats", MODE_PRIVATE)
        val calendar = Calendar.getInstance()
        val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)

        // 1. Ambil Data Hari Ini dari Home
        val consumedToday = sharedPref.getFloat("consumed_calories", 0f)

        // 2. Ambil Data "Gudang" (History). Jika kosong, sekarang default-nya 0f (JUJUR)
        val calMon = if (dayOfWeek == Calendar.MONDAY) consumedToday else sharedPref.getFloat("history_cal_Mon", 0f)
        val calTue = if (dayOfWeek == Calendar.TUESDAY) consumedToday else sharedPref.getFloat("history_cal_Tue", 0f)
        val calWed = if (dayOfWeek == Calendar.WEDNESDAY) consumedToday else sharedPref.getFloat("history_cal_Wed", 0f)
        val calThu = if (dayOfWeek == Calendar.THURSDAY) consumedToday else sharedPref.getFloat("history_cal_Thu", 0f)
        val calFri = if (dayOfWeek == Calendar.FRIDAY) consumedToday else sharedPref.getFloat("history_cal_Fri", 0f)
        val calSat = if (dayOfWeek == Calendar.SATURDAY) consumedToday else sharedPref.getFloat("history_cal_Sat", 0f)
        val calSun = if (dayOfWeek == Calendar.SUNDAY) consumedToday else sharedPref.getFloat("history_cal_Sun", 0f)

        // 3. Logika Rata-rata (Hanya membagi hari yang sudah dilewati)
        val urutanHari = if (dayOfWeek == Calendar.SUNDAY) 7 else dayOfWeek - 1
        val totalMingguIni = calMon + calTue + calWed + calThu + calFri + calSat + calSun

        // Menghindari pembagian dengan nol
        val avgCal = if (urutanHari > 0) totalMingguIni / urutanHari else 0f

        val tvAverageTotal = findViewById<TextView>(R.id.tv_average_total)
        tvAverageTotal?.text = String.format(Locale.US, "%,.0f kkal", avgCal)

        // 4. Update Grafik Batang (Tampil sesuai urutan hari)
        setupBar(R.id.bar_senin, R.id.tv_val_senin, R.id.tv_targetlabel_senin, calMon, urutanHari >= 1)
        setupBar(R.id.bar_selasa, R.id.tv_val_selasa, R.id.tv_targetlabel_selasa, calTue, urutanHari >= 2)
        setupBar(R.id.bar_rabu, R.id.tv_val_rabu, R.id.tv_targetlabel_rabu, calWed, urutanHari >= 3)
        setupBar(R.id.bar_kamis, R.id.tv_val_kamis, R.id.tv_targetlabel_kamis, calThu, urutanHari >= 4)
        setupBar(R.id.bar_jumat, R.id.tv_val_jumat, R.id.tv_targetlabel_jumat, calFri, urutanHari >= 5)
        setupBar(R.id.bar_sabtu, R.id.tv_val_sabtu, R.id.tv_targetlabel_sabtu, calSat, urutanHari >= 6)
        setupBar(R.id.bar_minggu, R.id.tv_val_minggu, R.id.tv_targetlabel_minggu, calSun, urutanHari >= 7)

        // 5. Tampilkan Catatan Harian & Nutrisi lainnya
        updateNutrientCards(sharedPref)
        displayFoodHistory(sharedPref)
    }

    private fun setupBar(barId: Int, textId: Int, targetLabelId: Int, value: Float, isVisible: Boolean) {
        val bar = findViewById<ProgressBar>(barId)
        val textVal = findViewById<TextView>(textId)
        val textTarget = findViewById<TextView>(targetLabelId)

        val sharedPref = getSharedPreferences("UserStats", MODE_PRIVATE)
        val targetUser = sharedPref.getFloat("daily_target_calories", 2000f)

        // Update Label Target di atas (misal 2.5k)
        textTarget?.text = String.format(Locale.US, "%.1fk", targetUser / 1000)

        // --- PERBAIKAN ALPHA DI SINI ---
        bar?.let {
            it.max = targetUser.toInt()
            it.progress = if (value > targetUser) targetUser.toInt() else value.toInt()

            // Set alpha selalu 1.0f agar warna wadah abu-abunya seragam semua hari
            it.alpha = 0.2f
        }

        if (isVisible) {
            // Tampilkan angka (0 atau ribuan) hanya untuk hari yang sudah lewat/hari ini
            textVal?.text = when {
                value >= 1000 -> String.format(Locale.US, "%.2fk", value / 1000)
                else -> value.toInt().toString()
            }
            textVal?.visibility = View.VISIBLE
        } else {
            // Hari esok: Sembunyikan teks angkanya saja, wadah ProgressBar tetap terlihat sama
            textVal?.visibility = View.INVISIBLE
        }
    }

    private fun displayFoodHistory(pref: android.content.SharedPreferences) {
        val tvFoodList = findViewById<TextView>(R.id.tv_food_list)
        val data = pref.getString("daily_food_history", "")
        if (!data.isNullOrEmpty()) {
            val build = StringBuilder()
            data.split("#").forEach {
                val d = it.split("|")
                if (d.size >= 3) build.append("${d[1]}      ${d[0]}      ${d[2]}\n\n")
            }
            tvFoodList?.text = build.toString()
        } else {
            tvFoodList?.text = "Belum ada catatan makan hari ini."
        }
    }

    private fun updateNutrientCards(pref: android.content.SharedPreferences) {
        findViewById<TextView>(R.id.tv_protein_avg)?.text = "${pref.getFloat("consumed_protein", 0f).toInt()}g avg"
        findViewById<TextView>(R.id.tv_carbs_avg)?.text = "${pref.getFloat("consumed_carbs", 0f).toInt()}g avg"
        findViewById<TextView>(R.id.tv_sugar_avg)?.text = "${pref.getFloat("consumed_sugar", 0f).toInt()}g avg"
        findViewById<TextView>(R.id.tv_fat_avg)?.text = "${pref.getFloat("consumed_fat", 0f).toInt()}g avg"
    }
}