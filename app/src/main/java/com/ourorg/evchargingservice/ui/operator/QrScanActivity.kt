package com.ourorg.evchargingservice.ui.operator

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.google.common.util.concurrent.ListenableFuture
import com.google.zxing.*
import com.google.zxing.common.HybridBinarizer
import com.ourorg.evchargingservice.R
import com.ourorg.evchargingservice.data.local.AppDb
import com.ourorg.evchargingservice.data.local.SessionDao
import com.ourorg.evchargingservice.data.repo.OperatorRepo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.nio.ByteBuffer
import java.util.concurrent.Executors

class QrScanActivity : AppCompatActivity() {
    private lateinit var repo: OperatorRepo
    private lateinit var preview: PreviewView
    private val exec = Executors.newSingleThreadExecutor()
    private var handled = false

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> if (granted) startCamera() else finish() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_qr_scan)
        repo = OperatorRepo(SessionDao(AppDb(this)))
        preview = findViewById(R.id.previewView)

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            startCamera()
        } else permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    private fun startCamera() {
        val provider: ListenableFuture<ProcessCameraProvider> = ProcessCameraProvider.getInstance(this)
        provider.addListener({
            val cameraProvider = provider.get()
            val previewUseCase = androidx.camera.core.Preview.Builder().build().also {
                it.setSurfaceProvider(preview.surfaceProvider)
            }
            val analysis = ImageAnalysis.Builder().build().apply {
                setAnalyzer(exec) { img -> decode(img) }
            }
            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(this, CameraSelector.DEFAULT_BACK_CAMERA, previewUseCase, analysis)
        }, ContextCompat.getMainExecutor(this))
    }

    private fun decode(image: ImageProxy) {
        if (handled) { image.close(); return }
        val buffer: ByteBuffer = image.planes[0].buffer
        val bytes = ByteArray(buffer.remaining()); buffer.get(bytes)
        val width = image.width; val height = image.height

        val source = PlanarYUVLuminanceSource(bytes, width, height, 0, 0, width, height, false)
        val bitmap = BinaryBitmap(HybridBinarizer(source))
        try {
            val result = MultiFormatReader().decode(bitmap)
            handled = true
            image.close()
            onQr(result.text)
        } catch (_: NotFoundException) {
            image.close()
        }
    }

    private fun onQr(text: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val res = repo.scan(text)
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@QrScanActivity, "Booking: ${res.getString("bookingId")}", Toast.LENGTH_LONG).show()
                    // you can navigate to a detail/confirm screen here
                    finish()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@QrScanActivity, e.message, Toast.LENGTH_LONG).show()
                    finish()
                }
            }
        }
    }
}