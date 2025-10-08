package com.ourorg.evchargingservice.ui.owner

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.location.LocationServices
import com.google.android.material.card.MaterialCardView
import com.ourorg.evchargingservice.R
import com.ourorg.evchargingservice.data.local.AppDb
import com.ourorg.evchargingservice.data.local.LocationDao
import com.ourorg.evchargingservice.data.local.SessionDao
import com.ourorg.evchargingservice.data.remote.Http
import com.ourorg.evchargingservice.data.repo.BookingRepo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

class OwnerDashboardActivity : AppCompatActivity() {

    private lateinit var bookingRepo: BookingRepo
    private lateinit var locationDao: LocationDao

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { _ -> captureLocation() }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_owner_dashboard)

        val db = AppDb(this)
        bookingRepo = BookingRepo(SessionDao(db))
        locationDao = LocationDao(db)

        // Load booking counts
        loadBookingCounts()

        // Request location permissions
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED
        ) {
            permissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
        } else {
            captureLocation()
        }

        findViewById<Button>(R.id.btnMap).setOnClickListener {
            startActivity(Intent(this, StationsMapActivity::class.java))
        }

        findViewById<Button>(R.id.btnMyBookings).setOnClickListener {
            startActivity(Intent(this, MyBookingsActivity::class.java))
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun loadBookingCounts() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val sessionDao = SessionDao(AppDb(this@OwnerDashboardActivity))
                val nic = sessionDao.getUserNic() ?: throw Exception("NIC not found")

                val upcoming = bookingRepo.getByPath("/EVOwner/$nic/upcoming-bookings")
                val past = bookingRepo.getByPath("/EVOwner/$nic/past-bookings")

                // Find next closest booking (sorted by reservationDateTime)
                val nextBooking = (0 until upcoming.length())
                    .map { upcoming.getJSONObject(it) }
                    .minByOrNull { it.getString("reservationDateTime") }

                withContext(Dispatchers.Main) {
                    findViewById<TextView>(R.id.tvUpcoming).text = "${upcoming.length()}"
                    findViewById<TextView>(R.id.tvPast).text = "${past.length()}"

                    val cardNext = findViewById<MaterialCardView>(R.id.cardNextBooking)
                    if (nextBooking != null) {
                        cardNext.visibility = android.view.View.VISIBLE

                        val stationName = nextBooking.getString("stationName")
                        val reservation = nextBooking.getString("reservationDateTime")

                        // Parse ISO date-time string
                        val zonedDateTime = ZonedDateTime.parse(reservation)

                        // Format date and time separately
                        val dateStr = zonedDateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                        val timeStr = zonedDateTime.format(DateTimeFormatter.ofPattern("hh:mm a"))

                        findViewById<TextView>(R.id.tvNextStation).text = "Station: $stationName"
                        findViewById<TextView>(R.id.tvNextDate).text = "Date: $dateStr"
                        findViewById<TextView>(R.id.tvNextTime).text = "Time: $timeStr"

                    } else {
                        cardNext.visibility = android.view.View.GONE
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@OwnerDashboardActivity,
                        e.message ?: "Error loading bookings",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun captureLocation() {
        val fused = LocationServices.getFusedLocationProviderClient(this)
        try {
            fused.lastLocation.addOnSuccessListener { location ->
                location?.let { locationDao.save(it.latitude, it.longitude) }
            }
        } catch (_: SecurityException) {
            // permission denied
        }
    }

    fun getByPath(path: String): JSONArray {
        val sessionDao = SessionDao(AppDb(this))
        val token = sessionDao.getToken() ?: throw Exception("Token not found")
        return Http.request(path, "GET", null, token) as JSONArray
    }

}
