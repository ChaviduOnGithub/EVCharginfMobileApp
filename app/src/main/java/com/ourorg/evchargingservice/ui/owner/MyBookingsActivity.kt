package com.ourorg.evchargingservice.ui.owner

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.ourorg.evchargingservice.R
import com.ourorg.evchargingservice.data.local.AppDb
import com.ourorg.evchargingservice.data.local.SessionDao
import com.ourorg.evchargingservice.data.repo.BookingRepo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject

class MyBookingsActivity : AppCompatActivity() {
    private lateinit var repo: BookingRepo
    private lateinit var container: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_my_bookings)
        repo = BookingRepo(SessionDao(AppDb(this)))
        container = findViewById(R.id.container)
        load("approved")
    }

    private fun load(status: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val items = repo.list(status).getJSONArray("items")
                withContext(Dispatchers.Main) {
                    container.removeAllViews()
                    for (i in 0 until items.length()) {
                        val b: JSONObject = items.getJSONObject(i)
                        container.addView(row(b))
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { Toast.makeText(this@MyBookingsActivity, e.message, Toast.LENGTH_LONG).show() }
            }
        }
    }

    private fun row(b: JSONObject): View {
        val v = layoutInflater.inflate(R.layout.row_booking, container, false)
        v.findViewById<TextView>(R.id.tvTitle).text = "${b.getString("stationName")} • ${b.getString("startTime")}"
        v.findViewById<Button>(R.id.btnQR).setOnClickListener { showQr(b.getString("id")) }
        v.findViewById<Button>(R.id.btnCancel).setOnClickListener { cancel(b.getString("id")) }
        return v
    }

    private fun showQr(id: String) {
        startActivity(QrDisplayActivity.intent(this, id))
    }

    private fun cancel(id: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            try { repo.cancel(id); withContext(Dispatchers.Main) { Toast.makeText(this@MyBookingsActivity, "Cancelled", Toast.LENGTH_SHORT).show(); load("approved") } }
            catch (e: Exception) { withContext(Dispatchers.Main) { Toast.makeText(this@MyBookingsActivity, e.message, Toast.LENGTH_LONG).show() } }
        }
    }
}