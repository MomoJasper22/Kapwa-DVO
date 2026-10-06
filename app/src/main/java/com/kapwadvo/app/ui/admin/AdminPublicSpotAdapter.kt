package com.kapwadvo.app.ui.admin

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.kapwadvo.app.data.models.PublicSpotRequest
import com.kapwadvo.app.databinding.ItemListingReviewBinding

class AdminPublicSpotAdapter(
    private val onAddListing: (PublicSpotRequest) -> Unit
) : ListAdapter<PublicSpotRequest, AdminPublicSpotAdapter.ViewHolder>(DIFF) {

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<PublicSpotRequest>() {
            override fun areItemsTheSame(oldItem: PublicSpotRequest, newItem: PublicSpotRequest) = oldItem.id == newItem.id
            override fun areContentsTheSame(oldItem: PublicSpotRequest, newItem: PublicSpotRequest) = oldItem == newItem
        }
    }

    inner class ViewHolder(private val b: ItemListingReviewBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(req: PublicSpotRequest) {
            b.tvName.text = req.name
            b.tvCategory.text = "Public Spot Request"
            
            val details = java.lang.StringBuilder()
            details.append("Address: ${req.address}")
            if (req.latitude != null && req.longitude != null) {
                details.append("\nLocation pinned on map")
            }
            if (!req.comments.isNullOrEmpty()) {
                details.append("\nComments: ${req.comments}")
            }
            b.tvDescription.text = details.toString()
            b.tvStatus.text = "Pending"
            b.tvUpdates.visibility = android.view.View.GONE

            b.btnApprove.text = "Add Listing"
            b.btnReject.visibility = android.view.View.GONE 

            b.btnApprove.setOnClickListener { onAddListing(req) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val b = ItemListingReviewBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(b)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
}
