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
import com.example.ussdemoproject.ai.PermissionInsight
import com.example.ussdemoproject.ai.PermissionInsightEngine
import com.example.ussdemoproject.ai.PermissionInsightResult
import com.example.ussdemoproject.databinding.ActivityAppDetailBinding
import com.example.ussdemoproject.ui.adapters.PermissionAdapter
import java.text.DateFormat
import java.util.Date
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AppDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAppDetailBinding
    private val insightEngine by lazy { PermissionInsightEngine(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAppDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val appName = intent.getStringExtra("appName")
        val packageName = intent.getStringExtra("packageName")

        if (appName.isNullOrBlank() || packageName.isNullOrBlank()) {
            finish()
            return
        }

        binding.appName.text = appName
        binding.packageName.text = packageName

        try {
            val icon = packageManager.getApplicationIcon(packageName)
            binding.appIcon.setImageDrawable(icon)
        } catch (_: Exception) {
        }

        val permissions = try {
            packageManager.getPackageInfo(packageName, PackageManager.GET_PERMISSIONS)
                .requestedPermissions?.toList() ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }

        binding.permissionsRecycler.apply {
            layoutManager = LinearLayoutManager(this@AppDetailActivity)
            adapter = PermissionAdapter(permissions)
        }

        binding.managePermissionsBtn.setOnClickListener {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            intent.data = Uri.parse("package:$packageName")
            startActivity(intent)
        }

        loadPermissionInsights(appName, packageName, permissions)
    }

    private fun loadPermissionInsights(appName: String, packageName: String, permissions: List<String>) {
        binding.insightProgress.isVisible = true
        binding.riskCard.isVisible = false
        hideUnavailableMessage()

        lifecycleScope.launch {
            val result = withContext(Dispatchers.Default) {
                insightEngine.analyze(appName, packageName, permissions)
            }
            binding.insightProgress.isVisible = false
            when (result) {
                is PermissionInsightResult.Success -> {
                    renderInsight(result.insight)
                    val llmIssue = result.insight.llmUnavailableReason
                    if (llmIssue != null) {
                        showUnavailableMessage(llmIssue)
                    } else {
                        hideUnavailableMessage()
                    }
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
        ViewCompat.setBackgroundTintList(
            binding.riskBadge,
            ColorStateList.valueOf(badgeColor)
        )
        binding.riskBadge.text = getString(insight.riskLevel.labelRes)
        binding.riskScoreValue.text = getString(R.string.risk_score_template, insight.riskScore)
        binding.riskScoreValue.setTextColor(badgeColor)
        binding.riskSummary.text = insight.summary
        binding.insightConfidence.text = getString(
            R.string.insight_confidence_template,
            insight.confidencePercent
        )
        binding.insightTimestamp.text = getString(
            R.string.insight_updated_at,
            formatTimestamp(insight.generatedAt)
        )
        binding.insightCacheBadge.isVisible = insight.fromCache
        if (insight.fromCache) {
            ViewCompat.setBackgroundTintList(
                binding.insightCacheBadge,
                ColorStateList.valueOf(
                    ContextCompat.getColor(this, R.color.purple_700)
                )
            )
        }

        binding.rationaleContainer.removeAllViews()
        binding.rationaleHeader.isVisible = insight.rationale.isNotEmpty()
        insight.rationale.forEach { rationale ->
            val rationaleView = TextView(this).apply {
                text = "- $rationale"
                textSize = 14f
                setTextColor(ContextCompat.getColor(context, R.color.text_secondary))
                setPadding(0, 8, 0, 8)
                setLineSpacing(0f, 1.2f)
            }
            binding.rationaleContainer.addView(rationaleView)
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
        val formatter = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT)
        return formatter.format(Date(epochMillis))
    }
}
