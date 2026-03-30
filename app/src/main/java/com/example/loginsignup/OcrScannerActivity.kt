package com.example.loginsignup

import android.os.Bundle
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity

class OcrScannerActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ocr_scanner)

        val btnBack = findViewById<ImageView>(R.id.btnBackOcr)
        btnBack.setOnClickListener {
            finish()
        }
    }
}