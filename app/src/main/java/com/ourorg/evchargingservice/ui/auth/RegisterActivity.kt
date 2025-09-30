package com.ourorg.evchargingservice.ui.auth

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.ourorg.evchargingservice.R
import com.ourorg.evchargingservice.data.local.AppDb
import com.ourorg.evchargingservice.data.local.SessionDao
import com.ourorg.evchargingservice.data.repo.AuthRepo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class RegisterActivity : AppCompatActivity() {
    private lateinit var repo: AuthRepo
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)
        repo = AuthRepo(SessionDao(AppDb(this)))

        val nic = findViewById<EditText>(R.id.etNic)
        val name = findViewById<EditText>(R.id.etName)
        val phone = findViewById<EditText>(R.id.etPhone)
        val email = findViewById<EditText>(R.id.etEmail)
        val pwd = findViewById<EditText>(R.id.etPwd)

        findViewById<Button>(R.id.btnRegister).setOnClickListener {
            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    repo.register(nic.text.toString(), name.text.toString(), phone.text.toString(), email.text.toString(), pwd.text.toString())
                    withContext(Dispatchers.Main) { Toast.makeText(this@RegisterActivity, "Registered. Please login.", Toast.LENGTH_LONG).show(); finish() }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) { Toast.makeText(this@RegisterActivity, e.message, Toast.LENGTH_LONG).show() }
                }
            }
        }
    }
}