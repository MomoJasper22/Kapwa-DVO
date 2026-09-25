package com.kapwadvo.app.ui.main.explore

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.kapwadvo.app.data.models.Listing
import com.kapwadvo.app.databinding.ItemHighlightListingBinding
import androidx.lifecycle.findViewTreeLifecycleOwner
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
data class HighlightItem(val listing: Listing, val avgRating: Double, val reviewCount: Int)

class HighlightAdapter(
    private val onItemClick: (Listing) -> Unit
) : RecyclerView.Adapter<HighlightAdapter.ViewHolder>() {

    private val items = mutableListOf<HighlightItem>()

    fun submitList(list: List<HighlightItem>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    inner class ViewHolder(private val binding: ItemHighlightListingBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: HighlightItem) {
            val listing = item.listing
            binding.tvHighlightName.text = listing.name
            binding.tvHighlightCategory.text = listing.category
            binding.tvHighlightDescription.text = listing.description
            binding.tvHighlightPhotoLabel.text = "[ ${listing.name} ]"

            // Rating display
            val stars = "★".repeat(item.avgRating.toInt().coerceIn(0, 5)) +
                        "☆".repeat((5 - item.avgRating.toInt()).coerceIn(0, 5))
            binding.tvHighlightRating.text = "$stars  ${String.format("%.1f", item.avgRating)} (${item.reviewCount})"

            binding.root.setOnClickListener { onItemClick(listing) }
            
            val ownerId = listing.ownerId
            if (ownerId != null) {
                if (listing.ownerName != null) {
                    binding.tvHighlightOwnerName.text = "By ${listing.ownerName}"
                    binding.tvHighlightOwnerName.visibility = android.view.View.VISIBLE
                } else {
                    binding.tvHighlightOwnerName.visibility = android.view.View.GONE
                    binding.root.findViewTreeLifecycleOwner()?.lifecycleScope?.launch {
                        val profile = com.kapwadvo.app.data.repository.AuthRepository.getProfile(ownerId)
                        val name = profile?.fullName ?: "Unknown Owner"
                        listing.ownerName = name
                        binding.tvHighlightOwnerName.text = "By $name"
                        binding.tvHighlightOwnerName.visibility = android.view.View.VISIBLE
                    }
                }
            } else {
                binding.tvHighlightOwnerName.visibility = android.view.View.GONE
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemHighlightListingBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(items[position])
    override fun getItemCount() = items.size
}
