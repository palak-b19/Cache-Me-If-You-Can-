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
        // Detect hex-like tokens and show a friendly masked label instead of the raw value
        val cleanedTitle = readableTitle.replace("\\s".toRegex(), "")
        val hexLikeRegex = Regex("^[0-9a-fA-F]{32,}\$")
        val isHexToken = hexLikeRegex.matches(cleanedTitle)

        val maxDisplayLength = 48
        val displayTitle = when {
            isHexToken -> "Token (hidden)"
            readableTitle.length > maxDisplayLength -> readableTitle.take(maxDisplayLength - 3) + "..."
            else -> readableTitle
        }

        holder.binding.permissionTitle.text = displayTitle
        holder.binding.permissionDescription.text =
            if (isHexToken) "This app declares a token-like permission." else "This app uses $displayTitle permission."

        holder.binding.permissionTitle.typeface = FontProvider.poppinsSemi(context)
        holder.binding.permissionDescription.typeface = FontProvider.poppinsRegular(context)
    }

    override fun getItemCount(): Int = permissions.size
}
