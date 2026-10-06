package com.kapwadvo.app.ui.owner

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.kapwadvo.app.R
import com.kapwadvo.app.data.models.Booking
import com.kapwadvo.app.data.models.Listing
import com.kapwadvo.app.data.models.Profile
import com.kapwadvo.app.data.repository.AuthRepository
import com.kapwadvo.app.data.repository.BookingRepository
import com.kapwadvo.app.data.repository.ListingRepository
import com.kapwadvo.app.databinding.FragmentOwnerBookingsBinding
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

class OwnerBookingsFragment : Fragment() {

    private var _binding: FragmentOwnerBookingsBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: OwnerBookingAdapter
    private var allBookingsList = listOf<Booking>()
    private var profilesMap = mapOf<String, Profile>()
    private var listingsMap = mapOf<String, Listing>()
    private var currentSortMode = 0 // 0 = Upcoming First, 1 = Furthest First

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentOwnerBookingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        adapter = OwnerBookingAdapter(
            onItemClick = { booking, listing ->
                val sheet = com.kapwadvo.app.ui.main.explore.ListingDetailSheet.newInstance(listing)
                sheet.show(parentFragmentManager, "ListingDetailSheet")
            },
            onAccept = { booking ->
                androidx.appcompat.app.AlertDialog.Builder(requireContext())
                    .setTitle("Accept Booking")
                    .setMessage("Are you sure you want to accept this booking request?\n\nDate: ${booking.date}")
                    .setPositiveButton("Accept") { _, _ ->
                        viewLifecycleOwner.lifecycleScope.launch {
                            val b = _binding ?: return@launch
                            val ctx = context ?: return@launch
                            try {
                                BookingRepository.updateBookingStatus(booking.id, booking.listingId, "accepted")
                                Toast.makeText(ctx, "Booking accepted", Toast.LENGTH_SHORT).show()
                            } catch (e: Exception) { if (e is kotlinx.coroutines.CancellationException) throw e;
                                Toast.makeText(ctx, "Failed to update booking. Please try again", Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            },
            onDecline = { booking ->
                val inputLayout = android.widget.FrameLayout(requireContext())
                inputLayout.setPadding(50, 20, 50, 0)
                val input = android.widget.EditText(requireContext()).apply {
                    hint = "Optional reason for declining..."
                    inputType = android.text.InputType.TYPE_CLASS_TEXT
                }
                inputLayout.addView(input)

                androidx.appcompat.app.AlertDialog.Builder(requireContext())
                    .setTitle("Decline Booking")
                    .setMessage("Are you sure you want to decline this booking request?\n\nDate: ${booking.date}")
                    .setView(inputLayout)
                    .setPositiveButton("Decline") { _, _ ->
                        val reason = input.text.toString().trim()
                        viewLifecycleOwner.lifecycleScope.launch {
                            val b = _binding ?: return@launch
                            val ctx = context ?: return@launch
                            try {
                                BookingRepository.updateBookingStatus(
                                    booking.id,
                                    booking.listingId,
                                    "declined",
                                    if (reason.isNotEmpty()) reason else null
                                )
                                Toast.makeText(ctx, "Booking declined", Toast.LENGTH_SHORT).show()
                            } catch (e: Exception) { if (e is kotlinx.coroutines.CancellationException) throw e;
                                Toast.makeText(ctx, "Failed to update booking. Please try again", Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            }
        )
        binding.rvBookings.layoutManager = LinearLayoutManager(requireContext())
        binding.rvBookings.adapter = adapter

        val sortOptions = listOf("Upcoming First", "Furthest First")
        val spinnerAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, sortOptions)
        binding.spinnerSort.adapter = spinnerAdapter
        binding.spinnerSort.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                currentSortMode = position
                applySorting()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        binding.cardBookingSummary.root.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, OwnerBookingHistoryFragment())
                .addToBackStack(null)
                .commit()
        }

        load()
    }

    private var bookingsJob: kotlinx.coroutines.Job? = null

    private fun load() {
        binding.progressBar.visibility = View.VISIBLE
        binding.tvEmpty.visibility = View.GONE
        
        // Removed the initial sync call using the potentially empty Room cache, 
        // as we will sync bookings dynamically when observeOwnerListings emits updated listings.
        viewLifecycleOwner.lifecycleScope.launch {
            val ownerId = com.kapwadvo.app.UserSession.userId ?: return@launch
            ListingRepository.observeOwnerListings(ownerId).collect { myListings ->
                val b = _binding ?: return@collect
                listingsMap = myListings.associateBy { it.id }
                val listingIds = myListings.map { it.id }
                
                // Trigger background sync silently for the updated listings
                if (listingIds.isNotEmpty()) {
                    launch {
                        try {
                            BookingRepository.syncListingsBookings(listingIds)
                        } catch (e: Exception) {
                            if (e is kotlinx.coroutines.CancellationException) throw e
                            // Ignored
                        }
                    }
                }
                
                bookingsJob?.cancel()
                bookingsJob = launch {
                    BookingRepository.observeBookingsForListings(listingIds).collect { bookings ->
                        val cb = _binding ?: return@collect
                        allBookingsList = bookings
                        val userIds = bookings.map { it.userId }.distinct()
                        
                        // Note: In offline mode, getProfiles may return empty, which is acceptable
                        val profiles = AuthRepository.getProfiles(userIds)
                        profilesMap = profiles.associateBy { it.id }

                        cb.progressBar.visibility = View.GONE
                        updateDashboard()
                        applySorting()
                        cb.tvEmpty.visibility = if (bookings.none { it.status == "pending" }) View.VISIBLE else View.GONE
                    }
                }
            }
        }
    }

    private fun updateDashboard() {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        val now = System.currentTimeMillis()

        var pendingCount = 0
        var ongoingCount = 0
        var doneCount = 0

        for (b in allBookingsList) {
            when (b.status.lowercase()) {
                "pending" -> pendingCount++
                "declined", "cancelled", "rejected" -> doneCount++
                "accepted", "confirmed" -> {
                    try {
                        val parts = b.date.split(" ")
                        val dateStr = parts.getOrNull(0) ?: ""
                        val timeStr = parts.getOrNull(1) ?: ""
                        
                        // Convert AM/PM to 24h format for parsing, or handle it properly.
                        // Assuming booking date string is stored as "yyyy-MM-dd hh:mm a"
                        val parsedDate = SimpleDateFormat("yyyy-MM-dd hh:mm a", Locale.getDefault()).parse(b.date)
                        
                        if (parsedDate != null && parsedDate.time > now) {
                            ongoingCount++
                        } else {
                            doneCount++
                        }
                    } catch (e: Exception) { if (e is kotlinx.coroutines.CancellationException) throw e;
                        // Fallback parsing or assume done if we can't parse
                        doneCount++
                    }
                }
            }
        }

        val card = binding.cardBookingSummary
        val tvPending = card.root.findViewById<android.widget.TextView>(R.id.tvPendingCount)
        val tvOngoing = card.root.findViewById<android.widget.TextView>(R.id.tvConfirmedCount)
        val tvDone = card.root.findViewById<android.widget.TextView>(R.id.tvPastCount)
        
        // Also update labels if needed
        val parentOngoing = tvOngoing.parent as android.view.ViewGroup
        val labelOngoing = parentOngoing.getChildAt(1) as? android.widget.TextView
        labelOngoing?.text = "On-going"

        tvPending?.text = pendingCount.toString()
        tvOngoing?.text = ongoingCount.toString()
        tvDone?.text = doneCount.toString()
    }

    private fun applySorting() {
        val pendingList = allBookingsList.filter { it.status == "pending" }
        if (pendingList.isEmpty()) {
            adapter.submitList(emptyList(), listingsMap, profilesMap)
            return
        }

        val sorted = if (currentSortMode == 0) {
            pendingList.sortedBy { it.date }
        } else {
            pendingList.sortedByDescending { it.date }
        }
        adapter.submitList(sorted, listingsMap, profilesMap)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
