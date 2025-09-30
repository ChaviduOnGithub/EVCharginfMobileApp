package com.ourorg.evchargingservice.data.local

import android.content.ContentValues

class LocationDao(private val db: AppDb) {
    fun save(lat: Double, lng: Double) {
        val w = db.writableDatabase
        w.delete("user_location", null, null)
        val v = ContentValues().apply {
            put("id", 1); put("lat", lat); put("lng", lng); put("captured_at", System.currentTimeMillis()/1000)
        }
        w.insertOrThrow("user_location", null, v)
    }
    fun get(): Pair<Double,Double>? {
        db.readableDatabase.rawQuery("SELECT lat,lng FROM user_location WHERE id=1", null).use {
            return if (it.moveToFirst()) it.getDouble(0) to it.getDouble(1) else null
        }
    }
}