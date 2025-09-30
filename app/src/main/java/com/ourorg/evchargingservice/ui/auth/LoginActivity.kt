package com.ourorg.evchargingservice.ui.auth

import android.content.Intent
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
import com.ourorg.evchargingservice.ui.owner.OwnerDashboardActivity
import com.ourorg.evchargingservice.ui.operator.OperatorHomeActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LoginActivity : AppCompatActivity() {
    private lateinit var repo: AuthRepo
    private lateinit var sessionDao: SessionDao

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        sessionDao = SessionDao(AppDb(this))
        repo = AuthRepo(sessionDao)

        // auto-route if session exists
        sessionDao.get()?.let { (_, _, role) ->
            route(role); return
        }

        val etNic = findViewById<EditText>(R.id.etNic)
        val etPwd = findViewById<EditText>(R.id.etPwd)
        findViewById<Button>(R.id.btnLogin).setOnClickListener {
            val nic = etNic.text.toString().trim()
            val pwd = etPwd.text.toString()
            if (nic.isEmpty() || pwd.isEmpty()) { toast("Enter NIC & Password"); return@setOnClickListener }
            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    repo.login(nic, pwd)
                    val role = sessionDao.get()!!.third
                    withContext(Dispatchers.Main) { route(role) }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) { toast(e.message ?: "Login failed") }
                }
            }
        }

        findViewById<Button>(R.id.btnGoRegister).setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }

    private fun route(role: String) {
        val target = if (role.equals("OPERATOR", true)) OperatorHomeActivity::class.java else OwnerDashboardActivity::class.java
        startActivity(Intent(this, target))
        finish()
    }

    private fun toast(s: String) = Toast.makeText(this, s, Toast.LENGTH_LONG).show()
}