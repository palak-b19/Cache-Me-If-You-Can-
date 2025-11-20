package com.example.ussdemoproject.ui.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.ussdemoproject.databinding.ItemPermissionBinding
import com.example.ussdemoproject.ui.FontProvider

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

        holder.binding.permissionTitle.typeface = FontProvider.poppinsSemi(context)
        holder.binding.permissionDescription.typeface = FontProvider.poppinsRegular(context)
    }

    override fun getItemCount(): Int = permissions.size
}
