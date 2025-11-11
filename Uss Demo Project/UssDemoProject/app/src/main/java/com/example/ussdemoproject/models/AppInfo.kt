package com.example.ussdemoproject.models

import android.graphics.drawable.Drawable

data class AppInfo(
    val appName: String,
    val packageName: String,
    val icon: Drawable,
    val permissions: List<String>?
)
