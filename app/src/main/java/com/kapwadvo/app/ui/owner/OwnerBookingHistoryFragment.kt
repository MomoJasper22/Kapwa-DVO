package com.kapwadvo.app.ui.owner

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.kapwadvo.app.UserSession
import com.kapwadvo.app.data.models.Booking
import com.kapwadvo.app.data.models.Listing
import com.kapwadvo.app.data.models.Profile
import com.kapwadvo.app.data.repository.AuthRepository
import com.kapwadvo.app.data.repository.BookingRepository
import com.kapwadvo.app.data.repository.ListingRepository
import com.kapwadvo.app.databinding.FragmentOwnerBookingHistoryBinding
import com.kapwadvo.app.ui.main.explore.ListingDetailSheet
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

class OwnerBookingHistoryFragment : Fragment() {

    private var _binding: FragmentOwnerBookingHistoryBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: OwnerBookingAdapter
    private var allBookingsList = listOf<Booking>()
    private var profilesMap = mapOf<String, Profile>()
    private var listingsMap = mapOf<String, Listing>()
    private var currentFilterMode = 0 // 0 = All, 1 = On-going, 2 = Done, 3 = Declined

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentOwnerBookingHistoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        adapter = OwnerBookingAdapter(
            onItemClick = { booking, listing ->
                if (listing != null) {
                    val sheet = ListingDetailSheet.newInstance(listing, expandImmediately = false)
                    sheet.show(parentFragmentManager, "listing_detail")
                }
            },
            onAccept = { _ -> },
            onDecline = { _ -> }
        )
        binding.rvHistory.layoutManager = LinearLayoutManager(requireContext())
        binding.rvHistory.adapter = adapter

        val filterOptions = listOf("All", "On-going", "Done", "Declined")
        val spinnerAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, filterOptions)
        binding.spinnerFilter.adapter = spinnerAdapter
        binding.spinnerFilter.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                currentFilterMode = position
                applyFilter()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        load()
    }

    private var bookingsJob: kotlinx.coroutines.Job? = null

    private fun load() {
        binding.progressBar.visibility = View.VISIBLE
        binding.tvEmpty.visibility = View.GONE
        
        viewLifecycleOwner.lifecycleScope.launch {
            val ownerId = UserSession.userId ?: return@launch
            val myListings = ListingRepository.getListingsByOwner(ownerId) // Cached
            listingsMap = myListings.associateBy { it.id }
            val listingIds = myListings.map { it.id }

            // Silently sync
            BookingRepository.syncListingsBookings(listingIds)
        }
        
        viewLifecycleOwner.lifecycleScope.launch {
            val ownerId = UserSession.userId ?: return@launch
            ListingRepository.observeOwnerListings(ownerId).collect { myListings ->
                listingsMap = myListings.associateBy { it.id }
                val listingIds = myListings.map { it.id }

                bookingsJob?.cancel()
                bookingsJob = launch {
                    BookingRepository.observeBookingsForListings(listingIds).collect { fetchedBookings ->
                        // Dynamic status transformation
                        val now = System.currentTimeMillis()
                        val sdf = SimpleDateFormat("yyyy-MM-dd hh:mm a", Locale.getDefault())
                        
                        allBookingsList = fetchedBookings.filter { it.status != "pending" }.map { b ->
                            if (b.status.lowercase() == "accepted") {
                                var isPast = false
                                try {
                                    val parsed = sdf.parse(b.date)
                                    if (parsed != null && parsed.time < now) {
                                        isPast = true
                                    }
                                } catch (e: Exception) {}
                                
                                b.copy(status = if (isPast) "done" else "on-going")
                            } else {
                                b
                            }
                        }.sortedByDescending { it.date }

                        val userIds = allBookingsList.map { it.userId }.distinct()
                        
                        val profiles = AuthRepository.getProfiles(userIds)
                        profilesMap = profiles.associateBy { it.id }

                        binding.progressBar.visibility = View.GONE
                        applyFilter()
                    }
                }
            }
        }
    }

    private fun applyFilter() {
        if (allBookingsList.isEmpty()) {
            adapter.submitList(emptyList(), listingsMap, profilesMap)
            binding.tvEmpty.visibility = View.VISIBLE
            return
        }

        val filteredList = when (currentFilterMode) {
            1 -> allBookingsList.filter { it.status.lowercase() == "on-going" }
            2 -> allBookingsList.filter { it.status.lowercase() == "done" }
            3 -> allBookingsList.filter { it.status.lowercase() == "declined" }
            else -> allBookingsList
        }
        
        adapter.submitList(filteredList, listingsMap, profilesMap)
        binding.tvEmpty.visibility = if (filteredList.isEmpty()) View.VISIBLE else View.GONE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
