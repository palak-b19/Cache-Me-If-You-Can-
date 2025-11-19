package com.example.ussdemoproject.ui.adapters

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.ussdemoproject.databinding.ItemAppBinding
import com.example.ussdemoproject.models.AppInfo
import com.example.ussdemoproject.ui.AppDetailActivity

class AppListAdapter(
    private val context: Context,
    private var appList: List<AppInfo>
) : RecyclerView.Adapter<AppListAdapter.AppViewHolder>() {

    fun updateData(newApps: List<AppInfo>) {
        appList = newApps
        notifyDataSetChanged()
    }

    inner class AppViewHolder(val binding: ItemAppBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppViewHolder {
        val binding = ItemAppBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return AppViewHolder(binding)
    }

    override fun onBindViewHolder(holder: AppViewHolder, position: Int) {
        val app = appList[position]

        holder.binding.appName.text = app.appName
        holder.binding.packageName.text = app.packageName
        holder.binding.appIcon.setImageDrawable(app.icon)

        holder.itemView.setOnClickListener {
            val intent = Intent(context, AppDetailActivity::class.java)
            intent.putExtra("appName", app.appName)
            intent.putExtra("packageName", app.packageName)
            context.startActivity(intent)
        }
    }

    override fun getItemCount(): Int = appList.size
}
