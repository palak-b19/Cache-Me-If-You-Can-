package com.example.ussdemoproject.models

data class AppInfo(
    val appName: String,
    val packageName: String,
    val permissions: List<String>? = null
)
