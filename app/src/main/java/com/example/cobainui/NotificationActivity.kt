package com.example.cobainui

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import java.text.SimpleDateFormat
import java.util.*

class NotificationActivity : AppCompatActivity() {

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) sendTestNotification()
        else Toast.makeText(this, "Izin ditolak", Toast.LENGTH_SHORT).show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_notification)

        val btnBack = findViewById<ImageButton>(R.id.btn_back)
        val layoutEmpty = findViewById<LinearLayout>(R.id.layout_empty_state)
        val cardNotif = findViewById<CardView>(R.id.card_notification)
        val tvContent = findViewById<TextView>(R.id.tv_notification_content)

        btnBack.setOnClickListener { finish() }

        createNotificationChannel()
        checkPermissionAndSend()

        // REAL TIME LOGIC (Tanpa Jeda)
        val calendar = Calendar.getInstance()
        val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
        val currentTime = SimpleDateFormat("HH:mm", Locale.getDefault()).format(calendar.time)

        val mealMessage = when (currentHour) {
            in 4..10 -> "Waktunya Sarapan!"
            in 11..15 -> "Waktunya Makan Siang!"
            in 16..20 -> "Waktunya Makan Malam!"
            else -> "Waktunya Ngemil Sehat!"
        }

        // Tampilkan langsung tanpa nunggu 3 detik
        layoutEmpty?.visibility = View.GONE
        cardNotif?.visibility = View.VISIBLE
        tvContent?.text = "$currentTime - $mealMessage\nCatat kalorimu sekarang di NutriScan."
    }

    private fun checkPermissionAndSend() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                sendTestNotification()
            }
        } else {
            sendTestNotification()
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel("NUTRI_CHANNEL", "NutriScan", NotificationManager.IMPORTANCE_DEFAULT)
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun sendTestNotification() {
        val calendar = Calendar.getInstance()
        val currentTime = SimpleDateFormat("HH:mm", Locale.getDefault()).format(calendar.time)

        val builder = NotificationCompat.Builder(this, "NUTRI_CHANNEL")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Halo Brader!")
            .setContentText("Sudah jam $currentTime, jangan lupa jaga target kalori harianmu ya.")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)

        try {
            val manager = NotificationManagerCompat.from(this)
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED || Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                manager.notify(1, builder.build())
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
