package com.ourorg.evchargingservice.data.repo

import android.util.Log
import com.ourorg.evchargingservice.data.local.SessionDao
import com.ourorg.evchargingservice.data.remote.Http
import org.json.JSONArray
import org.json.JSONObject

class BookingRepo(private val sessionDao: SessionDao) {

    private fun token() = sessionDao.get()?.second ?: throw IllegalStateException("No session")
    private fun nic() = sessionDao.get()?.first ?: throw IllegalStateException("No session")

    // Create booking
    fun create(stationId: String, startIso: String) =
        Http.request(
            "/api/booking",
            "POST",
            JSONObject()
                .put("OwnerNIC", nic())
                .put("StationId", stationId)
                .put("ReservationDateTime", startIso),
            token()
        )

    // Update booking
    fun update(id: String, startIso: String) =
        Http.request(
            "/api/booking/$id",
            "PUT",
            JSONObject().put("ReservationDateTime", startIso),
            token()
        )

    // Cancel booking
    fun cancel(id: String): Any {
        val path = "/Booking/$id/cancel"

        // Debug statements
        Log.d("BookingRepo", "Booking ID being cancelled: $id")
        Log.d("BookingRepo", "Full path being requested: $path")
        Log.d("BookingRepo", "Calling cancel API")

        // Use an empty JSON object as the body
        return Http.request(path, "PUT", JSONObject(), token())
    }


    // Get bookings for owner
    fun list(): List<JSONObject> {
        Log.d("BookingRepo", "Fetching bookings for NIC: ${nic()}")
        val res = Http.request("/Booking/owner/${nic()}", token = token()) // backend route
        Log.d("BookingRepo", "Raw response: $res")

        val jsonArray = when (res) {
            is JSONArray -> res
            is JSONObject -> JSONArray().apply { put(res) } // wrap single object
            else -> JSONArray() // fallback empty
        }

        Log.d("BookingRepo", "Parsed JSONArray length: ${jsonArray.length()}")

        val list = (0 until jsonArray.length()).map {
            val obj = jsonArray.getJSONObject(it)
            Log.d("BookingRepo", "Booking[$it]: $obj")
            obj
        }

        return list
    }

    // Get QR payload for booking
    fun qrPayload(id: String): JSONObject {
        val res = Http.request("/some/path/$id", "GET", null, token())
        return res as? JSONObject
            ?: throw IllegalStateException("Expected JSONObject but got ${res::class.java.simpleName}")
    }

    fun getByPath(path: String): JSONArray {
        return Http.request(path, "GET", null, token()) as JSONArray
    }



}

