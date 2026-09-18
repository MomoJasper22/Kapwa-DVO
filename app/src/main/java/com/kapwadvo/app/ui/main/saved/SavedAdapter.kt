package com.kapwadvo.app.ui.main.saved

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.kapwadvo.app.data.models.Listing
import com.kapwadvo.app.databinding.ItemSavedListingBinding

class SavedAdapter(
    private val onItemClick: (Listing) -> Unit,
    private val onRemoveClick: (Listing) -> Unit
) : RecyclerView.Adapter<SavedAdapter.ViewHolder>() {

    private val listings = mutableListOf<Listing>()

    fun submitList(newList: List<Listing>) {
        listings.clear()
        listings.addAll(newList)
        notifyDataSetChanged()
    }

    inner class ViewHolder(private val binding: ItemSavedListingBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(listing: Listing) {
            binding.tvName.text = listing.name
            binding.tvCategory.text = listing.category
            binding.tvPhotoLabel.text = "[ Photo: ${listing.name} ]"
            binding.root.setOnClickListener { onItemClick(listing) }
            binding.btnRemove.setOnClickListener { onRemoveClick(listing) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemSavedListingBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(listings[position])
    override fun getItemCount() = listings.size
}
