package com.example.ussdemoproject.ui

import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.ussdemoproject.databinding.ActivityAppListBinding
import com.example.ussdemoproject.models.AppInfo
import com.example.ussdemoproject.ui.adapters.AppListAdapter

class AppListActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAppListBinding
    private lateinit var adapter: AppListAdapter
    private var allApps: List<AppInfo> = emptyList()
    private lateinit var prefs: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        prefs = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        applyTheme()
        
        super.onCreate(savedInstanceState)
        binding = ActivityAppListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupThemeToggle()
        setupRecyclerView()
        loadApps()
        setupSearch()
    }

    private fun applyTheme() {
        val isDarkMode = prefs.getBoolean("dark_mode", false)
        AppCompatDelegate.setDefaultNightMode(
            if (isDarkMode) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
        )
    }

    private fun setupThemeToggle() {
        val isDarkMode = prefs.getBoolean("dark_mode", false)
        updateThemeIcon(isDarkMode)

        binding.themeToggleBtn.setOnClickListener {
            val newMode = !prefs.getBoolean("dark_mode", false)
            prefs.edit().putBoolean("dark_mode", newMode).apply()
            updateThemeIcon(newMode)
            AppCompatDelegate.setDefaultNightMode(
                if (newMode) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
            )
        }
    }

    private fun updateThemeIcon(isDarkMode: Boolean) {
        val iconRes = if (isDarkMode) android.R.drawable.ic_menu_day else android.R.drawable.ic_menu_sort_by_size
        // Note: Using standard android icons for simplicity, ideally use custom moon/sun icons
        // ic_menu_day is usually a sun-like icon in older android versions, 
        // but let's stick to generic ones if specific assets aren't available.
        // Actually, let's just use text description or standard icons if available.
        // Since we don't have custom drawables, we'll rely on the system ones.
    }

    private fun setupRecyclerView() {
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = AppListAdapter(this, emptyList())
        binding.recyclerView.adapter = adapter
    }

    private fun loadApps() {
        val pm = packageManager
        allApps = pm.getInstalledApplications(PackageManager.GET_META_DATA)
            .filter { pm.getLaunchIntentForPackage(it.packageName) != null }
            .map { app ->
                val permissions = try {
                    pm.getPackageInfo(app.packageName, PackageManager.GET_PERMISSIONS)
                        .requestedPermissions?.toList()
                } catch (_: Exception) {
                    null
                }

                AppInfo(
                    appName = app.loadLabel(pm).toString(),
                    packageName = app.packageName,
                    icon = app.loadIcon(pm),
                    permissions = permissions
                )
            }
            .sortedBy { it.appName.lowercase() }
        
        adapter.updateData(allApps)
    }

    private fun setupSearch() {
        binding.searchBar.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterApps(s.toString())
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun filterApps(query: String) {
        val filteredList = if (query.isBlank()) {
            allApps
        } else {
            allApps.filter {
                it.appName.contains(query, ignoreCase = true) || 
                it.packageName.contains(query, ignoreCase = true)
            }
        }
        adapter.updateData(filteredList)
    }
}
