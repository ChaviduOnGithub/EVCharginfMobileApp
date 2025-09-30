package com.ourorg.evchargingservice.data.repo

import com.ourorg.evchargingservice.data.local.SessionDao
import com.ourorg.evchargingservice.data.remote.Http
import org.json.JSONObject

class AuthRepo(private val sessionDao: SessionDao) {
    fun login(nic: String, password: String) {
        val res = Http.request("/auth/login", "POST", JSONObject().put("nic", nic).put("password", password))
        val token = res.getString("token")
        val role = res.getString("userRole")
        sessionDao.save(nic, token, role)
    }
    fun register(nic: String, name: String, phone: String, email: String, password: String) {
        Http.request("/auth/register", "POST", JSONObject()
            .put("nic", nic).put("name", name).put("phone", phone).put("email", email).put("password", password))
    }
}