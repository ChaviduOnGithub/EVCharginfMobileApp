package com.ourorg.evchargingservice.ui.owner

import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
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
import com.google.android.material.chip.Chip
import com.google.zxing.BarcodeFormat
import com.journeyapps.barcodescanner.BarcodeEncoder


class MyBookingsActivity : AppCompatActivity() {

    private lateinit var repo: BookingRepo
    private lateinit var container: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_my_bookings)

        repo = BookingRepo(SessionDao(AppDb(this)))
        container = findViewById(R.id.container)

        loadBookings()
    }

    private fun loadBookings() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val items = repo.list() // fetch all bookings

                withContext(Dispatchers.Main) {
                    container.removeAllViews()
                    if (items.isEmpty()) {
                        Toast.makeText(this@MyBookingsActivity, "No bookings found", Toast.LENGTH_SHORT).show()
                    } else {
                        items.forEach { booking ->
                            container.addView(createRow(booking))
                        }
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MyBookingsActivity, e.message ?: "Error loading bookings", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun createRow(booking: JSONObject): View {
        val row = layoutInflater.inflate(R.layout.row_booking, container, false)

        val stationName = booking.optString("stationName", booking.optString("stationId"))
        val reservationDate = booking.optString("reservationDateTime")
        val status = booking.optString("status", "Unknown").replaceFirstChar { it.uppercase() }

        row.findViewById<TextView>(R.id.tvTitle).text = "$stationName • $reservationDate"
        row.findViewById<Chip>(R.id.chipStatus).text = status

        val bookingId = booking.optString("bookingId", "")

        // Cancel button
        val btnCancel = row.findViewById<Button>(R.id.btnCancel)
        btnCancel.isEnabled = status.lowercase() != "cancelled" && bookingId.isNotEmpty()
        btnCancel.setOnClickListener {
            if (bookingId.isNotEmpty()) cancelBooking(bookingId)
            else Toast.makeText(row.context, "Cannot cancel booking: ID missing", Toast.LENGTH_SHORT).show()
        }

        // QR button
        val btnQR = row.findViewById<Button>(R.id.btnQR)
        if (bookingId.isNotEmpty()) {
            btnQR.visibility = View.VISIBLE
            btnQR.setOnClickListener {
                try {
                    val barcodeEncoder = BarcodeEncoder()
                    val bitmap: Bitmap = barcodeEncoder.encodeBitmap(
                        bookingId,
                        BarcodeFormat.QR_CODE,
                        400, 400
                    )

                    // Show in dialog
                    AlertDialog.Builder(this)
                        .setTitle("Booking QR")
                        .setView(android.widget.ImageView(this).apply { setImageBitmap(bitmap) })
                        .setPositiveButton("Close", null)
                        .show()
                } catch (e: Exception) {
                    Toast.makeText(row.context, "Error generating QR: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            btnQR.visibility = View.GONE
        }

        return row
    }
    private fun cancelBooking(id: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // Debug: log the ID being used
                Log.d("MyBookingsActivity", "Attempting to cancel booking with ID: $id")

                val response = repo.cancel(id)

                // Debug: log the raw response if any
                Log.d("MyBookingsActivity", "Cancel response: $response")

                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MyBookingsActivity, "Cancelled", Toast.LENGTH_SHORT).show()
                    loadBookings() // refresh bookings
                }
            } catch (e: Exception) {
                // Debug: log the exception
                Log.e("MyBookingsActivity", "Error cancelling booking", e)

                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@MyBookingsActivity,
                        e.message ?: "Error cancelling",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

}
