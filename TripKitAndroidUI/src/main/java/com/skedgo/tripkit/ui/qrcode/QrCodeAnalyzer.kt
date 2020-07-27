package com.skedgo.tripkit.ui.qrcode

import android.annotation.SuppressLint
import android.util.Log
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.barcode.Barcode
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage


class QrCodeAnalyzer(private val onQrCodesDetected: (qrCodes: List<Barcode>) -> Unit) : ImageAnalysis.Analyzer {
    @SuppressLint("UnsafeExperimentalUsageError")
    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
            val options = BarcodeScannerOptions.Builder()
                    .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
                    .build()

            val scanner = BarcodeScanning.getClient(options)
            scanner.process(image)
                    .addOnSuccessListener { barcodes ->
                        imageProxy.close()
                        if (barcodes.size > 0) {
                            onQrCodesDetected(barcodes)
                        }
                    }
                    .addOnFailureListener {
                        Log.e("TripKit", "Error scanning barcodes", it)
                        imageProxy.close()
                    }
        }
    }

}