package com.ourorg.evchargingservice.ui.operator

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.ourorg.evchargingservice.R

class OperatorHomeActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_operator_home)
        findViewById<Button>(R.id.btnScan).setOnClickListener {
            startActivity(Intent(this, QrScanActivity::class.java))
        }
    }
}