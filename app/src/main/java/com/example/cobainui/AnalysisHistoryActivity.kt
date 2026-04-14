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

        // 2. Ambil Data Real-time dari SharedPreferences (Data dari Home)
        val sharedPref = getSharedPreferences("UserStats", MODE_PRIVATE)
        val consumedCal = sharedPref.getFloat("consumed_calories", 0f)
        val consumedCarbs = sharedPref.getFloat("consumed_carbs", 0f)
        val consumedProtein = sharedPref.getFloat("consumed_protein", 0f)

        // 3. Logika Rata-rata Mingguan (Senin + Selasa)
        // Kita kunci Senin di 1200 kkal agar sinkron dengan visual grafiknya
        val seninCal = 1200f
        val selasaCal = consumedCal // Data asli dari Home (Misal: 2250)

        // Rumus Rata-rata: (1200 + 2250) / 2 = 1725 kkal
        val avgCal = (seninCal + selasaCal) / 2

        val tvAverageTotal = findViewById<TextView>(R.id.tv_average_total)
        tvAverageTotal?.text = String.format(Locale.US, "%,.0f kkal", avgCal)

        // 4. Logika Deteksi Hari (Senin=2, Selasa=3, dst)
        val calendar = Calendar.getInstance()
        val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)

        // --- SETUP GRAFIK SENIN ---
        // Tampil jika hari ini sudah lewat Senin (dayOfWeek > 2)
        setupBar(R.id.bar_senin, R.id.tv_val_senin, seninCal, isPastOrCurrentDay = dayOfWeek >= 2)

        // --- SETUP GRAFIK SELASA ---
        // Tampil jika hari ini Selasa atau lebih (dayOfWeek >= 3)
        // Jika hari ini Selasa, ambil data 'consumedCal'
        val selasaDisplayVal = if (dayOfWeek == 3) consumedCal else if (dayOfWeek > 3) 2000f else 0f
        setupBar(R.id.bar_selasa, R.id.tv_val_selasa, selasaDisplayVal, isPastOrCurrentDay = dayOfWeek >= 3)

        // --- SETUP HARI RABU - MINGGU (Dikosongkan jika belum harinya) ---
        setupBar(R.id.bar_rabu, R.id.tv_val_rabu, 0f, isPastOrCurrentDay = dayOfWeek >= 4)
        setupBar(R.id.bar_kamis, R.id.tv_val_kamis, 0f, isPastOrCurrentDay = dayOfWeek >= 5)
        setupBar(R.id.bar_jumat, R.id.tv_val_jumat, 0f, isPastOrCurrentDay = dayOfWeek >= 6)
        setupBar(R.id.bar_sabtu, R.id.tv_val_sabtu, 0f, isPastOrCurrentDay = dayOfWeek >= 7)
        setupBar(R.id.bar_minggu, R.id.tv_val_minggu, 0f, isPastOrCurrentDay = dayOfWeek == 1)

        // 5. Update Kartu Nutrisi di bawah (Protein & Karbo)
        findViewById<TextView>(R.id.tv_protein_avg)?.text = "${consumedProtein.toInt()}g avg"
        findViewById<TextView>(R.id.tv_carbs_avg)?.text = "${consumedCarbs.toInt()}g avg"
    }

    /**
     * Fungsi sakti untuk mengatur batang grafik agar tidak overlap (stuck)
     * dan menampilkan teks k yang presisi (pakai %.2f agar 2250 jadi 2.25k)
     */
    private fun setupBar(barId: Int, textId: Int, value: Float, isPastOrCurrentDay: Boolean) {
        val bar = findViewById<ProgressBar>(barId)
        val text = findViewById<TextView>(textId)
        val targetLimit = 2000f

        if (isPastOrCurrentDay && value > 0) {
            bar?.let {
                it.max = targetLimit.toInt()
                // Logika STUCK: jika kalori > 2000, batang tetap penuh di 2000
                it.progress = if (value > targetLimit) targetLimit.toInt() else value.toInt()
                it.alpha = 1.0f // Terang
            }

            // Format angka: Jika 2250 kkal tampilkan 2.25k (lebih presisi)
            if (value >= 1000) {
                text?.text = String.format(Locale.US, "%.2fk", value / 1000)
            } else {
                text?.text = value.toInt().toString()
            }
            text?.visibility = android.view.View.VISIBLE
        } else {
            // Jika hari belum dilewati, bar dibuat redup dan teks hilang
            bar?.progress = 0
            bar?.alpha = 0.3f
            text?.visibility = android.view.View.INVISIBLE
        }
    }
}