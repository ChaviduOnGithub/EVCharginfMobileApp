package com.ourorg.evchargingservice.ui.owner

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import com.ourorg.evchargingservice.R
import com.ourorg.evchargingservice.data.local.AppDb
import com.ourorg.evchargingservice.data.local.SessionDao
import com.ourorg.evchargingservice.data.repo.BookingRepo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class QrDisplayActivity : AppCompatActivity() {
    companion object {
        private const val EXTRA_ID = "id"
        fun intent(ctx: Context, bookingId: String) = Intent(ctx, QrDisplayActivity::class.java).putExtra(EXTRA_ID, bookingId)
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_qr_display)
        val id = intent.getStringExtra(EXTRA_ID) ?: return
        val repo = BookingRepo(SessionDao(AppDb(this)))
        val img = findViewById<ImageView>(R.id.imgQr)

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val payload = repo.qrPayload(id).getString("payload")
                val bmp = toQrBitmap(payload, 800)
                withContext(Dispatchers.Main) { img.setImageBitmap(bmp) }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { Toast.makeText(this@QrDisplayActivity, e.message, Toast.LENGTH_LONG).show() }
            }
        }
    }

    private fun toQrBitmap(text: String, size: Int): Bitmap {
        val matrix = QRCodeWriter().encode(text, BarcodeFormat.QR_CODE, size, size)
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.RGB_565)
        for (x in 0 until size) for (y in 0 until size)
            bmp.setPixel(x, y, if (matrix.get(x, y)) 0xFF000000.toInt() else 0xFFFFFFFF.toInt())
        return bmp
    }
}