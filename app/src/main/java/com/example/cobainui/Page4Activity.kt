package com.example.cobainui

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

class Page4Activity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private var backPressedOnce = false

    // Variabel untuk komponen UI
    private lateinit var loginTitle: TextView
    private lateinit var emailInput: EditText
    private lateinit var passwordInput: EditText
    private lateinit var loginButton: Button
    private lateinit var signupPrompt: TextView
    private lateinit var signupLink: TextView
    private lateinit var forgotPasswordLink: TextView
    private lateinit var googleButton: Button
    private lateinit var emailError: TextView
    private lateinit var passwordError: TextView
    private lateinit var generalError: TextView

    // State untuk melacak apakah kita di mode Login atau Register
    private var isLoginMode = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_page4)

        // Inisialisasi Firebase Auth
        auth = Firebase.auth

        // Hubungkan variabel dengan komponen di layout
        loginTitle = findViewById(R.id.login_title)
        emailInput = findViewById(R.id.email_input)
        passwordInput = findViewById(R.id.password_input)
        loginButton = findViewById(R.id.login_button)
        signupPrompt = findViewById(R.id.signup_prompt)
        signupLink = findViewById(R.id.signup_link)
        forgotPasswordLink = findViewById(R.id.forgot_password)
        googleButton = findViewById(R.id.google_button)
        emailError = findViewById(R.id.email_error)
        passwordError = findViewById(R.id.password_error)
        generalError = findViewById(R.id.general_error)

        // Set listener untuk tombol utama (yang bisa "Masuk" atau "Daftar")
        loginButton.setOnClickListener {
            if (isLoginMode) {
                performSignIn()
            } else {
                performSignUp()
            }
        }

        // Listener untuk menghilangkan error saat user mengetik
        addTextChangeListeners()

        // Set listener untuk link ganti mode (Daftar/Masuk)
        signupLink.setOnClickListener {
            toggleMode()
        }

        // Set listener untuk Lupa Password
        forgotPasswordLink.setOnClickListener {
            val intent = Intent(this, ForgotPasswordActivity::class.java)
            startActivity(intent)
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
        }

        // Set listener untuk Google Sign-In (untuk nanti)
        googleButton.setOnClickListener {
            Toast.makeText(this, "Fitur Google Sign-In akan segera hadir!", Toast.LENGTH_SHORT).show()
        }

        // Handle back press
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (backPressedOnce) {
                    finishAffinity() // Keluar dari aplikasi
                    return
                }

                backPressedOnce = true
                Toast.makeText(this@Page4Activity, "Tekan lagi untuk keluar", Toast.LENGTH_SHORT).show()

                Handler(Looper.getMainLooper()).postDelayed({
                    backPressedOnce = false
                }, 2000) // 2 detik
            }
        })
    }

    private fun validateForm(): Boolean {
        val email = emailInput.text.toString().trim()
        val password = passwordInput.text.toString().trim()
        var isValid = true

        if (email.isEmpty()) {
            emailError.visibility = View.VISIBLE
            emailInput.background = ContextCompat.getDrawable(this, R.drawable.bg_dark_edittext_error)
            isValid = false
        } else {
            emailError.visibility = View.GONE
            emailInput.background = ContextCompat.getDrawable(this, R.drawable.bg_dark_edittext)
        }

        if (password.isEmpty()) {
            passwordError.visibility = View.VISIBLE
            passwordInput.background = ContextCompat.getDrawable(this, R.drawable.bg_dark_edittext_error)
            isValid = false
        } else {
            passwordError.visibility = View.GONE
            passwordInput.background = ContextCompat.getDrawable(this, R.drawable.bg_dark_edittext)
        }

        return isValid
    }

    private fun addTextChangeListeners() {
        val textWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                generalError.visibility = View.GONE // Sembunyikan error umum

                if (emailInput.hasFocus()) {
                    if (emailInput.text.isNotEmpty()) {
                        emailError.visibility = View.GONE
                        emailInput.background = ContextCompat.getDrawable(this@Page4Activity, R.drawable.bg_dark_edittext)
                    }
                }
                if (passwordInput.hasFocus()) {
                    if (passwordInput.text.isNotEmpty()) {
                        passwordError.visibility = View.GONE
                        passwordInput.background = ContextCompat.getDrawable(this@Page4TActivity, R.drawable.bg_dark_edittext)
                    }
                }
            }
        }
        emailInput.addTextChangedListener(textWatcher)
        passwordInput.addTextChangedListener(textWatcher)
    }

    private fun performSignIn() {
        generalError.visibility = View.GONE // Selalu sembunyikan sebelum mencoba
        if (!validateForm()) return

        val email = emailInput.text.toString().trim()
        val password = passwordInput.text.toString().trim()

        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    Toast.makeText(this, "Login Berhasil!", Toast.LENGTH_SHORT).show()
                    val intent = Intent(this, HomeActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
                    finish()
                } else {
                    generalError.visibility = View.VISIBLE
                }
            }
    }

    private fun performSignUp() {
        generalError.visibility = View.GONE // Selalu sembunyikan sebelum mencoba
        if (!validateForm()) return

        val email = emailInput.text.toString().trim()
        val password = passwordInput.text.toString().trim()

        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    Toast.makeText(this, "Pendaftaran Berhasil! Silakan Masuk.", Toast.LENGTH_LONG).show()
                    toggleMode() // Kembali ke mode login
                } else {
                    // Untuk pendaftaran, lebih baik menampilkan pesan error spesifik dari Firebase
                    Toast.makeText(this, "Pendaftaran Gagal: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                }
            }
    }

    private fun toggleMode() {
        isLoginMode = !isLoginMode // Balik state

        // Reset error states
        emailError.visibility = View.GONE
        passwordError.visibility = View.GONE
        generalError.visibility = View.GONE
        emailInput.background = ContextCompat.getDrawable(this, R.drawable.bg_dark_edittext)
        passwordInput.background = ContextCompat.getDrawable(this, R.drawable.bg_dark_edittext)

        if (isLoginMode) {
            // Ubah ke Mode Login
            loginTitle.text = "Selamat Datang Kembali!"
            loginButton.text = "Masuk"
            signupPrompt.text = "Belum punya akun? "
            signupLink.text = "Daftar"
            forgotPasswordLink.visibility = View.VISIBLE // Munculkan lagi
        } else {
            // Ubah ke Mode Registrasi
            loginTitle.text = "Buat Akun Baru"
            loginButton.text = "Daftar"
            signupPrompt.text = "Sudah punya akun? "
            signupLink.text = "Masuk"
            forgotPasswordLink.visibility = View.GONE // Sembunyikan saat mendaftar
        }
    }
}
