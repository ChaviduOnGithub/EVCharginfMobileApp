package com.ourorg.evchargingservice.data.repo

import com.ourorg.evchargingservice.data.local.SessionDao
import com.ourorg.evchargingservice.data.remote.Http
import org.json.JSONObject

class AuthRepo(private val sessionDao: SessionDao) {
    fun login(nic: String, password: String) {
        val res = Http.request(
            "/evowner/auth/login",
            "POST",
            JSONObject().put("NIC", nic).put("Password", password)
        ) as JSONObject  // <-- cast here
        val token = res.getString("token")
        val role = res.getString("role")
        sessionDao.save(nic, token, role)
    }

    fun register(nic: String, name: String, phone: String, email: String, password: String) {
        Http.request(
            "/evowner/auth/register",
            "POST",
            JSONObject()
                .put("NIC", nic)
                .put("Name", name)
                .put("Email", email)
                .put("Phone", phone)
                .put("Password", password)
        )
    }
}