package com.kapwadvo.app.ui.owner

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.kapwadvo.app.UserSession
import com.kapwadvo.app.data.repository.BookingRepository
import com.kapwadvo.app.data.repository.ListingRepository
import com.kapwadvo.app.databinding.FragmentOwnerBookingsBinding
import kotlinx.coroutines.launch

class OwnerBookingsFragment : Fragment() {

    private var _binding: FragmentOwnerBookingsBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: OwnerBookingAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentOwnerBookingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        adapter = OwnerBookingAdapter(
            onAccept = { booking ->
                lifecycleScope.launch {
                    try {
                        BookingRepository.updateBookingStatus(booking.id, "accepted")
                        Toast.makeText(context, "Booking accepted", Toast.LENGTH_SHORT).show()
                        load()
                    } catch (e: Exception) {
                        Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            },
            onDecline = { booking ->
                lifecycleScope.launch {
                    try {
                        BookingRepository.updateBookingStatus(booking.id, "declined")
                        Toast.makeText(context, "Booking declined", Toast.LENGTH_SHORT).show()
                        load()
                    } catch (e: Exception) {
                        Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        )
        binding.rvBookings.layoutManager = LinearLayoutManager(requireContext())
        binding.rvBookings.adapter = adapter
        load()
    }

    private fun load() {
        binding.progressBar.visibility = View.VISIBLE
        binding.tvEmpty.visibility = View.GONE
        lifecycleScope.launch {
            val ownerId = UserSession.userId ?: return@launch
            val myListings = ListingRepository.getListingsByOwner(ownerId)
            val listingNames = myListings.associate { it.id to it.name }

            val allBookings = myListings.flatMap { listing ->
                BookingRepository.getBookingsForListing(listing.id)
            }

            binding.progressBar.visibility = View.GONE
            adapter.submitList(allBookings, listingNames)
            binding.tvEmpty.visibility = if (allBookings.isEmpty()) View.VISIBLE else View.GONE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
