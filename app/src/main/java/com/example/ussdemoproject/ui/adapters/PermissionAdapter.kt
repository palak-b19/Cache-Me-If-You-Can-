package com.example.ussdemoproject.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.ussdemoproject.databinding.ItemPermissionBinding

class PermissionAdapter(private val permissions: List<String>) :
    RecyclerView.Adapter<PermissionAdapter.PermissionViewHolder>() {

    inner class PermissionViewHolder(val binding: ItemPermissionBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PermissionViewHolder {
        val binding = ItemPermissionBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return PermissionViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PermissionViewHolder, position: Int) {
        holder.binding.permissionText.text = permissions[position]
    }

    override fun getItemCount(): Int = permissions.size
}
