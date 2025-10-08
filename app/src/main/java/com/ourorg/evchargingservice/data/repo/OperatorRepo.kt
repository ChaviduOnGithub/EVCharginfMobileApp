package com.ourorg.evchargingservice.data.repo

import com.ourorg.evchargingservice.data.local.SessionDao
import com.ourorg.evchargingservice.data.remote.Http
import org.json.JSONObject

class OperatorRepo(private val sessionDao: SessionDao) {

    private fun token(): String =
        sessionDao.get()?.second ?: throw IllegalStateException("No session")

    // scan now explicitly returns JSONObject
    fun scan(payload: String): JSONObject {
        val res = Http.request(
            "/operator/scan",
            "POST",
            JSONObject().put("qrPayload", payload),
            token()
        )

        return when (res) {
            is JSONObject -> res
            else -> throw IllegalStateException("Expected JSONObject but got ${res::class.java.simpleName}")
        }
    }

    // finalize also returns JSONObject
    fun finalize(bookingId: String, kwh: Double, notes: String?): JSONObject {
        val res = Http.request(
            "/operator/finalize",
            "POST",
            JSONObject()
                .put("bookingId", bookingId)
                .put("readingKWh", kwh)
                .put("notes", notes ?: ""),
            token()
        )

        return when (res) {
            is JSONObject -> res
            else -> throw IllegalStateException("Expected JSONObject but got ${res::class.java.simpleName}")
        }
    }
}
