package com.ourorg.evchargingservice.ui.owner

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.location.LocationServices
import com.ourorg.evchargingservice.R
import com.ourorg.evchargingservice.data.local.AppDb
import com.ourorg.evchargingservice.data.local.LocationDao
import com.ourorg.evchargingservice.data.local.SessionDao
import com.ourorg.evchargingservice.data.repo.BookingRepo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class OwnerDashboardActivity : AppCompatActivity() {
    private lateinit var bookingRepo: BookingRepo
    private lateinit var locationDao: LocationDao

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { _ -> captureLocation() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_owner_dashboard)

        val db = AppDb(this)
        bookingRepo = BookingRepo(SessionDao(db))
        locationDao = LocationDao(db)

        // counts
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val pending = bookingRepo.list("pending").getJSONArray("items").length()
                val approved = bookingRepo.list("approved").getJSONArray("items").length()
                withContext(Dispatchers.Main) {
                    findViewById<TextView>(R.id.tvPending).text = "Pending: $pending"
                    findViewById<TextView>(R.id.tvApproved).text = "Approved: $approved"
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { Toast.makeText(this@OwnerDashboardActivity, e.message, Toast.LENGTH_LONG).show() }
            }
        }

        // request location (thin: store only)
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            permissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
        } else captureLocation()

        findViewById<Button>(R.id.btnMap).setOnClickListener {
            startActivity(Intent(this, StationsMapActivity::class.java))
        }
        findViewById<Button>(R.id.btnMyBookings).setOnClickListener {
            startActivity(Intent(this, MyBookingsActivity::class.java))
        }
    }

    private fun captureLocation() {
        val fused = LocationServices.getFusedLocationProviderClient(this)
        try {
            fused.lastLocation.addOnSuccessListener { l ->
                if (l != null) locationDao.save(l.latitude, l.longitude)
            }
        } catch (_: SecurityException) {}
    }
}