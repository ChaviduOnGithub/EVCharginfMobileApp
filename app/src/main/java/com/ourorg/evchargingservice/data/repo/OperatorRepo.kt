package com.ourorg.evchargingservice.data.repo

import com.ourorg.evchargingservice.data.local.SessionDao
import com.ourorg.evchargingservice.data.remote.Http
import org.json.JSONObject

class OperatorRepo(private val sessionDao: SessionDao) {
    private fun token() = sessionDao.get()?.second ?: throw IllegalStateException("No session")

    fun scan(payload: String) = Http.request("/operator/scan", "POST", JSONObject().put("qrPayload", payload), token())
    fun finalize(bookingId: String, kwh: Double, notes: String?) =
        Http.request("/operator/finalize", "POST", JSONObject().put("bookingId", bookingId).put("readingKWh", kwh).put("notes", notes ?: ""), token())
}