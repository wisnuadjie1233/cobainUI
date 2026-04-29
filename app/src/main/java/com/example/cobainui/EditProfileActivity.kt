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

    // Variabel untuk menyimpan data awal (sebagai pembanding)
    private var initialName = ""
    private var initialWeight = 0f
    private var initialHeight = 0f
    private var initialGender = "Pria"
    private var initialAge = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_profile)

        auth = FirebaseAuth.getInstance()
        val currentUser = auth.currentUser
        val sharedPref = getSharedPreferences("UserStats", MODE_PRIVATE)

        val btnBack = findViewById<ImageButton>(R.id.btn_back_edit_profile)
        val etName = findViewById<TextInputEditText>(R.id.et_edit_name)
        val etEmail = findViewById<TextInputEditText>(R.id.et_edit_email)
        val etWeight = findViewById<TextInputEditText>(R.id.et_edit_weight)
        val etHeight = findViewById<TextInputEditText>(R.id.et_edit_height)
        val btnBirthDate = findViewById<Button>(R.id.btn_birth_date)
        val spinnerGender = findViewById<Spinner>(R.id.spinner_gender)
        val btnSave = findViewById<MaterialButton>(R.id.btn_save_profile)

        // 1. LOAD DATA AWAL
        initialName = currentUser?.displayName ?: ""
        initialWeight = sharedPref.getFloat("user_weight", 0f)
        initialHeight = sharedPref.getFloat("user_height", 0f)
        initialGender = sharedPref.getString("user_gender", "Pria") ?: "Pria"
        initialAge = sharedPref.getInt("user_age", 0)
        userAge = initialAge

        etName.setText(initialName)
        etEmail.setText(currentUser?.email ?: "Guest")
        etEmail.isEnabled = false
        if (initialWeight > 0) etWeight.setText(initialWeight.toString())
        if (initialHeight > 0) etHeight.setText(initialHeight.toString())
        if (initialAge > 0) btnBirthDate.text = "Umur: $initialAge Tahun"

        val genders = arrayOf("Pria", "Wanita")
        spinnerGender.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, genders)
        spinnerGender.setSelection(if (initialGender == "Pria") 0 else 1)

        // 2. LOGIKA MATIKAN TOMBOL SIMPAN DI AWAL
        btnSave.isEnabled = false
        btnSave.alpha = 0.5f

        // Fungsi untuk cek apakah ada perubahan
        fun checkChanges() {
            val currentName = etName.text.toString().trim()
            val currentWeight = etWeight.text.toString().toFloatOrNull() ?: 0f
            val currentHeight = etHeight.text.toString().toFloatOrNull() ?: 0f
            val currentGender = spinnerGender.selectedItem.toString()

            val isChanged = currentName != initialName ||
                    currentWeight != initialWeight ||
                    currentHeight != initialHeight ||
                    currentGender != initialGender ||
                    userAge != initialAge

            val isInputValid = currentName.isNotEmpty() && currentWeight > 0 && currentHeight > 0 && userAge > 0

            btnSave.isEnabled = isChanged && isInputValid
            btnSave.alpha = if (btnSave.isEnabled) 1.0f else 0.5f
        }

        // 3. PASANG WATCHER PADA SEMUA INPUT
        val watcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) { checkChanges() }
        }
        etName.addTextChangedListener(watcher)
        etWeight.addTextChangedListener(watcher)
        etHeight.addTextChangedListener(watcher)

        spinnerGender.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: android.view.View?, pos: Int, id: Long) { checkChanges() }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        btnBirthDate.setOnClickListener {
            DatePickerDialog(this, { _, year, month, day ->
                selectedCalendar.set(year, month, day)
                val today = Calendar.getInstance()
                userAge = today.get(Calendar.YEAR) - year
                if (today.get(Calendar.DAY_OF_YEAR) < selectedCalendar.get(Calendar.DAY_OF_YEAR)) userAge--
                btnBirthDate.text = "$day/${month + 1}/$year (Umur: $userAge)"
                checkChanges()
            }, 2000, 0, 1).show()
        }

        btnBack.setOnClickListener { finish() }

        // 4. SIMPAN DENGAN POPUP HASIL HITUNGAN
        btnSave.setOnClickListener {
            val weight = etWeight.text.toString().toFloat()
            val height = etHeight.text.toString().toFloat()
            val gender = spinnerGender.selectedItem.toString()

            // Hitung Target Kalori (Mifflin-St Jeor)
            val bmr = if (gender == "Pria") {
                (10 * weight) + (6.25 * height) - (5 * userAge) + 5
            } else {
                (10 * weight) + (6.25 * height) - (5 * userAge) - 161
            }
            val targetCalories = (bmr * 1.2f).toInt()

            sharedPref.edit().apply {
                putFloat("user_weight", weight)
                putFloat("user_height", height)
                putString("user_gender", gender)
                putInt("user_age", userAge)
                putFloat("daily_target_calories", targetCalories.toFloat())
                apply()
            }

            val profileUpdates = UserProfileChangeRequest.Builder().setDisplayName(etName.text.toString()).build()
            currentUser?.updateProfile(profileUpdates)?.addOnCompleteListener {
                // POPUP INFORMASI KALORI TERBARU
                Toast.makeText(this, "Profil Diperbarui! Target harian Anda: $targetCalories kkal", Toast.LENGTH_LONG).show()
                finish()
            }
        }
    }
}
