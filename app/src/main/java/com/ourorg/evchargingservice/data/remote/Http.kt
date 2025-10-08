package com.ourorg.evchargingservice.data.remote

import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object Http {
    private const val BASE = "http://172.20.10.3:5009/api"

    @Throws(RuntimeException::class, JSONException::class)
    fun request(
        path: String,
        method: String = "GET",
        body: JSONObject? = null,
        token: String? = null
    ): Any { // Returns JSONObject or JSONArray
        val url = URL("$BASE$path")
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = method
            setRequestProperty("Accept", "application/json")
            setRequestProperty("Content-Type", "application/json")
            if (token != null) setRequestProperty("Authorization", "Bearer $token")
            doInput = true
            if (method != "GET" && body != null) {
                doOutput = true
                outputStream.bufferedWriter().use { it.write(body.toString()) }
            }
        }

        val code = conn.responseCode
        val stream = if (code in 200..299) conn.inputStream else conn.errorStream
        val text = stream.bufferedReader().use { it.readText() }

        if (code !in 200..299) throw RuntimeException("HTTP $code: $text")

        val trimmed = text.trim()
        return when {
            trimmed.isEmpty() -> JSONObject()
            trimmed.startsWith("{") -> JSONObject(trimmed)
            trimmed.startsWith("[") -> JSONArray(trimmed)
            else -> throw JSONException("Unknown JSON format")
        }
    }
}
