package com.kapwadvo.app.ui.main.explore

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.kapwadvo.app.R
import com.kapwadvo.app.UserSession
import com.kapwadvo.app.data.repository.BookingRepository
import com.kapwadvo.app.data.repository.ListingRepository
import com.kapwadvo.app.databinding.FragmentMyReservationsBinding
import kotlinx.coroutines.launch

class MyReservationsFragment : Fragment() {

    private var _binding: FragmentMyReservationsBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: ReservationAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMyReservationsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupBackButton()
        loadReservations()

        parentFragmentManager.setFragmentResultListener("booking_updated", viewLifecycleOwner) { _, _ ->
            loadReservations()
        }
    }

    private fun setupRecyclerView() {
        adapter = ReservationAdapter(
            onItemClick = { booking, listing ->
                if (listing != null) {
                    val detailFragment = ReservationDetailFragment.newInstance(booking, listing)
                    parentFragmentManager.beginTransaction()
                        .replace(R.id.fragmentContainer, detailFragment)
                        .addToBackStack(null)
                        .commit()
                }
            },
            onCancelClick = { booking ->
                showCancelConfirmation(booking)
            },
            onViewClick = { booking, listing ->
                if (listing != null) {
                    val detailFragment = ReservationDetailFragment.newInstance(booking, listing)
                    parentFragmentManager.beginTransaction()
                        .replace(R.id.fragmentContainer, detailFragment)
                        .addToBackStack(null)
                        .commit()
                }
            }
        )
        binding.rvReservations.apply {
            this.adapter = this@MyReservationsFragment.adapter
            layoutManager = LinearLayoutManager(requireContext())
        }
    }

    private fun setupBackButton() {
        binding.btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }
    }

    private fun loadReservations() {
        val uid = UserSession.userId ?: return
        binding.progressBar.visibility = View.VISIBLE

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                // Background sync
                launch {
                    try {
                        BookingRepository.syncUserBookings(uid)
                        val updatedBookings = BookingRepository.getBookingsForUser(uid)
                        if (updatedBookings.isEmpty()) {
                            binding.tvEmpty.visibility = View.VISIBLE
                        } else {
                            val listingIds = updatedBookings.map { it.listingId }.distinct()
                            val listingsMap = listingIds.mapNotNull {
                                ListingRepository.getListingById(it)
                            }.associateBy { it.id }
                            val pairs = updatedBookings.sortedByDescending { it.date }.map { b -> b to listingsMap[b.listingId] }
                            adapter.submitList(pairs)
                            binding.tvEmpty.visibility = View.GONE
                        }
                    } catch (e: Exception) {}
                }

                val bookings = BookingRepository.getBookingsForUser(uid)
                binding.progressBar.visibility = View.GONE

                if (bookings.isEmpty()) {
                    binding.tvEmpty.visibility = View.VISIBLE
                    return@launch
                }

                // Resolve listing names
                val listingIds = bookings.map { it.listingId }.distinct()
                val listingsMap = listingIds.mapNotNull {
                    ListingRepository.getListingById(it)
                }.associateBy { it.id }

                val pairs = bookings
                    .sortedByDescending { it.date }
                    .map { booking ->
                        val listing = listingsMap[booking.listingId]
                        booking to listing
                    }

                adapter.submitList(pairs)
                binding.rvReservations.visibility = View.VISIBLE

            } catch (e: Exception) {
                binding.progressBar.visibility = View.GONE
                binding.tvEmpty.visibility = View.VISIBLE
            }
        }
    }

    private fun showCancelConfirmation(booking: com.kapwadvo.app.data.models.Booking) {
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Cancel Request")
            .setMessage("Are you sure you want to cancel this reservation request?")
            .setPositiveButton("Yes") { _, _ ->
                cancelBooking(booking.id, booking.listingId)
            }
            .setNegativeButton("No", null)
            .show()
    }

    private fun cancelBooking(bookingId: String, listingId: String) {
        val networkMonitor = com.kapwadvo.app.util.NetworkMonitor(requireContext())
        val isOnline = networkMonitor.isOnline.value
        networkMonitor.unregister()

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                if (isOnline) {
                    BookingRepository.updateBookingStatus(bookingId, listingId, "cancelled")
                    android.widget.Toast.makeText(context, "Reservation cancelled", android.widget.Toast.LENGTH_SHORT).show()
                } else {
                    // Update cache manually so it reflects immediately
                    val dao = com.kapwadvo.app.KapwaDVOApp.database.bookingDao()
                    val booking = dao.getBookingById(bookingId)
                    if (booking != null) {
                        dao.insert(booking.copy(status = "cancelled"))
                    }
                    
                    // Queue for sync
                    val payload = org.json.JSONObject().apply {
                        put("id", bookingId)
                        put("listingId", listingId)
                        put("status", "cancelled")
                    }.toString()
                    com.kapwadvo.app.data.repository.SyncQueueRepository.queueAction(
                        com.kapwadvo.app.UserSession.userId ?: "",
                        "CANCEL_RESERVATION",
                        payload
                    )
                    
                    android.widget.Toast.makeText(context, "Changes Made Will Apply Once Online", android.widget.Toast.LENGTH_LONG).show()
                }
                loadReservations()
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                android.widget.Toast.makeText(context, "Failed to cancel", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
