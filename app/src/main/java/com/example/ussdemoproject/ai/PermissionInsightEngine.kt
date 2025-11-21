package com.example.ussdemoproject.ai

import android.content.Context
import android.util.LruCache
import com.example.ussdemoproject.R
import com.example.ussdemoproject.ai.llm.TinyLlamaInsightClient
import java.util.Locale
import java.util.concurrent.TimeUnit

private const val CACHE_SIZE = 64
private val CACHE_TTL_MILLIS = TimeUnit.HOURS.toMillis(6)

class PermissionInsightEngine(context: Context) {

    private val appContext = context.applicationContext
    private val cache = object : LruCache<String, PermissionInsight>(CACHE_SIZE) {}
    private val tinyLlamaClient = TinyLlamaInsightClient(appContext)

    fun analyze(
        appName: String,
        packageName: String,
        permissions: List<String>,
        forceHeuristic: Boolean = false
    ): PermissionInsightResult {
        if (permissions.isEmpty()) {
            return PermissionInsightResult.Unavailable(
                appContext.getString(R.string.insight_reason_no_permissions)
            )
        }

        if (!forceHeuristic) {
            cache.get(packageName)?.takeIf { insight ->
                System.currentTimeMillis() - insight.generatedAt <= CACHE_TTL_MILLIS
            }?.let { cached ->
                return PermissionInsightResult.Success(cached.copy(fromCache = true))
            }

            val llmInsight = runTinyLlamaInsight(appName, packageName, permissions)
            if (llmInsight != null) {
                // Check for generic/hallucinated rationale from the prompt example
                val isGenericResponse = llmInsight.rationale.any { reason ->
                    reason.contains("Short reason", ignoreCase = true)
                }

                if (!isGenericResponse) {
                    cache.put(packageName, llmInsight)
                    return PermissionInsightResult.Success(llmInsight)
                }
            }
        }

        val insight = runHeuristicModel(
            appName = appName,
            packageName = packageName,
            permissions = permissions,
            llmIssue = if (forceHeuristic) "Skipped by user" else {
                tinyLlamaClient.lastKnownIssue() ?: "LLM returned generic response"
            }
        )
        cache.put(packageName, insight)
        return PermissionInsightResult.Success(insight)
    }

    private fun runTinyLlamaInsight(
        appName: String,
        packageName: String,
        permissions: List<String>
    ): PermissionInsight? {
        val payload = tinyLlamaClient.generateInsight(appName, packageName, permissions) ?: return null

        val riskLevel = when {
            payload.riskScore >= 70 -> RiskLevel.HIGH
            payload.riskScore >= 45 -> RiskLevel.MEDIUM
            else -> RiskLevel.LOW
        }

        return PermissionInsight(
            packageName = packageName,
            appName = appName,
            summary = payload.summary,
            riskScore = payload.riskScore.coerceIn(0, 100),
            riskLevel = riskLevel,
            rationale = payload.rationale.ifEmpty {
                listOf(appContext.getString(R.string.insight_default_rationale))
            },
            confidencePercent = payload.confidencePercent.coerceIn(0, 100),
            generatedAt = payload.generatedAt,
            source = InsightSource.LLM,
            llmUnavailableReason = null,
            fromCache = false
        )
    }

    private fun runHeuristicModel(
        appName: String,
        packageName: String,
        permissions: List<String>,
        llmIssue: String?
    ): PermissionInsight {
        val signalMatches = permissions.flatMap { permission ->
            PERMISSION_SIGNALS.filter { signal ->
                permission.contains(signal.keyword, ignoreCase = true)
            }.map { signal -> permission to signal }
        }

        val sensitiveMatches = signalMatches.size
        val baseScore = 20 + permissions.size * 2
        val signalScore = signalMatches.sumOf { it.second.weight }
        val bonus = if (permissions.size > 6) 6 else 0
        val riskScore = (baseScore + signalScore + bonus).coerceIn(5, 100)
        val riskLevel = when {
            riskScore >= 70 -> RiskLevel.HIGH
            riskScore >= 45 -> RiskLevel.MEDIUM
            else -> RiskLevel.LOW
        }

        val summary = buildSummary(appName, permissions.size, signalMatches)
        val rationale = buildRationale(signalMatches)
        val confidence = when {
            sensitiveMatches >= 4 -> 75
            sensitiveMatches >= 2 -> 65
            else -> 55
        }

        return PermissionInsight(
            packageName = packageName,
            appName = appName,
            summary = summary,
            riskScore = riskScore,
            riskLevel = riskLevel,
            rationale = if (rationale.isEmpty()) listOf(
                appContext.getString(R.string.insight_default_rationale)
            ) else rationale,
            confidencePercent = confidence,
            generatedAt = System.currentTimeMillis(),
            source = InsightSource.HEURISTIC,
            llmUnavailableReason = llmIssue
                ?: appContext.getString(R.string.insight_unavailable_offline_short),
            fromCache = false
        )
    }

