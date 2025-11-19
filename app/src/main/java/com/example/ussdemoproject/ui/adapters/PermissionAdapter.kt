package com.example.ussdemoproject.ui.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.res.ResourcesCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.ussdemoproject.R
import com.example.ussdemoproject.databinding.ItemPermissionBinding



class PermissionAdapter(
    private val context: Context,
    private val permissions: List<String>
) : RecyclerView.Adapter<PermissionAdapter.PermissionViewHolder>() {

    inner class PermissionViewHolder(val binding: ItemPermissionBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PermissionViewHolder {
        val binding = ItemPermissionBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return PermissionViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PermissionViewHolder, position: Int) {
        val permission = permissions[position]

        val readableTitle = permission.substringAfterLast('.')
            .replace("_", " ")
            .lowercase()
            .replaceFirstChar { it.uppercase() }

        holder.binding.permissionTitle.text = readableTitle
        holder.binding.permissionDescription.text =
            "This app uses $readableTitle permission."

        // Apply Poppins fonts
        holder.binding.permissionTitle.typeface =
            ResourcesCompat.getFont(context, R.font.poppins_semibold)

        holder.binding.permissionDescription.typeface =
            ResourcesCompat.getFont(context, R.font.poppins_regular)
    }

    override fun getItemCount(): Int = permissions.size
}
