package com.example.ussdemoproject.ui

import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.ussdemoproject.R
import com.example.ussdemoproject.ai.InsightSource
import com.example.ussdemoproject.ai.PermissionInsight
import com.example.ussdemoproject.ai.PermissionInsightEngine
import com.example.ussdemoproject.ai.PermissionInsightResult
import com.example.ussdemoproject.databinding.ActivityAppDetailBinding
import com.example.ussdemoproject.ui.adapters.PermissionAdapter
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.DateFormat
import java.util.Date

class   AppDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAppDetailBinding
    private val insightEngine by lazy { PermissionInsightEngine(this) }
    private var analysisJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAppDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // -----------------------------
        // SAFE FONT LOADING
        // -----------------------------
        val anton = FontProvider.anton(this)
        val montserratRegular = FontProvider.montserratRegular(this)
        val montserratSemiBold = FontProvider.montserratSemi(this)

        // -----------------------------
        // GET APP INFORMATION
        // -----------------------------
        val appName = intent.getStringExtra("appName")
        val packageName = intent.getStringExtra("packageName")

        if (appName.isNullOrBlank() || packageName.isNullOrBlank()) {
            finish()
            return
        }

        binding.appName.text = appName
        binding.packageName.text = packageName

        // Apply fonts
        binding.appName.typeface = anton
        binding.packageName.typeface = montserratRegular
        binding.permissionsHeader.typeface = anton

        // -----------------------------
        // LOAD APP ICON
        // -----------------------------
        try {
            binding.appIcon.setImageDrawable(packageManager.getApplicationIcon(packageName))
        } catch (_: Exception) {}

        // -----------------------------
        // GET PERMISSIONS
        // -----------------------------
        val permissions = intent.getStringArrayListExtra("permissions")?.toList() ?: try {
            packageManager.getPackageInfo(packageName, PackageManager.GET_PERMISSIONS)
                .requestedPermissions?.toList() ?: emptyList()
        } catch (_: Exception) { emptyList() }

        // -----------------------------
        // SETUP RECYCLERVIEW
        // -----------------------------
        binding.permissionsRecycler.layoutManager = LinearLayoutManager(this)
        binding.permissionsRecycler.adapter =
            PermissionAdapter(this, permissions)

        // -----------------------------
        // MANAGE PERMISSIONS BUTTON
        // -----------------------------
        binding.managePermissionsBtn.setOnClickListener {
            val intent = Intent(
                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.parse("package:$packageName")
            )
            startActivity(intent)
        }

        // -----------------------------
        // LOAD INSIGHTS
        // -----------------------------
        loadPermissionInsights(appName, packageName, permissions)
    }

    private fun loadPermissionInsights(
        appName: String,
        packageName: String,
        permissions: List<String>,
        forceHeuristic: Boolean = false
    ) {
        binding.insightProgress.isVisible = true
        binding.riskCard.isVisible = false
        hideUnavailableMessage()

        analysisJob?.cancel()
        analysisJob = lifecycleScope.launch {
            val timerJob = if (!forceHeuristic) {
                launch {
                    delay(20000) // 20 seconds
                    if (isActive) {
                        Snackbar.make(
                            binding.root,
                            "Analysis is taking longer than expected.",
                            Snackbar.LENGTH_INDEFINITE
                        )
                            .setAction("Use Heuristic") {
                                loadPermissionInsights(appName, packageName, permissions, forceHeuristic = true)
                            }
                            .show()
                    }
                }
            } else null

            val result = withContext(Dispatchers.Default) {
                insightEngine.analyze(appName, packageName, permissions, forceHeuristic)
            }

            timerJob?.cancel()
            binding.insightProgress.isVisible = false

            when (result) {
                is PermissionInsightResult.Success -> {
                    renderInsight(result.insight)
                    result.insight.llmUnavailableReason?.let { showUnavailableMessage(it) }
                }
                is PermissionInsightResult.Unavailable -> {
                    showUnavailableMessage(
                        getString(R.string.insight_unavailable_generic, result.reason)
                    )
                }
            }
        }
    }

    private fun renderInsight(insight: PermissionInsight) {
        binding.riskCard.isVisible = true

        val badgeColor = ContextCompat.getColor(this, insight.riskLevel.colorRes)

        ViewCompat.setBackgroundTintList(binding.riskBadge, ColorStateList.valueOf(badgeColor))
        binding.riskBadge.text = getString(insight.riskLevel.labelRes)
        binding.riskScoreValue.text = getString(R.string.risk_score_template, insight.riskScore)
        binding.riskScoreValue.setTextColor(badgeColor)

        val cleanedSummary = insight.summary
            .replace(Regex("\\b\\d+ permissions?\\b"), "")
            .trim()

        binding.riskSummary.text = cleanedSummary

        binding.insightConfidence.text =
            getString(R.string.insight_confidence_template, insight.confidencePercent)
        binding.insightTimestamp.text =
            getString(R.string.insight_updated_at, formatTimestamp(insight.generatedAt))

        val sourceText = if (insight.source == InsightSource.LLM) "AI Model" else "Heuristic"
        binding.insightSource.text = "Source: $sourceText"

        binding.insightCacheBadge.isVisible = insight.fromCache

        // RATIONALE LIST
        binding.rationaleContainer.removeAllViews()
        binding.rationaleHeader.isVisible = insight.rationale.isNotEmpty()

        insight.rationale.forEach { rationale ->
            val tv = TextView(this).apply {
                text = "• $rationale"
                textSize = 14f
                typeface = FontProvider.montserratRegular(context)
                setTextColor(ContextCompat.getColor(context, R.color.text_secondary))
                setPadding(0, 8, 0, 8)
                setLineSpacing(0f, 1.2f)
            }
            binding.rationaleContainer.addView(tv)
        }
    }

    private fun showUnavailableMessage(message: String) {
        binding.insightUnavailableGroup.isVisible = true
        binding.insightUnavailableMessage.text = message
    }

    private fun hideUnavailableMessage() {
        binding.insightUnavailableGroup.isVisible = false
        binding.insightUnavailableMessage.text = ""
    }

    private fun formatTimestamp(epochMillis: Long): String {
        return DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT)
            .format(Date(epochMillis))
    }
}
