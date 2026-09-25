package com.kapwadvo.app.ui.main.explore

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.kapwadvo.app.data.models.Booking
import com.kapwadvo.app.databinding.ItemReservationBinding

/**
 * Adapter for the full My Reservations list.
 * Each item is a Pair<Booking, String> where the String is the listing name.
 */
class ReservationAdapter : ListAdapter<Pair<Booking, String>, ReservationAdapter.ViewHolder>(DIFF) {

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<Pair<Booking, String>>() {
            override fun areItemsTheSame(
                oldItem: Pair<Booking, String>,
                newItem: Pair<Booking, String>
            ) = oldItem.first.id == newItem.first.id

            override fun areContentsTheSame(
                oldItem: Pair<Booking, String>,
                newItem: Pair<Booking, String>
            ) = oldItem == newItem
        }
    }

    inner class ViewHolder(private val b: ItemReservationBinding) :
        RecyclerView.ViewHolder(b.root) {

        fun bind(booking: Booking, listingName: String) {
            b.tvListingName.text = listingName
            b.tvDate.text = "Visit date: ${booking.date}"

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
                else ->
                    Triple("Pending", Color.parseColor("#FFF3CD"), Color.parseColor("#856404"))
            }
            b.tvStatus.text = label
            b.tvStatus.setBackgroundColor(bgColor)
            b.tvStatus.setTextColor(textColor)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val b = ItemReservationBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(b)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val (booking, name) = getItem(position)
        holder.bind(booking, name)
    }
}
