package com.example.ussdemoproject.models

import android.graphics.drawable.Drawable
import com.example.ussdemoproject.ai.RiskLevel

data class AppInfo(
    val appName: String,
    val packageName: String,
    val permissions: List<String>? = null,
    var riskScore: Int? = null,
    var riskLevel: RiskLevel? = null,
    var summary: String? = null,
    var generatedAt: Long? = null
)
