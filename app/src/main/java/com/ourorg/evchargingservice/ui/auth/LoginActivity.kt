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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import retrofit2.HttpException
import java.io.IOException

class LoginActivity : AppCompatActivity() {

    private lateinit var repo: AuthRepo
    private lateinit var sessionDao: SessionDao

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        sessionDao = SessionDao(AppDb(this))
        repo = AuthRepo(sessionDao)

        val etNic = findViewById<EditText>(R.id.etNic)
        val etPwd = findViewById<EditText>(R.id.etPwd)
        val btnLogin = findViewById<Button>(R.id.btnLogin)
        val btnGoRegister = findViewById<Button>(R.id.btnGoRegister)

        btnLogin.setOnClickListener {
            val nic = etNic.text.toString().trim()
            val pwd = etPwd.text.toString()

            if (nic.isEmpty() || pwd.isEmpty()) {
                toast("Please enter both NIC and Password")
                return@setOnClickListener
            }

            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    val response = repo.login(nic, pwd)
                    withContext(Dispatchers.Main) {
                        toast("Login successful!")
                        routeToOwnerDashboard()
                    }

                } catch (e: HttpException) {
                    val errorMsg = parseHttpError(e)
                    withContext(Dispatchers.Main) {
                        toast(errorMsg)
                    }

                } catch (e: IOException) {
                    withContext(Dispatchers.Main) {
                        toast("Network error! Please check your internet connection.")
                    }

                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        toast("Unexpected error: ${e.message}")
                    }
                }
            }
        }

        btnGoRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }

    private fun parseHttpError(e: HttpException): String {
        return try {
            val errorBody = e.response()?.errorBody()?.string()
            val message = JSONObject(errorBody ?: "{}").optString("message")
            message.ifEmpty { "Invalid NIC or Password" }
        } catch (ex: Exception) {
            "Login failed, please try again."
        }
    }

    private fun routeToOwnerDashboard() {
        val intent = Intent(this, OwnerDashboardActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun toast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
    }
}
