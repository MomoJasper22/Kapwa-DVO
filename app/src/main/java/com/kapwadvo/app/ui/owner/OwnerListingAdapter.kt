package com.kapwadvo.app.ui.owner

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.kapwadvo.app.R
import com.kapwadvo.app.data.models.Listing
import com.kapwadvo.app.databinding.ItemOwnerListingBinding

class OwnerListingAdapter(
    private val onEdit: (Listing) -> Unit,
    private val onDelete: (Listing) -> Unit,
    private val onToggleBooking: (Listing, Boolean) -> Unit,
    private val onCancelUpdate: (Listing) -> Unit
) : RecyclerView.Adapter<OwnerListingAdapter.ViewHolder>() {

    private val listings = mutableListOf<Listing>()

    fun submitList(newList: List<Listing>) {
        listings.clear()
        listings.addAll(newList)
        notifyDataSetChanged()
    }

    inner class ViewHolder(private val binding: ItemOwnerListingBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(listing: Listing) {
            binding.tvName.text = listing.name
            // Owner sees full category info including "Others: <custom text>"
            binding.tvCategory.text = com.kapwadvo.app.data.CategoryManager
                .toAdminDisplayString(listing.category).ifEmpty { listing.category }
            
            if (listing.photoUrls.isNotEmpty()) {
                binding.ivPhoto.visibility = android.view.View.VISIBLE
                binding.tvPhotoLabel.visibility = android.view.View.GONE
                com.bumptech.glide.Glide.with(binding.root.context)
                    .load(listing.photoUrls.first())
                    .into(binding.ivPhoto)
            } else {
                binding.ivPhoto.visibility = android.view.View.GONE
                binding.tvPhotoLabel.visibility = android.view.View.VISIBLE
                binding.tvPhotoLabel.text = "[ ${listing.name} ]"
            }
            binding.tvStatus.text = listing.status.replaceFirstChar { it.uppercase() }

            val (bg, textColor) = when (listing.status) {
                "approved" -> R.drawable.bg_badge_approved to R.color.approved_text
                "rejected" -> R.drawable.bg_badge_rejected to R.color.rejected_text
                else -> R.drawable.bg_badge_pending to R.color.pending_text
            }
            binding.tvStatus.setBackgroundResource(bg)
            binding.tvStatus.setTextColor(ContextCompat.getColor(binding.root.context, textColor))
            binding.tvStatus.setPadding(12, 4, 12, 4)

            // Booking toggle — only relevant for approved listings
            val isApproved = listing.status == "approved"
            binding.btnToggleBooking.isEnabled = isApproved
            if (listing.bookingsEnabled) {
                binding.btnToggleBooking.text = "Disable Booking"
                binding.btnToggleBooking.alpha = 1f
            } else {
                binding.btnToggleBooking.text = "Enable Booking"
                binding.btnToggleBooking.alpha = if (isApproved) 1f else 0.4f
            }

            binding.btnToggleBooking.setOnClickListener {
                onToggleBooking(listing, !listing.bookingsEnabled)
            }

            if (listing.pendingUpdates != null) {
                binding.tvPendingUpdate.visibility = android.view.View.VISIBLE
                binding.btnEdit.text = "Cancel Update"
                binding.btnEdit.setOnClickListener { onCancelUpdate(listing) }
            } else {
                binding.tvPendingUpdate.visibility = android.view.View.GONE
                binding.btnEdit.text = binding.root.context.getString(R.string.edit)
                binding.btnEdit.setOnClickListener { onEdit(listing) }
            }

            binding.btnDelete.setOnClickListener { onDelete(listing) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemOwnerListingBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(listings[position])
    override fun getItemCount() = listings.size
}
