package com.kapwadvo.app.ui.admin

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.kapwadvo.app.R
import com.kapwadvo.app.data.models.Listing
import com.kapwadvo.app.databinding.ItemListingAdminBinding

class AdminListingAdapter(
    private val onEdit: (Listing) -> Unit,
    private val onDelete: (Listing) -> Unit
) : RecyclerView.Adapter<AdminListingAdapter.ViewHolder>() {

    private val listings = mutableListOf<Listing>()

    fun submitList(newList: List<Listing>) {
        listings.clear()
        listings.addAll(newList)
        notifyDataSetChanged()
    }

    inner class ViewHolder(private val binding: ItemListingAdminBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(listing: Listing) {
            binding.tvName.text = listing.name
            binding.tvCategory.text = listing.category
            binding.tvPhotoLabel.text = "[ ${listing.name} ]"
            binding.tvStatus.text = listing.status.replaceFirstChar { it.uppercase() }

            val (bg, textColor) = when (listing.status) {
                "approved" -> R.drawable.bg_badge_approved to R.color.approved_text
                "rejected" -> R.drawable.bg_badge_rejected to R.color.rejected_text
                else -> R.drawable.bg_badge_pending to R.color.pending_text
            }
            binding.tvStatus.setBackgroundResource(bg)
            binding.tvStatus.setTextColor(ContextCompat.getColor(binding.root.context, textColor))

            binding.btnEdit.setOnClickListener { onEdit(listing) }
            binding.btnDelete.setOnClickListener { onDelete(listing) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemListingAdminBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(listings[position])
    override fun getItemCount() = listings.size
}
