package com.example.cobainui

import android.os.Bundle
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest

class EditProfileActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_profile)

        auth = FirebaseAuth.getInstance()
        val currentUser = auth.currentUser

        val btnBack = findViewById<ImageButton>(R.id.btn_back_edit_profile)
        val etName = findViewById<TextInputEditText>(R.id.et_edit_name)
        val etEmail = findViewById<TextInputEditText>(R.id.et_edit_email)
        val btnSave = findViewById<MaterialButton>(R.id.btn_save_profile)

        // 1. Ambil data user dan simpan nama awal untuk perbandingan
        val initialName = currentUser?.displayName ?: "" // TAMBAHKAN BARIS INI

        if (currentUser != null) {
            etName.setText(initialName)
            etEmail.setText(currentUser.email)
        }

        // SET TOMBOL MATI SAAT PERTAMA KALI DIBUKA
        btnSave.isEnabled = false
        btnSave.alpha = 0.5f

        // 2. Logika Tombol Kembali
        btnBack.setOnClickListener {
            finish()
            overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right)
        }

        etName.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: android.text.Editable?) {
                val currentName = s.toString().trim()

                // Cek: Apakah teks sekarang berbeda dengan nama awal?
                val isChanged = currentName != initialName && currentName.isNotEmpty()

                if (isChanged) {
                    btnSave.isEnabled = true
                    btnSave.alpha = 1.0f // Terang (Aktif)
                } else {
                    btnSave.isEnabled = false
                    btnSave.alpha = 0.5f // Redup (Mati)
                }
            }
        })

        // 3. Logika Simpan Perubahan
        btnSave.setOnClickListener {
            val newName = etName.text.toString().trim()

            if (newName.isEmpty()) {
                etName.error = "Nama tidak boleh kosong"
                return@setOnClickListener
            }

            val profileUpdates = UserProfileChangeRequest.Builder()
                .setDisplayName(newName)
                .build()

            currentUser?.updateProfile(profileUpdates)
                ?.addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Toast.makeText(this, "Profil diperbarui!", Toast.LENGTH_SHORT).show()
                        finish() // Balik ke halaman pengaturan
                    } else {
                        Toast.makeText(this, "Gagal memperbarui profil", Toast.LENGTH_SHORT).show()
                    }
                }
        }
    }
}