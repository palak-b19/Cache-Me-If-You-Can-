package com.example.ussdemoproject.ui.adapters

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.ussdemoproject.R
import com.example.ussdemoproject.ai.RiskLevel
import com.example.ussdemoproject.databinding.ListItemAppBinding
import com.example.ussdemoproject.databinding.ListItemHeaderBinding
import com.example.ussdemoproject.models.AppInfo
import com.example.ussdemoproject.ui.AppDetailActivity

sealed class ListItem {
    data class Header(val riskLevel: RiskLevel) : ListItem()
    data class AppItem(val appInfo: AppInfo) : ListItem()
}

class RiskGroupedAppListAdapter(
    private val context: Context,
    private var originalData: List<AppInfo>
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var items: List<ListItem> = emptyList()

    companion object {
        private const val TYPE_HEADER = 0
        private const val TYPE_APP = 1
    }

    init {
        updateData(originalData)
    }

    override fun getItemViewType(position: Int): Int {
        return when (items[position]) {
            is ListItem.Header -> TYPE_HEADER
            is ListItem.AppItem -> TYPE_APP
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(context)
        return if (viewType == TYPE_HEADER) {
            HeaderViewHolder(inflater.inflate(R.layout.list_item_header, parent, false))
        } else {
            AppViewHolder(inflater.inflate(R.layout.list_item_app, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = items[position]) {
            is ListItem.Header -> (holder as HeaderViewHolder).bind(item.riskLevel)
            is ListItem.AppItem -> (holder as AppViewHolder).bind(item.appInfo)
        }
    }

    override fun getItemCount(): Int = items.size

    fun updateData(newApps: List<AppInfo>) {
        this.originalData = newApps
        val grouped = newApps.groupBy { it.riskLevel ?: RiskLevel.LOW }

        val newItems = mutableListOf<ListItem>()
        grouped.entries.sortedByDescending { it.key.ordinal }.forEach { (riskLevel, apps) ->
            newItems.add(ListItem.Header(riskLevel))
            newItems.addAll(apps.map { ListItem.AppItem(it) })
        }
        this.items = newItems
        notifyDataSetChanged()
    }

    inner class AppViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val binding = ListItemAppBinding.bind(itemView)

        fun bind(appInfo: AppInfo) {
            binding.appName.text = appInfo.appName
            binding.appIcon.setImageDrawable(appInfo.icon)

            itemView.setOnClickListener {
                val intent = Intent(context, AppDetailActivity::class.java).apply {
                    putExtra("appName", appInfo.appName)
                    putExtra("packageName", appInfo.packageName)
                }
                context.startActivity(intent)
            }
        }
    }

    inner class HeaderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val binding = ListItemHeaderBinding.bind(itemView)

        fun bind(riskLevel: RiskLevel) {
            binding.headerTitle.text = context.getString(riskLevel.labelRes)
        }
    }
}
