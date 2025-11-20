package com.example.ussdemoproject.ai

import androidx.annotation.ColorRes
import androidx.annotation.StringRes
import com.example.ussdemoproject.R

enum class RiskLevel(
    @StringRes val labelRes: Int,
    @ColorRes val colorRes: Int
) {
    UNKNOWN(R.string.risk_level_unknown, R.color.risk_unknown),
    LOW(R.string.risk_level_low, R.color.risk_low),
    MEDIUM(R.string.risk_level_medium, R.color.risk_medium),
    HIGH(R.string.risk_level_high, R.color.risk_high)
}

enum class InsightSource {
    LLM,
    HEURISTIC
}

data class PermissionInsight(
    val packageName: String,
    val appName: String,
    val summary: String,
    val riskScore: Int,
    val riskLevel: RiskLevel,
    val rationale: List<String>,
    val confidencePercent: Int,
    val generatedAt: Long,
    val source: InsightSource,
    val llmUnavailableReason: String? = null,
    val fromCache: Boolean = false
)

sealed class PermissionInsightResult {
    data class Success(val insight: PermissionInsight) : PermissionInsightResult()
    data class Unavailable(val reason: String) : PermissionInsightResult()
}