    private fun buildSummary(
        appName: String,
        permissionCount: Int,
        signalMatches: List<Pair<String, PermissionSignal>>
    ): String {
        val topSignals = signalMatches.map { it.second.friendlyName }
            .distinct()
            .take(2)

        return if (topSignals.isEmpty()) {
            appContext.getString(
                R.string.insight_summary_generic,
                permissionCount,
                appName
            )
        } else {
            appContext.getString(
                R.string.insight_summary_with_signals,
                permissionCount,
                topSignals.joinToString(" & ")
            )
        }
    }

    private fun buildRationale(
        signalMatches: List<Pair<String, PermissionSignal>>
    ): List<String> {
        if (signalMatches.isEmpty()) return emptyList()

        return signalMatches
            .groupBy { it.second }
            .map { (signal, matches) ->
                val friendlyPermissions = matches
                    .map { formatPermissionName(it.first) }
                    .distinct()
                    .joinToString()
                "${signal.friendlyName}: ${signal.rationale} ($friendlyPermissions)"
            }
            .sortedByDescending { it.length }
            .take(4)
    }

    private fun formatPermissionName(permission: String): String {
        return permission.substringAfterLast('.')
            .replace('_', ' ')
            .lowercase(Locale.getDefault())
            .replaceFirstChar { current ->
                if (current.isLowerCase()) current.titlecase(Locale.getDefault()) else current.toString()
            }
    }

}

private data class PermissionSignal(
    val keyword: String,
    val weight: Int,
    val rationale: String,
    val friendlyName: String
)

private val PERMISSION_SIGNALS = listOf(
    PermissionSignal(
        keyword = "READ_SMS",
        weight = 35,
        rationale = "Reading SMS can expose one-time passwords and personal chats.",
        friendlyName = "SMS access"
    ),
    PermissionSignal(
        keyword = "SEND_SMS",
        weight = 30,
        rationale = "Sending SMS allows silently texting premium numbers.",
        friendlyName = "SMS sending"
    ),
    PermissionSignal(
        keyword = "READ_CONTACTS",
        weight = 25,
        rationale = "Contacts reveal who you communicate with.",
        friendlyName = "Contacts"
    ),
    PermissionSignal(
        keyword = "READ_CALL_LOG",
        weight = 28,
        rationale = "Call logs expose who you spoke with and when.",
        friendlyName = "Call history"
    ),
    PermissionSignal(
        keyword = "RECORD_AUDIO",
        weight = 30,
        rationale = "Microphone access could capture live conversations.",
        friendlyName = "Microphone"
    ),
    PermissionSignal(
        keyword = "CAMERA",
        weight = 32,
        rationale = "Camera access can capture photos or video without notice.",
        friendlyName = "Camera"
    ),
    PermissionSignal(
        keyword = "ACCESS_FINE_LOCATION",
        weight = 28,
        rationale = "Precise location reveals real-world movements.",
        friendlyName = "Precise location"
    ),
    PermissionSignal(
        keyword = "ACCESS_BACKGROUND_LOCATION",
        weight = 26,
        rationale = "Background location lets apps track you even when closed.",
        friendlyName = "Background location"
    ),
    PermissionSignal(
        keyword = "READ_PHONE_STATE",
        weight = 20,
        rationale = "Phone state includes device identifiers and network info.",
        friendlyName = "Phone state"
    ),
    PermissionSignal(
        keyword = "READ_EXTERNAL_STORAGE",
        weight = 18,
        rationale = "Storage access can expose personal files.",
        friendlyName = "Storage"
    ),
    PermissionSignal(
        keyword = "WRITE_EXTERNAL_STORAGE",
        weight = 18,
        rationale = "Write access lets apps modify or exfiltrate files.",
        friendlyName = "Storage modification"
    ),
    PermissionSignal(
        keyword = "PACKAGE_USAGE_STATS",
        weight = 22,
        rationale = "Usage stats reveal which apps you use and for how long.",
        friendlyName = "Usage stats"
    ),
    PermissionSignal(
        keyword = "SYSTEM_ALERT_WINDOW",
        weight = 34,
        rationale = "Overlay windows can spoof UI and capture input.",
        friendlyName = "Screen overlays"
    ),
    PermissionSignal(
        keyword = "REQUEST_INSTALL_PACKAGES",
        weight = 30,
        rationale = "Install packages lets apps sideload other APKs.",
        friendlyName = "Package installs"
    ),
    PermissionSignal(
        keyword = "BLUETOOTH_CONNECT",
        weight = 12,
        rationale = "Bluetooth access can reveal nearby devices.",
        friendlyName = "Bluetooth"
    ),
    PermissionSignal(
        keyword = "BODY_SENSORS",
        weight = 16,
        rationale = "Body sensors capture biometric measurements.",
        friendlyName = "Body sensors"
    )
)
