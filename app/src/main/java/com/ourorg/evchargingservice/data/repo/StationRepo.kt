package com.ourorg.evchargingservice.data.repo

import com.ourorg.evchargingservice.data.local.SessionDao
import com.ourorg.evchargingservice.data.remote.Http
import org.json.JSONArray

class StationRepo(private val sessionDao: SessionDao) {
    fun nearby(lat: Double, lng: Double, radiusKm: Int = 10): JSONArray {
        val (_, token, _) = sessionDao.get() ?: throw IllegalStateException("No session")
        val path = "/stations?lat=$lat&lng=$lng&radiusKm=$radiusKm"
        return Http.request(path, token = token).getJSONArray("items")
    }
    fun details(stationId: String) = Http.request("/station/$stationId",
        token = sessionDao.get()?.second ?: throw IllegalStateException("No session"))
}