package com.hawky.shadowdisplay.permission

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.hawky.shadowdisplay.R

/**
 * 权限引导列表适配器
 */
class PermissionGuideAdapter(
    private val permissions: List<PermissionStep>,
    private val onRequestPermission: (PermissionStep) -> Unit
) : RecyclerView.Adapter<PermissionGuideAdapter.PermissionViewHolder>() {

    class PermissionViewHolder(itemView: android.view.View) : RecyclerView.ViewHolder(itemView) {
        val titleText: TextView = itemView.findViewById(R.id.tv_permission_title)
        val descriptionText: TextView = itemView.findViewById(R.id.tv_permission_desc)
        val importanceBadge: TextView = itemView.findViewById(R.id.tv_importance)
        val requestButton: TextView = itemView.findViewById(R.id.btn_request_permission)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PermissionViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_permission_step, parent, false)
        return PermissionViewHolder(view)
    }

    override fun onBindViewHolder(holder: PermissionViewHolder, position: Int) {
        val permission = permissions[position]

        holder.titleText.text = permission.title
        holder.descriptionText.text = permission.description

        // 设置重要性徽章
        when (permission.importance) {
            PermissionStep.PermissionImportance.CRITICAL -> {
                holder.importanceBadge.text = "必需"
                holder.importanceBadge.setBackgroundResource(R.drawable.bg_importance_critical)
            }
            PermissionStep.PermissionImportance.HIGH -> {
                holder.importanceBadge.text = "重要"
                holder.importanceBadge.setBackgroundResource(R.drawable.bg_importance_high)
            }
            PermissionStep.PermissionImportance.MEDIUM -> {
                holder.importanceBadge.text = "建议"
                holder.importanceBadge.setBackgroundResource(R.drawable.bg_importance_medium)
            }
            PermissionStep.PermissionImportance.LOW -> {
                holder.importanceBadge.text = "可选"
                holder.importanceBadge.setBackgroundResource(R.drawable.bg_importance_low)
            }
        }

        holder.requestButton.setOnClickListener {
            onRequestPermission(permission)
        }
    }

    override fun getItemCount(): Int = permissions.size
}
