package com.ourorg.evchargingservice.data.repo

import com.ourorg.evchargingservice.data.local.SessionDao
import com.ourorg.evchargingservice.data.remote.Http
import org.json.JSONArray
import org.json.JSONObject

class StationRepo(private val sessionDao: SessionDao) {

    fun nearby(lat: Double, lng: Double, radiusKm: Int = 10): JSONArray {
        val session = sessionDao.get() ?: throw IllegalStateException("No session")
        val token = session.second // token is the second element in the Triple

        val path = "/station/nearby?lat=$lat&lng=$lng&radiusKm=$radiusKm"
        val res = Http.request(path, token = token) as JSONObject
        return res.getJSONArray("items")
    }

    fun details(stationId: String): JSONObject {
        val session = sessionDao.get() ?: throw IllegalStateException("No session")
        val token = session.second
        return Http.request("/station/$stationId", token = token) as JSONObject
    }
}
