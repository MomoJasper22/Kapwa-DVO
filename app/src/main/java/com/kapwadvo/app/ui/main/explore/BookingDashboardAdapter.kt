package com.kapwadvo.app.ui.main.explore

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.kapwadvo.app.data.models.Booking
import com.kapwadvo.app.databinding.ItemBookingDashboardBinding

class BookingDashboardAdapter(
    private val onItemClick: (Booking) -> Unit = {}
) : RecyclerView.Adapter<BookingDashboardAdapter.ViewHolder>() {

    private val items = mutableListOf<Pair<Booking, String>>() // Booking + listing name

    fun submitList(list: List<Pair<Booking, String>>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    inner class ViewHolder(private val binding: ItemBookingDashboardBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(booking: Booking, listingName: String) {
            binding.tvBookingListingName.text = listingName
            binding.tvBookingDate.text = "📅 ${booking.date}"

            val (text, bgColor, textColor) = when (booking.status) {
                "confirmed", "accepted" -> Triple("Confirmed", Color.parseColor("#D4EDDA"), Color.parseColor("#155724"))
                "declined" -> Triple("Declined", Color.parseColor("#F8D7DA"), Color.parseColor("#721C24"))
                else -> Triple("Pending", Color.parseColor("#FFF3CD"), Color.parseColor("#856404"))
            }
            binding.tvBookingStatus.text = text
            binding.tvBookingStatus.setBackgroundColor(bgColor)
            binding.tvBookingStatus.setTextColor(textColor)

            binding.root.setOnClickListener { onItemClick(booking) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemBookingDashboardBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val (booking, name) = items[position]
        holder.bind(booking, name)
    }

    override fun getItemCount() = items.size
}
