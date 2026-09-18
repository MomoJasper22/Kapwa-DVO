package com.kapwadvo.app.ui.owner

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.kapwadvo.app.R
import com.kapwadvo.app.data.models.Booking
import com.kapwadvo.app.databinding.ItemBookingBinding

class OwnerBookingAdapter(
    private val onAccept: (Booking) -> Unit,
    private val onDecline: (Booking) -> Unit
) : RecyclerView.Adapter<OwnerBookingAdapter.ViewHolder>() {

    private val bookings = mutableListOf<Booking>()
    private val listingNames = mutableMapOf<String, String>()

    fun submitList(newList: List<Booking>, names: Map<String, String> = emptyMap()) {
        bookings.clear()
        bookings.addAll(newList)
        listingNames.clear()
        listingNames.putAll(names)
        notifyDataSetChanged()
    }

    inner class ViewHolder(private val binding: ItemBookingBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(booking: Booking) {
            binding.tvListingName.text = listingNames[booking.listingId] ?: "Listing"
            binding.tvDate.text = "Date: ${booking.date}"
            binding.tvNotes.text = if (booking.notes.isNullOrEmpty()) "" else "Notes: ${booking.notes}"
            binding.tvStatus.text = booking.status.replaceFirstChar { it.uppercase() }

            val (bg, textColor) = when (booking.status) {
                "accepted" -> R.drawable.bg_badge_approved to R.color.approved_text
                "declined" -> R.drawable.bg_badge_rejected to R.color.rejected_text
                else -> R.drawable.bg_badge_pending to R.color.pending_text
            }
            binding.tvStatus.setBackgroundResource(bg)
            binding.tvStatus.setTextColor(ContextCompat.getColor(binding.root.context, textColor))

            // Hide actions if already actioned
            if (booking.status == "pending") {
                binding.layoutActions.visibility = android.view.View.VISIBLE
                binding.btnAccept.setOnClickListener { onAccept(booking) }
                binding.btnDecline.setOnClickListener { onDecline(booking) }
            } else {
                binding.layoutActions.visibility = android.view.View.GONE
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemBookingBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(bookings[position])
    override fun getItemCount() = bookings.size
}
