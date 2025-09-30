package com.ourorg.evchargingservice.data.local

import android.content.ContentValues

class SessionDao(private val db: AppDb) {
    fun save(nic: String, token: String, role: String) {
        val w = db.writableDatabase
        w.delete("session", null, null)
        val v = ContentValues().apply {
            put("id", 1); put("nic", nic); put("token", token); put("role", role)
            put("logged_at", System.currentTimeMillis()/1000)
        }
        w.insertOrThrow("session", null, v)
    }
    fun get(): Triple<String,String,String>? {
        db.readableDatabase.rawQuery("SELECT nic,token,role FROM session WHERE id=1", null).use {
            return if (it.moveToFirst()) Triple(it.getString(0), it.getString(1), it.getString(2)) else null
        }
    }
    fun clear() { db.writableDatabase.delete("session", null, null) }
}