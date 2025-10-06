package com.ourorg.evchargingservice.data.remote

import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object Http {
    // TODO: point to your real API base
    private const val BASE = "https://boltevcharging-c6f0g0dme0bea2gg.eastasia-01.azurewebsites.net"

    @Throws(RuntimeException::class)
    fun request(
        path: String,
        method: String = "GET",
        body: JSONObject? = null,
        token: String? = null
    ): JSONObject {
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
        return if (text.isEmpty()) JSONObject() else JSONObject(text)
    }
}