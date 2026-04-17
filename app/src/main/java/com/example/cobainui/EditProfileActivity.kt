package com.example.cobainui

import android.app.DatePickerDialog
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import java.util.*

class EditProfileActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private var selectedCalendar = Calendar.getInstance()
    private var userAge = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_profile)

        auth = FirebaseAuth.getInstance()
        val currentUser = auth.currentUser
        val sharedPref = getSharedPreferences("UserStats", MODE_PRIVATE)

        // Inisialisasi View (Pastikan ID ini ada di XML kamu)
        val btnBack = findViewById<ImageButton>(R.id.btn_back_edit_profile)
        val etName = findViewById<TextInputEditText>(R.id.et_edit_name)
        val etWeight = findViewById<TextInputEditText>(R.id.et_edit_weight) // Input BB
        val etHeight = findViewById<TextInputEditText>(R.id.et_edit_height) // Input TB
        val btnBirthDate = findViewById<Button>(R.id.btn_birth_date)        // Tombol Tgl Lahir
        val spinnerGender = findViewById<Spinner>(R.id.spinner_gender)      // Pilih Gender
        val btnSave = findViewById<MaterialButton>(R.id.btn_save_profile)

        // 1. Load Data Lama (Jika ada)
        val initialName = currentUser?.displayName ?: ""
        val initialWeight = sharedPref.getFloat("user_weight", 0f)
        val initialHeight = sharedPref.getFloat("user_height", 0f)
        val initialGender = sharedPref.getString("user_gender", "Pria") ?: "Pria"

        etName.setText(initialName)
        if (initialWeight > 0) etWeight.setText(initialWeight.toString())
        if (initialHeight > 0) etHeight.setText(initialHeight.toString())

        // 2. Setup Spinner Gender
        val genders = arrayOf("Pria", "Wanita")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, genders)
        spinnerGender.adapter = adapter
        spinnerGender.setSelection(if (initialGender == "Pria") 0 else 1)

        // --- Load Data User saat Activity dibuka ---
        val etEmail = findViewById<TextInputEditText>(R.id.et_edit_email)

// Tampilkan email asli dari Firebase
        etEmail.setText(currentUser?.email ?: "Tidak ada email")

// Tambahkan juga pengambilan data tanggal lahir/umur jika sudah tersimpan
        userAge = sharedPref.getInt("user_age", 0)
        val savedWeight = sharedPref.getFloat("user_weight", 0f)
        val savedHeight = sharedPref.getFloat("user_height", 0f)

        if (userAge > 0) {
            btnBirthDate.text = "Umur: $userAge Tahun"
        }

        // 3. Setup DatePicker (Hitung Umur)
        btnBirthDate.setOnClickListener {
            val datePicker = DatePickerDialog(this, { _, year, month, day ->
                selectedCalendar.set(year, month, day)
                val today = Calendar.getInstance()
                userAge = today.get(Calendar.YEAR) - year
                if (today.get(Calendar.DAY_OF_YEAR) < selectedCalendar.get(Calendar.DAY_OF_YEAR)) userAge--

                btnBirthDate.text = "$day/${month + 1}/$year (Umur: $userAge)"
                checkChanges(etName, initialName, btnSave)
            }, 2000, 0, 1)
            datePicker.show()
        }

        // 4. Tombol Kembali
        btnBack.setOnClickListener {
            finish()
            overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right)
        }

        // 5. Pantau Perubahan (Agar tombol Simpan Nyala)
        val watcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val hasChanged = etName.text.toString().trim() != initialName ||
                        etWeight.text.toString() != initialWeight.toString() ||
                        etHeight.text.toString() != initialHeight.toString()

                btnSave.isEnabled = hasChanged && etWeight.text!!.isNotEmpty() && etHeight.text!!.isNotEmpty()
                btnSave.alpha = if (btnSave.isEnabled) 1.0f else 0.5f
            }
        }
        etName.addTextChangedListener(watcher)
        etWeight.addTextChangedListener(watcher)
        etHeight.addTextChangedListener(watcher)

        // 6. Logika Simpan & Hitung Kalori
        // 6. Logika Simpan & Hitung Kalori
        btnSave.setOnClickListener {
            val name = etName.text.toString().trim()
            val weightStr = etWeight.text.toString()
            val heightStr = etHeight.text.toString()

            if (weightStr.isEmpty() || heightStr.isEmpty()) {
                Toast.makeText(this, "Mohon isi berat dan tinggi badan", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val weight = weightStr.toFloat()
            val height = heightStr.toFloat()
            val gender = spinnerGender.selectedItem.toString()

            // HITUNG PAKAI RUMUS MIFFLIN-ST JEOR
            // Kita tambahkan .toFloat() di akhir perhitungan agar tidak error 'Double'
            val bmr = if (gender == "Pria") {
                (10 * weight) + (6.25 * height) - (5 * userAge) + 5
            } else {
                (10 * weight) + (6.25 * height) - (5 * userAge) - 161
            }

            // Paksa hasil perkalian menjadi Float
            val targetCalories = (bmr * 1.2).toFloat()

            // Simpan ke SharedPreferences
            val editor = sharedPref.edit()
            editor.putFloat("user_weight", weight)
            editor.putFloat("user_height", height)
            editor.putString("user_gender", gender)
            editor.putInt("user_age", userAge)
            editor.putFloat("daily_target_calories", targetCalories) // Karakter <caret> sudah dibuang
            editor.apply()

            // Update Nama di Firebase
            val profileUpdates = UserProfileChangeRequest.Builder().setDisplayName(name).build()
            currentUser?.updateProfile(profileUpdates)?.addOnCompleteListener {
                Toast.makeText(this, "Target Baru: ${targetCalories.toInt()} kkal", Toast.LENGTH_LONG).show()
                finish()
            }
        }
    }

    private fun checkChanges(etName: EditText, initial: String, btn: Button) {
        btn.isEnabled = etName.text.toString().trim() != initial || userAge > 0
        btn.alpha = if (btn.isEnabled) 1.0f else 0.5f
    }
}