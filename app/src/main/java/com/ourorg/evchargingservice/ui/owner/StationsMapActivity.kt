package com.ourorg.evchargingservice.ui.owner

import android.Manifest
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.ourorg.evchargingservice.R
import com.ourorg.evchargingservice.data.local.AppDb
import com.ourorg.evchargingservice.data.local.LocationDao
import com.ourorg.evchargingservice.data.local.SessionDao
import com.ourorg.evchargingservice.data.repo.StationRepo
import com.ourorg.evchargingservice.data.remote.Http
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.osmdroid.config.Configuration
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import java.text.SimpleDateFormat
import java.util.*

class StationsMapActivity : AppCompatActivity() {

    private lateinit var map: MapView
    private lateinit var locationDao: LocationDao
    private lateinit var sessionDao: SessionDao
    private val TAG = "StationsMapActivity"

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { granted ->
        if (granted.all { it.value }) showMyLocation()
        else Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Configuration.getInstance().load(applicationContext, getSharedPreferences("osmdroid", MODE_PRIVATE))
        setContentView(R.layout.activity_stations_map)

        map = findViewById(R.id.map)
        map.setMultiTouchControls(true)

        val db = AppDb(this)
        locationDao = LocationDao(db)
        sessionDao = SessionDao(db)
        val repo = StationRepo(sessionDao)

        val savedLoc = locationDao.get() ?: (6.9271 to 79.8612) // Default to Colombo
        map.controller.setZoom(13.0)
        map.controller.setCenter(GeoPoint(savedLoc.first, savedLoc.second))

        // Show user's location marker
        showMyLocation()

        // Load nearby stations
        loadStations(repo, savedLoc.first, savedLoc.second)

        // Request location permissions if not granted
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED
        ) {
            permissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
        }
    }

    /** Show current user location on the map **/
    private fun showMyLocation() {
        locationDao.get()?.let {
            val myLoc = GeoPoint(it.first, it.second)
            val marker = Marker(map).apply {
                position = myLoc
                title = "You are here"
                icon = ContextCompat.getDrawable(this@StationsMapActivity, R.drawable.ic_my_location_marker)
            }
            map.overlays.add(marker)
            map.controller.setCenter(myLoc)
        }
    }

    /** Load EV charging stations and add markers **/
    @RequiresApi(Build.VERSION_CODES.O)
    private fun loadStations(repo: StationRepo, lat: Double, lng: Double) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val stationsArray = repo.nearby(lat, lng, 10)
                Log.d(TAG, "Loaded ${stationsArray.length()} stations")

                withContext(Dispatchers.Main) {
                    val myLocationMarker = map.overlays.find { it is Marker && (it as Marker).title == "You are here" } as Marker?
                    map.overlays.clear()
                    myLocationMarker?.let { map.overlays.add(it) }

                    for (i in 0 until stationsArray.length()) {
                        val station = stationsArray.getJSONObject(i)
                        Log.d(TAG, "Station[$i]: $station")
                        val latStation = station.optDouble("lat", Double.NaN)
                        val lngStation = station.optDouble("lng", Double.NaN)
                        val name = station.optString("StationName", station.optString("name", "Unknown"))
                        val id = station.optString("_id", station.optString("id", ""))

                        if (!latStation.isNaN() && !lngStation.isNaN()) {
                            val pos = GeoPoint(latStation, lngStation)
                            val marker = Marker(map).apply {
                                position = pos
                                title = name
                                snippet = id
                                relatedObject = station
                                icon = ContextCompat.getDrawable(this@StationsMapActivity, R.drawable.ic_ev_station_marker)
                                setOnMarkerClickListener { marker, _ ->
                                    val clickedStation = marker.relatedObject as? JSONObject
                                    clickedStation?.let { openBookingForm(it) }
                                        ?: Toast.makeText(this@StationsMapActivity, "Station data unavailable", Toast.LENGTH_SHORT).show()
                                    true
                                }
                            }
                            map.overlays.add(marker)
                        }
                    }

                    myLocationMarker?.let { map.controller.setCenter(it.position) }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading stations", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@StationsMapActivity, "Failed to load stations", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    /** Show booking dialog for a selected station **/
    @RequiresApi(Build.VERSION_CODES.O)
    private fun openBookingForm(station: JSONObject) {
        val stationName = station.optString("StationName", station.optString("name", "Unknown"))
        val view = layoutInflater.inflate(R.layout.dialog_booking, null)
        val editReservationDateTime = view.findViewById<EditText>(R.id.editReservationDateTime)
        val editDetails = view.findViewById<EditText>(R.id.editDetails)

        editReservationDateTime.setOnClickListener { showDateTimePicker(editReservationDateTime) }

        android.app.AlertDialog.Builder(this)
            .setTitle("Create Booking for $stationName")
            .setView(view)
            .setPositiveButton("Book") { dialog, _ ->
                val reservationDateTime = editReservationDateTime.text.toString()
                if (reservationDateTime.isBlank()) {
                    Toast.makeText(this, "Please select a date & time", Toast.LENGTH_SHORT).show()
                } else {
                    createBooking(station, reservationDateTime)
                }
                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }
            .show()
    }

    /** Date & Time Picker **/
    private fun showDateTimePicker(target: EditText) {
        val cal = Calendar.getInstance()
        DatePickerDialog(this, { _, year, month, dayOfMonth ->
            TimePickerDialog(this, { _, hourOfDay, minute ->
                cal.set(year, month, dayOfMonth, hourOfDay, minute)
                val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault())
                target.setText(sdf.format(cal.time))
            }, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), true).show()
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
    }

    /** Create booking via API **/
    @RequiresApi(Build.VERSION_CODES.O)
    private fun createBooking(station: JSONObject, reservationDateTime: String) {
        val ownerNic = sessionDao.getUserNic()
        if (ownerNic.isNullOrBlank()) {
            Log.e(TAG, "No user session found")
            Toast.makeText(this, "No user session found", Toast.LENGTH_SHORT).show()
            return
        }

        val stationId = station.optString("_id", station.optString("id", ""))
        if (stationId.isBlank()) {
            Log.e(TAG, "Station ID missing, cannot create booking: $station")
            Toast.makeText(this, "Station ID missing, cannot create booking", Toast.LENGTH_SHORT).show()
            return
        }

        val stationName = station.optString("StationName", station.optString("name", ""))

        val payload = JSONObject().apply {
            put("OwnerNIC", ownerNic)
            put("StationId", stationId)
            put("StationName", stationName)
            put("ReservationDateTime", reservationDateTime)
            put("CreatedAt", java.time.Instant.now().toString())
            put("Status", "Pending")
        }

        Log.d(TAG, "Booking Payload: $payload")

        lifecycleScope.launch {
            try {
                val token = sessionDao.getToken() ?: throw IllegalStateException("No token")

                // Run network request on background thread
                withContext(Dispatchers.IO) {
                    Http.request("/booking", "POST", payload, token)
                }

                // Toast on Main thread
                Toast.makeText(this@StationsMapActivity, "Booking created!", Toast.LENGTH_SHORT).show()

            } catch (ex: Exception) {
                Log.e(TAG, "Error creating booking", ex)
                Toast.makeText(
                    this@StationsMapActivity,
                    "Error creating booking: ${ex.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

}
