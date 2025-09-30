package com.ourorg.evchargingservice.data.repo

import com.ourorg.evchargingservice.data.local.SessionDao
import com.ourorg.evchargingservice.data.remote.Http
import org.json.JSONObject

class BookingRepo(private val sessionDao: SessionDao) {
    private fun token() = sessionDao.get()?.second ?: throw IllegalStateException("No session")
    private fun nic() = sessionDao.get()?.first ?: throw IllegalStateException("No session")

    fun create(stationId: String, startIso: String) =
        Http.request("/bookings", "POST", JSONObject().put("nic", nic()).put("stationId", stationId).put("startTime", startIso), token())

    fun update(id: String, startIso: String) =
        Http.request("/bookings/$id", "PUT", JSONObject().put("startTime", startIso), token())

    fun cancel(id: String) = Http.request("/bookings/$id", "DELETE", null, token())

    fun list(status: String) = Http.request("/bookings?nic=${nic()}&status=$status", token = token())

    fun qrPayload(id: String) = Http.request("/bookings/$id/qr", token = token())
}