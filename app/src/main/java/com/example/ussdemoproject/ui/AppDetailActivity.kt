package com.example.ussdemoproject.ui

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.ussdemoproject.databinding.ActivityAppDetailBinding
import com.example.ussdemoproject.ui.adapters.PermissionAdapter

class AppDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAppDetailBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAppDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val appName = intent.getStringExtra("appName")
        val packageName = intent.getStringExtra("packageName")

        binding.appName.text = appName
        binding.packageName.text = packageName

        try {
            val icon = packageManager.getApplicationIcon(packageName!!)
            binding.appIcon.setImageDrawable(icon)
        } catch (_: Exception) {}

        val permissions = try {
            packageManager.getPackageInfo(packageName!!, PackageManager.GET_PERMISSIONS)
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
    }
}
