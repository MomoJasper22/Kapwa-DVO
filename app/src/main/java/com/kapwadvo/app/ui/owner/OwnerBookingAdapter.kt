package com.kapwadvo.app.ui.owner

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.kapwadvo.app.R
import com.kapwadvo.app.data.models.Booking
import com.kapwadvo.app.data.models.Listing
import com.kapwadvo.app.data.models.Profile
import com.kapwadvo.app.databinding.ItemBookingBinding
import java.text.SimpleDateFormat
import java.util.Locale

class OwnerBookingAdapter(
    private val onItemClick: (Booking, Listing) -> Unit,
    private val onAccept: (Booking) -> Unit,
    private val onDecline: (Booking) -> Unit
) : RecyclerView.Adapter<OwnerBookingAdapter.ViewHolder>() {

    private val bookings = mutableListOf<Booking>()
    private val listingsMap = mutableMapOf<String, Listing>()
    private val profilesMap = mutableMapOf<String, Profile>()

    fun submitList(newList: List<Booking>, listings: Map<String, Listing> = emptyMap(), profiles: Map<String, Profile> = emptyMap()) {
        bookings.clear()
        bookings.addAll(newList)
        listingsMap.clear()
        listingsMap.putAll(listings)
        profilesMap.clear()
        profilesMap.putAll(profiles)
        notifyDataSetChanged()
    }

    inner class ViewHolder(private val binding: ItemBookingBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(booking: Booking) {
            val profile = profilesMap[booking.userId]
            val listing = listingsMap[booking.listingId]

            val custName = if (profile != null) "${profile.firstName} ${profile.lastName}".trim() else "Unknown Customer"
            val contact = buildString {
                if (!profile?.phoneNumber.isNullOrBlank()) append(profile?.phoneNumber)
                if (!profile?.phoneNumber.isNullOrBlank() && !profile?.email.isNullOrBlank()) append(" | ")
                if (!profile?.email.isNullOrBlank()) append(profile?.email)
            }.ifEmpty { "No contact info" }

            binding.tvCustomerName.text = custName
            binding.tvCustomerContact.text = contact

            binding.tvListingName.text = "Destination: ${listing?.name ?: "Unknown Listing"}"
            
            val parts = booking.date.split(" ")
            val dateStr = parts.getOrNull(0) ?: booking.date
            val timeStr = parts.getOrNull(1) ?: ""
            binding.tvDate.text = "Date: $dateStr at $timeStr | Guests: ${booking.guests}"
            
            binding.tvNotes.text = if (booking.notes.isNullOrEmpty()) "Notes: None" else "Notes: ${booking.notes}"
            binding.tvStatus.text = booking.status.replaceFirstChar { it.uppercase() }

            val (bg, textColor) = when (booking.status.lowercase()) {
                "accepted", "on-going" -> R.drawable.bg_badge_approved to R.color.approved_text
                "declined" -> R.drawable.bg_badge_rejected to R.color.rejected_text
                "done" -> R.drawable.bg_badge_rejected to R.color.text_secondary // Reuse greyish or another color
                else -> R.drawable.bg_badge_pending to R.color.pending_text
            }
            binding.tvStatus.setBackgroundResource(bg)
            binding.tvStatus.setTextColor(ContextCompat.getColor(binding.root.context, textColor))

            binding.root.setOnClickListener {
                if (listing != null) onItemClick(booking, listing)
            }

            // Hide actions if already actioned
            if (booking.status == "pending") {
                binding.layoutActions.visibility = android.view.View.VISIBLE
                
                // Check if date is in the past
                var isPast = false
                try {
                    val sdf = SimpleDateFormat("yyyy-MM-dd hh:mm a", Locale.getDefault())
                    val parsedDate = sdf.parse(booking.date)
                    if (parsedDate != null && parsedDate.time < System.currentTimeMillis()) {
                        isPast = true
                    }
                } catch (e: Exception) { }

                if (isPast) {
                    binding.btnAccept.visibility = android.view.View.GONE
                    // Optional: change decline button text to indicate why
                    binding.btnDecline.text = "Decline (Past Date)"
                } else {
                    binding.btnAccept.visibility = android.view.View.VISIBLE
                    binding.btnDecline.text = "Decline"
                    binding.btnAccept.setOnClickListener { onAccept(booking) }
                }

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
