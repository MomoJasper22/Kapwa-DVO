package com.kapwadvo.app.ui.admin

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.kapwadvo.app.data.models.Listing
import com.kapwadvo.app.databinding.ItemListingReviewBinding

class AdminReviewAdapter(
    private val onApprove: (Listing) -> Unit,
    private val onReject: (Listing) -> Unit
) : RecyclerView.Adapter<AdminReviewAdapter.ViewHolder>() {

    private val listings = mutableListOf<Listing>()

    fun submitList(newList: List<Listing>) {
        listings.clear()
        listings.addAll(newList)
        notifyDataSetChanged()
    }

    inner class ViewHolder(private val binding: ItemListingReviewBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(listing: Listing) {
            binding.tvName.text = listing.name
            binding.tvCategory.text = listing.category
            binding.tvDescription.text = listing.description
            binding.tvStatus.text = "Pending"
            binding.tvStatus.setBackgroundResource(com.kapwadvo.app.R.drawable.bg_badge_pending)

            binding.btnApprove.setOnClickListener { onApprove(listing) }
            binding.btnReject.setOnClickListener { onReject(listing) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemListingReviewBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(listings[position])
    override fun getItemCount() = listings.size
}
