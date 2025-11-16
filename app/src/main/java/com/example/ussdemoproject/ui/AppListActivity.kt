package com.example.ussdemoproject.ui

import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.SearchView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.ussdemoproject.R
import com.example.ussdemoproject.models.AppInfo
import com.example.ussdemoproject.ui.adapters.AppListAdapter

class AppListActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var searchView: SearchView
    private lateinit var appListAdapter: AppListAdapter
    private val allApps = mutableListOf<AppInfo>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_app_list)

        recyclerView = findViewById(R.id.recyclerView)
        searchView = findViewById(R.id.searchView)

        recyclerView.layoutManager = LinearLayoutManager(this)

        val pm = packageManager
        allApps.addAll(pm.getInstalledApplications(PackageManager.GET_META_DATA)
            .filter { pm.getLaunchIntentForPackage(it.packageName) != null }  // ✅ Only show user apps
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
            .sortedBy { it.appName.lowercase() })

        appListAdapter = AppListAdapter(this, allApps)
        recyclerView.adapter = appListAdapter

        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                val filteredList = allApps.filter {
                    it.appName.contains(newText ?: "", ignoreCase = true)
                }
                recyclerView.adapter = AppListAdapter(this@AppListActivity, filteredList)
                return true
            }
        })
    }
}
