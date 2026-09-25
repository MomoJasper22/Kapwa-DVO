package com.kapwadvo.app.ui.main.explore

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.kapwadvo.app.R
import com.kapwadvo.app.data.models.Listing
import com.kapwadvo.app.databinding.ItemListingBinding
import androidx.lifecycle.findViewTreeLifecycleOwner
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import android.view.GestureDetector
import android.view.MotionEvent
class ListingAdapter(
    private val onItemClick: (Listing) -> Unit,
    private val onItemDoubleClick: (Listing) -> Unit,
    private val onSaveClick: (Listing, Boolean) -> Unit,
    private val isLoggedIn: Boolean
) : RecyclerView.Adapter<ListingAdapter.ViewHolder>() {

    private val listings = mutableListOf<Listing>()
    private val savedIds = mutableSetOf<String>()

    fun submitList(newList: List<Listing>) {
        listings.clear()
        listings.addAll(newList)
        notifyDataSetChanged()
    }

    fun setSavedIds(ids: Set<String>) {
        savedIds.clear()
        savedIds.addAll(ids)
        notifyDataSetChanged()
    }

    inner class ViewHolder(private val binding: ItemListingBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(listing: Listing) {
            binding.tvName.text = listing.name
            binding.tvCategory.text = listing.category
            binding.tvDescription.text = listing.description
            binding.tvPhotoLabel.text = "[ Photo: ${listing.name} ]"

            val isSaved = listing.id in savedIds
            binding.btnSave.setImageResource(
                if (isSaved) R.drawable.ic_bookmark_filled else R.drawable.ic_bookmark
            )
            binding.btnSave.setColorFilter(
                ContextCompat.getColor(
                    binding.root.context,
                    if (isSaved) R.color.primary else R.color.text_secondary
                )
            )

            if (!isLoggedIn) {
                binding.btnSave.visibility = android.view.View.GONE
            }

            val gestureDetector = GestureDetector(binding.root.context, object : GestureDetector.SimpleOnGestureListener() {
                override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                    onItemClick(listing)
                    return true
                }
                override fun onDoubleTap(e: MotionEvent): Boolean {
                    onItemDoubleClick(listing)
                    return true
                }
            })
            binding.root.setOnTouchListener { _, event ->
                gestureDetector.onTouchEvent(event)
                true
            }

            binding.btnSave.setOnClickListener { onSaveClick(listing, isSaved) }

            val ownerId = listing.ownerId
            if (ownerId != null) {
                if (listing.ownerName != null) {
                    binding.tvOwnerName.text = "By ${listing.ownerName}"
                    binding.tvOwnerName.visibility = android.view.View.VISIBLE
                } else {
                    binding.tvOwnerName.visibility = android.view.View.GONE
                    binding.root.findViewTreeLifecycleOwner()?.lifecycleScope?.launch {
                        val profile = com.kapwadvo.app.data.repository.AuthRepository.getProfile(ownerId)
                        val name = profile?.fullName ?: "Unknown Owner"
                        listing.ownerName = name
                        binding.tvOwnerName.text = "By $name"
                        binding.tvOwnerName.visibility = android.view.View.VISIBLE
                    }
                }
            } else {
                binding.tvOwnerName.visibility = android.view.View.GONE
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemListingBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(listings[position])
    override fun getItemCount() = listings.size
}
