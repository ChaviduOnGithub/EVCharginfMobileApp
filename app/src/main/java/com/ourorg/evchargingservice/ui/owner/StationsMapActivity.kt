package com.ourorg.evchargingservice.ui.owner

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.ourorg.evchargingservice.R
import com.ourorg.evchargingservice.data.local.AppDb
import com.ourorg.evchargingservice.data.local.LocationDao
import com.ourorg.evchargingservice.data.local.SessionDao
import com.ourorg.evchargingservice.data.repo.StationRepo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject

class StationsMapActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_stations_map)

        val db = AppDb(this)
        val loc = LocationDao(db).get() ?: (6.9271 to 79.8612) // default Colombo
        val repo = StationRepo(SessionDao(db))

        val mapFrag = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFrag.getMapAsync { map ->
            val me = LatLng(loc.first, loc.second)
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(me, 13f))

            lifecycleScope.launch(Dispatchers.IO) {
                val stations = repo.nearby(loc.first, loc.second, 10)
                withContext(Dispatchers.Main) {
                    for (i in 0 until stations.length()) {
                        val s: JSONObject = stations.getJSONObject(i)
                        val p = LatLng(s.getDouble("lat"), s.getDouble("lng"))
                        map.addMarker(MarkerOptions().position(p).title(s.getString("name")))
                    }
                }
            }
        }
    }
}