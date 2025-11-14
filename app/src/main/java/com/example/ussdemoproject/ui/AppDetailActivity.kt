package com.example.ussdemoproject.ui

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.res.ResourcesCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.ussdemoproject.R
import com.example.ussdemoproject.databinding.ActivityAppDetailBinding
import com.example.ussdemoproject.ui.adapters.PermissionAdapter

class AppDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAppDetailBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAppDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // -----------------------------
        // LOAD CUSTOM FONTS
        // -----------------------------
        val anton = ResourcesCompat.getFont(this, R.font.anton_regular)
        val montserratRegular = ResourcesCompat.getFont(this, R.font.montserrat_regular)
        val montserratSemiBold = ResourcesCompat.getFont(this, R.font.montserrat_semibold)

        // -----------------------------
        // GET APP INFORMATION
        // -----------------------------
        val appName = intent.getStringExtra("appName")
        val packageName = intent.getStringExtra("packageName")

        binding.appName.text = appName
        binding.packageName.text = packageName

        // APPLY FONTS
        binding.appName.typeface = anton
        binding.packageName.typeface = montserratRegular
        binding.permissionsHeader.typeface = anton

        // -----------------------------
        // LOAD APP ICON SAFELY
        // -----------------------------
        try {
            val icon = packageManager.getApplicationIcon(packageName!!)
            binding.appIcon.setImageDrawable(icon)
        } catch (_: Exception) {}

        // -----------------------------
        // GET PERMISSIONS FOR THIS APP
        // -----------------------------
        val permissions = try {
            packageManager.getPackageInfo(packageName!!, PackageManager.GET_PERMISSIONS)
                .requestedPermissions?.toList() ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }

        // -----------------------------
        // SETUP RECYCLER VIEW
        // -----------------------------
        binding.permissionsRecycler.apply {
            layoutManager = LinearLayoutManager(this@AppDetailActivity)
            adapter = PermissionAdapter(this@AppDetailActivity, permissions)
        }

        // -----------------------------
        // MANAGE PERMISSIONS BUTTON
        // -----------------------------
        binding.managePermissionsBtn.setOnClickListener {
            packageName?.let { pkg ->
                val intent = Intent(
                    android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                )
                intent.data = Uri.parse("package:$pkg")
                startActivity(intent)
            }
        }
    }
}
