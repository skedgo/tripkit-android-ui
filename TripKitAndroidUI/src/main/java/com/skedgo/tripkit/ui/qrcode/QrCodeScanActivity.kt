package com.skedgo.tripkit.ui.qrcode

import android.content.Intent
import android.os.Bundle
import android.os.PersistableBundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import com.araujo.jordan.excuseme.ExcuseMe
import com.skedgo.tripkit.ui.R
import kotlinx.android.synthetic.main.qr_scan_activity.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

const val INTENT_KEY_BARCODES = "barcodes"
const val INTENT_KEY_INTERNAL_URL = "internal_url"
const val INTENT_KEY_BUTTON_ID = "button_id"
class QrCodeScanActivity : AppCompatActivity() {
    private lateinit var cameraExecutor: ExecutorService
    private var preview: Preview? = null
    private var imageCapture: ImageCapture? = null
    private var imageAnalyzer: ImageAnalysis? = null
    private var camera: Camera? = null
    private var internalUrl = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.qr_scan_activity)
        cameraExecutor = Executors.newSingleThreadExecutor()
        internalUrl = intent.getStringExtra(INTENT_KEY_INTERNAL_URL) ?: ""
    }

    override fun onStart() {
        super.onStart()
        ExcuseMe.couldYouGive(this).permissionFor(android.Manifest.permission.CAMERA) {
            if(it.granted.contains(android.Manifest.permission.CAMERA)) {
                viewFinder.post { runCamera() }
            }
        }
    }

    fun runCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener(Runnable {
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()
            preview = Preview.Builder()
                    .build()

            val cameraSelector = CameraSelector.Builder().requireLensFacing(CameraSelector.LENS_FACING_BACK).build()
            imageAnalyzer = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                    // The analyzer can then be assigned to the instance
                    .also {
                        it.setAnalyzer(cameraExecutor, QrCodeAnalyzer { list ->
                            val results = mutableListOf<String>()
                            list.forEach {
                                it.rawValue?.let { results.add(it) }
                            }
                            done(results.toTypedArray())

                        })
                    }

            try {
                cameraProvider.unbindAll()
                camera = cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalyzer)
                preview?.setSurfaceProvider(viewFinder.createSurfaceProvider())
            } catch(exc: Exception) {
                Log.e("TripKit","Use case binding failed", exc)
            }

        }, ContextCompat.getMainExecutor(this))

    }

    fun done(results: Array<String>) {
        val result = Intent()
        result.putExtra(INTENT_KEY_BARCODES, results)
        result.putExtra(INTENT_KEY_INTERNAL_URL, internalUrl)
        result.putExtra(INTENT_KEY_BUTTON_ID, intent.getIntExtra(INTENT_KEY_BUTTON_ID, -1))
        setResult(RESULT_OK, result)
        finish()
    }
}