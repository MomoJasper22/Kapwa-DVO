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

        // Hide bottom nav
        requireActivity().findViewById<BottomNavigationView>(R.id.bottomNav)
            ?.visibility = View.GONE

        setupRecyclerView()
        setupBackButton()
        loadReservations()
    }

    private fun setupRecyclerView() {
        adapter = ReservationAdapter()
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

        lifecycleScope.launch {
            try {
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
                        val name = listingsMap[booking.listingId]?.name ?: "Unknown"
                        booking to name
                    }

                adapter.submitList(pairs)
                binding.rvReservations.visibility = View.VISIBLE

            } catch (e: Exception) {
                binding.progressBar.visibility = View.GONE
                binding.tvEmpty.visibility = View.VISIBLE
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Restore bottom nav when leaving
        requireActivity().findViewById<BottomNavigationView>(R.id.bottomNav)
            ?.visibility = View.VISIBLE
        _binding = null
    }
}
