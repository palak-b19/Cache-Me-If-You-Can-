package com.example.ussdemoproject.ui

import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.ussdemoproject.ai.PermissionInsightEngine
import com.example.ussdemoproject.ai.PermissionInsightResult
import com.example.ussdemoproject.databinding.ActivityAppListBinding
import com.example.ussdemoproject.models.AppInfo
import com.example.ussdemoproject.ui.adapters.RiskGroupedAppListAdapter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AppListActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAppListBinding
    private lateinit var adapter: RiskGroupedAppListAdapter
    private lateinit var prefs: SharedPreferences
    private var allApps: List<AppInfo> = emptyList()
    private val insightEngine by lazy { PermissionInsightEngine(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        prefs = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        applyTheme()

        super.onCreate(savedInstanceState)
        binding = ActivityAppListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupThemeToggle()
        setupRecyclerView()
        loadApps()
    }

    // -----------------------------------------------------------
    // THEME LOGIC
    // -----------------------------------------------------------
    private fun applyTheme() {
        val isDark = prefs.getBoolean("dark_mode", false)
        AppCompatDelegate.setDefaultNightMode(
            if (isDark) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
        )
    }

    private fun setupThemeToggle() {
        val isDark = prefs.getBoolean("dark_mode", false)
        updateThemeIcon(isDark)

        binding.themeToggleBtn.setOnClickListener {
            val newMode = !prefs.getBoolean("dark_mode", false)
            prefs.edit().putBoolean("dark_mode", newMode).apply()

            updateThemeIcon(newMode)

            AppCompatDelegate.setDefaultNightMode(
                if (newMode) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
            )
        }
    }

    private fun updateThemeIcon(isDark: Boolean) {
        val icon = if (isDark) {
            android.R.drawable.ic_menu_day
        } else {
            android.R.drawable.ic_menu_sort_by_size
        }
        binding.themeToggleBtn.setImageResource(icon)
    }

    // -----------------------------------------------------------
    // LIST SETUP
    // -----------------------------------------------------------
    private fun setupRecyclerView() {
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = RiskGroupedAppListAdapter(this, emptyList())
        binding.recyclerView.adapter = adapter
    }

    private fun loadApps() {
        binding.progressBar.isVisible = true
        lifecycleScope.launch(Dispatchers.IO) {
            val pm = packageManager

            allApps = pm.getInstalledApplications(PackageManager.GET_META_DATA)
                .filter { pm.getLaunchIntentForPackage(it.packageName) != null } // only user apps
                .map { app ->
                    val permissions = try {
                        pm.getPackageInfo(app.packageName, PackageManager.GET_PERMISSIONS)
                            .requestedPermissions?.toList()
                    } catch (_: Exception) { null }

                    AppInfo(
                        appName = app.loadLabel(pm).toString(),
                        packageName = app.packageName,
                        icon = app.loadIcon(pm),
                        permissions = permissions
                    )
                }
                .sortedBy { it.appName.lowercase() }

            withContext(Dispatchers.Main) {
                adapter.updateData(allApps)
                binding.progressBar.isVisible = false
                startInsightAnalysis()
            }
        }
    }

    private fun startInsightAnalysis() {
        lifecycleScope.launch(Dispatchers.IO) {
            allApps.forEach { appInfo ->
                val result = insightEngine.analyze(
                    appName = appInfo.appName,
                    packageName = appInfo.packageName,
                    permissions = appInfo.permissions ?: emptyList()
                )

                if (result is PermissionInsightResult.Success) {
                    appInfo.riskScore = result.insight.riskScore
                    appInfo.riskLevel = result.insight.riskLevel
                    appInfo.summary = result.insight.summary
                    appInfo.generatedAt = result.insight.generatedAt

                    withContext(Dispatchers.Main) {
                        adapter.updateData(allApps)
                    }
                }
            }
        }
    }
}
