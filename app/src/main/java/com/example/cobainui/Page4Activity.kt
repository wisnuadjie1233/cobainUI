package com.example.cobainui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

class Page4Activity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private var backPressedOnce = false

    private lateinit var skipButton: TextView
    private lateinit var loginTitle: TextView
    private lateinit var emailInput: EditText
    private lateinit var usernameInput: EditText
    private lateinit var passwordInputLayout: TextInputLayout
    private lateinit var passwordInput: EditText
    private lateinit var loginButton: Button
    private lateinit var signupPrompt: TextView
    private lateinit var signupLink: TextView
    private lateinit var forgotPasswordLink: TextView
    private lateinit var googleButton: Button
    private lateinit var emailError: TextView
    private lateinit var usernameError: TextView
    private lateinit var passwordError: TextView
    private lateinit var generalError: TextView
    private lateinit var usernameLabel: TextView

    private var isLoginMode = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_page4)

        auth = Firebase.auth

        loginTitle = findViewById(R.id.login_title)
        emailInput = findViewById(R.id.email_input)
        usernameInput = findViewById(R.id.username_input)
        passwordInputLayout = findViewById(R.id.password_input_layout)
        passwordInput = findViewById(R.id.password_input)
        loginButton = findViewById(R.id.login_button)
        signupPrompt = findViewById(R.id.signup_prompt)
        signupLink = findViewById(R.id.signup_link)
        forgotPasswordLink = findViewById(R.id.forgot_password)
        googleButton = findViewById(R.id.google_button)
        emailError = findViewById(R.id.email_error)
        usernameError = findViewById(R.id.username_error)
        passwordError = findViewById(R.id.password_error)
        generalError = findViewById(R.id.general_error)
        usernameLabel = findViewById(R.id.username_label)
        skipButton = findViewById(R.id.skip_button)

        loginButton.setOnClickListener {
            if (isLoginMode) {
                performSignIn()
            } else {
                performSignUp()
            }
        }

        addTextChangeListeners()

        signupLink.setOnClickListener {
            toggleMode()
        }

        forgotPasswordLink.setOnClickListener {
            val intent = Intent(this, ForgotPasswordActivity::class.java)
            startActivity(intent)
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
        }

        googleButton.setOnClickListener {
            showCustomToast("Fitur Google Sign-In akan segera hadir!")
        }

        skipButton.setOnClickListener {
            signInAsGuest()
        }

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

        // --- LOGIKA MEMBACA LOGIN TERAKHIR ---
        val sharedPref = getSharedPreferences("LoginPrefs", Context.MODE_PRIVATE)
        val savedEmail = sharedPref.getString("last_email", "")
        val savedPassword = sharedPref.getString("last_password", "")

        if (!savedEmail.isNullOrEmpty()) {
            emailInput.setText(savedEmail)
        }
        if (!savedPassword.isNullOrEmpty()) {
            passwordInput.setText(savedPassword)
        }
        // -------------------------------------

    }

    private fun showCustomToast(message: String) {
        val inflater = LayoutInflater.from(this)
        val layout: View = inflater.inflate(R.layout.custom_toast_layout, null)
        val textView: TextView = layout.findViewById(R.id.toast_text)
        textView.text = message
        with(Toast(applicationContext)) {
            duration = Toast.LENGTH_SHORT
            view = layout
            show()
        }
    }

    private fun validateForm(): Boolean {
        val email = emailInput.text.toString().trim()
        val password = passwordInput.text.toString().trim()
        val username = usernameInput.text.toString().trim()
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
            isValid = false
        } else {
            passwordError.visibility = View.GONE
        }

        if (!isLoginMode && username.isEmpty()) {
            usernameError.visibility = View.VISIBLE
            usernameInput.background = ContextCompat.getDrawable(this, R.drawable.bg_dark_edittext_error)
            isValid = false
        } else {
            usernameError.visibility = View.GONE
            usernameInput.background = ContextCompat.getDrawable(this, R.drawable.bg_dark_edittext)
        }

        return isValid
    }

    private fun addTextChangeListeners() {
        val textWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                generalError.visibility = View.GONE
                if (emailInput.hasFocus() && emailInput.text.isNotEmpty()) {
                    emailError.visibility = View.GONE
                    emailInput.background = ContextCompat.getDrawable(this@Page4Activity, R.drawable.bg_dark_edittext)
                }
                if (passwordInput.hasFocus() && passwordInput.text.isNotEmpty()) {
                    passwordError.visibility = View.GONE
                }
                if (usernameInput.hasFocus() && usernameInput.text.isNotEmpty()) {
                    usernameError.visibility = View.GONE
                    usernameInput.background = ContextCompat.getDrawable(this@Page4Activity, R.drawable.bg_dark_edittext)
                }
            }
        }
        emailInput.addTextChangedListener(textWatcher)
        passwordInput.addTextChangedListener(textWatcher)
        usernameInput.addTextChangedListener(textWatcher)
    }

    private fun performSignIn() {
        generalError.visibility = View.GONE
        if (!validateForm()) return

        val email = emailInput.text.toString().trim()
        val password = passwordInput.text.toString().trim()

        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser

                    if (user != null && user.isEmailVerified) {

                        // === TAMBAHKAN LOGIKA SIMPAN DI SINI ===
                        val sharedPref = getSharedPreferences("LoginPrefs", Context.MODE_PRIVATE)
                        val editor = sharedPref.edit()
                        editor.putString("last_email", email)
                        editor.putString("last_password", password)
                        editor.apply()
                        // ======================================

                        goToHomeActivity()
                    } else {
                        showCustomToast("Email belum diverifikasi. Cek inbox kamu!")
                        auth.signOut()
                    }
                } else {
                    generalError.visibility = View.VISIBLE
                }
            }
    }

    private fun performSignUp() {
        generalError.visibility = View.GONE
        if (!validateForm()) return

        val email = emailInput.text.toString().trim()
        val password = passwordInput.text.toString().trim()
        val username = usernameInput.text.toString().trim()

        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    val profileUpdates = UserProfileChangeRequest.Builder()
                        .setDisplayName(username)
                        .build()

                    user?.updateProfile(profileUpdates)?.addOnCompleteListener { updateTask ->
                        if (updateTask.isSuccessful) {
                            // KIRIM LINK VERIFIKASI KE EMAIL
                            user.sendEmailVerification().addOnCompleteListener { verifyTask ->
                                if (verifyTask.isSuccessful) {
                                    showCustomToast("Daftar Berhasil! Cek email untuk verifikasi.")
                                    auth.signOut() // Logout paksa supaya dia tidak langsung masuk
                                    if (!isLoginMode) toggleMode() // Balikkan ke tampilan 'Masuk'
                                } else {
                                    showCustomToast("Gagal mengirim email verifikasi.")
                                }
                            }
                        }
                    }
                } else {
                    showCustomToast("Pendaftaran Gagal: ${task.exception?.message}")
                }
            }
    }

    private fun toggleMode() {
        isLoginMode = !isLoginMode

        emailError.visibility = View.GONE
        passwordError.visibility = View.GONE
        usernameError.visibility = View.GONE
        generalError.visibility = View.GONE
        emailInput.background = ContextCompat.getDrawable(this, R.drawable.bg_dark_edittext)
        usernameInput.background = ContextCompat.getDrawable(this, R.drawable.bg_dark_edittext)

        if (isLoginMode) {
            loginTitle.text = "Selamat Datang Kembali!"
            loginButton.text = "Masuk"
            signupPrompt.text = "Belum punya akun? "
            signupLink.text = "Daftar"
            forgotPasswordLink.visibility = View.VISIBLE
            usernameInput.visibility = View.GONE
            usernameLabel.visibility = View.GONE
            usernameError.visibility = View.GONE
        } else {
            loginTitle.text = "Buat Akun Baru"
            loginButton.text = "Daftar"
            signupPrompt.text = "Sudah punya akun? "
            signupLink.text = "Masuk"
            forgotPasswordLink.visibility = View.GONE
            usernameInput.visibility = View.VISIBLE
            usernameLabel.visibility = View.VISIBLE
        }
    }

    private fun signInAsGuest() {
        val sharedPreferences = getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        sharedPreferences.edit().putBoolean("skipped_login", true).apply()

        auth.signInAnonymously()
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    goToHomeActivity()
                } else {
                    showCustomToast("Gagal masuk sebagai tamu: ${task.exception?.message}")
                }
            }
    }

    private fun goToHomeActivity() {
        val intent = Intent(this, HomeActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
        finish()
    }
}
