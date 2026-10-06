package com.kapwadvo.app.ui.admin

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.kapwadvo.app.data.CategoryManager
import com.kapwadvo.app.data.models.OwnerApplication
import com.kapwadvo.app.databinding.ItemApplicationBinding

class AdminAppAdapter(
    private val onApprove: (OwnerApplication) -> Unit,
    private val onReject: (OwnerApplication) -> Unit
) : RecyclerView.Adapter<AdminAppAdapter.ViewHolder>() {

    private val apps = mutableListOf<OwnerApplication>()

    fun submitList(newList: List<OwnerApplication>) {
        apps.clear()
        apps.addAll(newList)
        notifyDataSetChanged()
    }

    inner class ViewHolder(private val binding: ItemApplicationBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(app: OwnerApplication) {
            val shortId = app.userId.take(8) + "…"
            binding.tvUserId.text = "User: $shortId"
            binding.tvAvatarLetter.text = "U"
            binding.tvSubmittedAt.text = "Submitted: ${app.submittedAt?.take(10) ?: "—"}"
            binding.tvAppStatus.text = app.status.replaceFirstChar { it.uppercase() }

            // Show business name & full admin category (includes "Others: <detail>")
            if (app.businessName != null) {
                binding.tvBusinessName.text = "Business: ${app.businessName}"
                binding.tvBusinessName.visibility = View.VISIBLE
            } else {
                binding.tvBusinessName.visibility = View.GONE
            }

            val catDisplay = CategoryManager.toAdminDisplayString(app.category ?: "")
            if (catDisplay.isNotEmpty()) {
                binding.tvCategoryDisplay.text = "Category: $catDisplay"
                binding.tvCategoryDisplay.visibility = View.VISIBLE
            } else {
                binding.tvCategoryDisplay.visibility = View.GONE
            }

            binding.btnApprove.setOnClickListener { onApprove(app) }
            binding.btnReject.setOnClickListener { onReject(app) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemApplicationBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(apps[position])
    override fun getItemCount() = apps.size
}
