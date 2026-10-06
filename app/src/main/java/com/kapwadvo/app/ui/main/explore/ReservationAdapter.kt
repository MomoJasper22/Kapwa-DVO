package com.kapwadvo.app.ui.main.explore

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.kapwadvo.app.data.models.Booking
import com.kapwadvo.app.data.models.Listing
import com.kapwadvo.app.databinding.ItemReservationBinding

/**
 * Adapter for the full My Reservations list.
 * Each item is a Pair<Booking, Listing?>.
 */
class ReservationAdapter(
    private val onItemClick: (Booking, Listing?) -> Unit,
    private val onCancelClick: (Booking) -> Unit,
    private val onViewClick: (Booking, Listing?) -> Unit
) : ListAdapter<Pair<Booking, Listing?>, ReservationAdapter.ViewHolder>(DIFF) {

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<Pair<Booking, Listing?>>() {
            override fun areItemsTheSame(
                oldItem: Pair<Booking, Listing?>,
                newItem: Pair<Booking, Listing?>
            ) = oldItem.first.id == newItem.first.id

            override fun areContentsTheSame(
                oldItem: Pair<Booking, Listing?>,
                newItem: Pair<Booking, Listing?>
            ) = oldItem == newItem
        }
    }

    inner class ViewHolder(private val b: ItemReservationBinding) :
        RecyclerView.ViewHolder(b.root) {

        fun bind(booking: Booking, listing: Listing?) {
            b.tvListingName.text = listing?.name ?: "Unknown"
            
            val parts = booking.date.split(" ")
            val dateStr = parts.getOrNull(0) ?: booking.date
            val timeStr = parts.getOrNull(1) ?: ""
            b.tvDate.text = "Visit: $dateStr at $timeStr | ${booking.guests} Guest(s)"

            if (!booking.notes.isNullOrBlank()) {
                b.tvNotes.text = booking.notes
                b.tvNotes.visibility = ViewGroup.VISIBLE
            } else {
                b.tvNotes.visibility = ViewGroup.GONE
            }

            val (label, bgColor, textColor) = when (booking.status.lowercase()) {
                "confirmed", "accepted" ->
                    Triple("Accepted", Color.parseColor("#D4EDDA"), Color.parseColor("#155724"))
                "declined", "rejected" ->
                    Triple("Declined", Color.parseColor("#F8D7DA"), Color.parseColor("#721C24"))
                "cancelled" ->
                    Triple("Cancelled", Color.parseColor("#E2E3E5"), Color.parseColor("#383D41"))
                else ->
                    Triple("Pending", Color.parseColor("#FFF3CD"), Color.parseColor("#856404"))
            }
            b.tvStatus.text = label
            b.tvStatus.setBackgroundColor(bgColor)
            b.tvStatus.setTextColor(textColor)

            if (booking.status.lowercase() == "pending") {
                b.layoutActions.visibility = android.view.View.VISIBLE
                b.btnCancel.setOnClickListener { onCancelClick(booking) }
                b.btnView.setOnClickListener { onViewClick(booking, listing) }
            } else {
                b.layoutActions.visibility = android.view.View.GONE
            }

            b.root.setOnClickListener {
                onItemClick(booking, listing)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val b = ItemReservationBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(b)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val (booking, listing) = getItem(position)
        holder.bind(booking, listing)
    }
}
