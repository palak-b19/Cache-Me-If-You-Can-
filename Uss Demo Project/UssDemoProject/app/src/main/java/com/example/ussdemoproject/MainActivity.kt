package com.example.ussdemoproject

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val btnOpenAppList = findViewById<Button>(R.id.btnOpenAppList)

        btnOpenAppList.setOnClickListener {
            startActivity(Intent(this, AppListActivity::class.java))
        }
    }
}
