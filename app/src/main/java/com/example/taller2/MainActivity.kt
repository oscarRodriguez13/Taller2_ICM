package com.example.taller2

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.ImageButton

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val contactsButton = findViewById<ImageButton>(R.id.contactsButton)
        val cameraButton = findViewById<ImageButton>(R.id.cameraButton)
        val mapaButton = findViewById<ImageButton>(R.id.mapaButton)

        contactsButton.setOnClickListener {
            val intent = Intent(
                this,
                ContactosActivity::class.java
            )
            startActivity(intent)
        }

        cameraButton.setOnClickListener {
            val intent = Intent(
                this,
                CamaraActivity::class.java
            )
            startActivity(intent)
        }
    }
}