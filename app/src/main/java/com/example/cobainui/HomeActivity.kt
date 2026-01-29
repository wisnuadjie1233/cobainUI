package com.example.cobainui

import android.os.Bundle
import android.os.Handler
import android.os.Looper
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

    private lateinit var auth: FirebaseAuth
    private var backPressedOnce = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        auth = FirebaseAuth.getInstance()
        val currentUser = auth.currentUser

        val welcomeMessage = findViewById<TextView>(R.id.welcome_message)
        // Ambil nama pengguna dari Firebase Auth jika tersedia
        val displayName = currentUser?.displayName ?: "Dewa"
        welcomeMessage.text = "Selamat Pagi, $displayName!"

        setupDateRecyclerView()
        setupCaloriesProgressBar()

        // Handle back press
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (backPressedOnce) {
                    finishAffinity() // Keluar dari aplikasi
                    return
                }

                backPressedOnce = true
                Toast.makeText(this@HomeActivity, "Tekan lagi untuk keluar", Toast.LENGTH_SHORT).show()

                Handler(Looper.getMainLooper()).postDelayed({
                    backPressedOnce = false
                }, 2000) // 2 detik
            }
        })
    }

    private fun setupDateRecyclerView() {
        val dateRecyclerView = findViewById<RecyclerView>(R.id.date_recycler_view)
        dateRecyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)

        val dates = getWeekDates()
        val adapter = DateAdapter(dates)
        dateRecyclerView.adapter = adapter

        // Set tanggal hari ini sebagai yang dipilih
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

        // Mundur ke hari Senin
        calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)

        for (i in 0..6) {
            dates.add(DateItem(calendar.clone() as Calendar))
            calendar.add(Calendar.DAY_OF_MONTH, 1)
        }
        return dates
    }

    private fun setupCaloriesProgressBar() {
        val caloriesProgressBar = findViewById<CircularProgressBar>(R.id.calories_progress_bar)
        // Anda bisa mengatur progres dan teks kalori dari data yang sebenarnya
        caloriesProgressBar.progress = 65f
    }
}
