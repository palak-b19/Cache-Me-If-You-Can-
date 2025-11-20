package com.example.ussdemoproject.ui

import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import com.example.ussdemoproject.databinding.ActivityAppListBinding
import com.example.ussdemoproject.models.AppInfo
import com.example.ussdemoproject.ui.adapters.AppListAdapter

class AppListActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAppListBinding
    private lateinit var adapter: AppListAdapter
    private lateinit var prefs: SharedPreferences
    private var allApps: List<AppInfo> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {

        // ------------------------------------------------------------------
        // SECURE THEME PREFERENCES - ENCRYPTED SHARED PREFS
        // ------------------------------------------------------------------
        val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)

        prefs = EncryptedSharedPreferences.create(
            "app_prefs",
            masterKeyAlias,
            this,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )

        applyTheme()

        super.onCreate(savedInstanceState)
        binding = ActivityAppListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupThemeToggle()
        setupRecyclerView()
        loadApps()
        setupSearch()
    }

    // ----------------------------------------------------------------------
    // THEME SYSTEM
    // ----------------------------------------------------------------------
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

    // ----------------------------------------------------------------------
    // LIST SETUP
    // ----------------------------------------------------------------------
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
                } catch (_: Exception) { null }

                AppInfo(
                    appName = app.loadLabel(pm).toString(),
                    packageName = app.packageName,
                    permissions = permissions   // ✅ no icon here
                )
            }
            .sortedBy { it.appName.lowercase() }

        adapter.updateData(allApps)
    }

    // ----------------------------------------------------------------------
    // SEARCH SYSTEM
    // ----------------------------------------------------------------------
    private fun setupSearch() {
        binding.searchBar.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun afterTextChanged(s: Editable?) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterApps(s.toString())
            }
        })
    }

    private fun filterApps(query: String) {
        val filtered = if (query.isBlank()) {
            allApps
        } else {
            allApps.filter {
                it.appName.contains(query, ignoreCase = true) ||
                        it.packageName.contains(query, ignoreCase = true)
            }
        }
        adapter.updateData(filtered)
    }
}
