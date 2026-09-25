package com.kapwadvo.app.ui.main.explore

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.kapwadvo.app.R
import com.kapwadvo.app.UserSession
import com.kapwadvo.app.data.repository.BookingRepository
import com.kapwadvo.app.data.repository.ListingRepository
import com.kapwadvo.app.data.repository.ReviewRepository
import com.kapwadvo.app.databinding.FragmentExploreBinding
import com.kapwadvo.app.databinding.LayoutBookingSummaryCardBinding
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.Calendar

class ExploreFragment : Fragment() {

    private var _binding: FragmentExploreBinding? = null
    private val binding get() = _binding!!

    private lateinit var highlightAdapter: HighlightAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentExploreBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupGreeting()
        setupHighlightsRecyclerView()
        loadData()
    }

    // ── Greeting ─────────────────────────────────────────────────────────────

    private fun setupGreeting() {
        if (UserSession.isLoggedIn() && UserSession.firstName.isNotEmpty()) {
            val timeGreeting = timeOfDayGreeting()
            binding.tvGreeting.text = "$timeGreeting, ${UserSession.firstName}!"
        } else if (UserSession.isGuest) {
            binding.tvGreeting.text = getString(R.string.greeting_guest)
        } else {
            binding.tvGreeting.text = "${timeOfDayGreeting()}!"
        }
        binding.tvGreetingSub.text = "Where are you exploring today?"
    }

    private fun timeOfDayGreeting(): String {
        return when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
            in 0..11  -> getString(R.string.greeting_morning)
            in 12..17 -> getString(R.string.greeting_afternoon)
            else      -> getString(R.string.greeting_evening)
        }
    }

    // ── RecyclerViews ─────────────────────────────────────────────────────────

    private fun setupHighlightsRecyclerView() {
        highlightAdapter = HighlightAdapter { listing ->
            val sheet = ListingDetailSheet.newInstance(listing)
            sheet.show(parentFragmentManager, "ListingDetailSheet")
        }
        binding.rvHighlights.apply {
            adapter = highlightAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }
    }

    // ── Data loading ──────────────────────────────────────────────────────────

    private fun loadData() {
        loadBookingSummary()
        loadHighlights()
    }

    private fun loadBookingSummary() {
        val uid = UserSession.userId
        if (!UserSession.isLoggedIn() || uid == null) {
            binding.tvBookingsPlaceholder.visibility = View.VISIBLE
            return
        }

        binding.progressBookings.visibility = View.VISIBLE

        lifecycleScope.launch {
            try {
                val bookings = BookingRepository.getBookingsForUser(uid)
                binding.progressBookings.visibility = View.GONE

                val today = try { LocalDate.now().toString() } catch (e: Exception) { "" }

                val pending   = bookings.count { it.status.lowercase() == "pending" }
                val confirmed = bookings.count {
                    it.status.lowercase() in listOf("confirmed", "accepted")
                }
                val past      = bookings.count {
                    it.status.lowercase() in listOf("declined", "rejected") ||
                    (today.isNotEmpty() && it.date < today)
                }

                // Bind into the included card's views
                val cardBinding = LayoutBookingSummaryCardBinding.bind(
                    binding.bookingSummaryCard.root
                )
                cardBinding.tvPendingCount.text   = pending.toString()
                cardBinding.tvConfirmedCount.text = confirmed.toString()
                cardBinding.tvPastCount.text      = past.toString()

                binding.bookingSummaryCard.root.visibility = View.VISIBLE

                // Navigate to My Reservations on click
                binding.bookingSummaryCard.root.setOnClickListener {
                    parentFragmentManager.beginTransaction()
                        .replace(R.id.fragmentContainer, MyReservationsFragment())
                        .addToBackStack(null)
                        .commit()
                }

            } catch (e: Exception) {
                binding.progressBookings.visibility = View.GONE
                binding.tvBookingsPlaceholder.text = getString(R.string.no_bookings)
                binding.tvBookingsPlaceholder.visibility = View.VISIBLE
            }
        }
    }

    private fun loadHighlights() {
        binding.progressHighlights.visibility = View.VISIBLE

        lifecycleScope.launch {
            try {
                val listings = ListingRepository.getApprovedListings()

                // Fetch reviews for all listings concurrently
                val reviewJobs = listings.map { listing ->
                    async {
                        val reviews = ReviewRepository.getReviewsForListing(listing.id)
                        val avg = if (reviews.isEmpty()) 0.0
                                  else reviews.map { it.rating }.average()
                        HighlightItem(listing, avg, reviews.size)
                    }
                }
                val highlighted = reviewJobs.map { it.await() }
                    .filter { it.reviewCount > 0 }               // only listings with reviews
                    .sortedByDescending { it.avgRating }
                    .take(10)

                // If no reviewed listings, show top 10 by name as fallback
                val finalList = if (highlighted.isEmpty()) {
                    listings.take(10).map { HighlightItem(it, 0.0, 0) }
                } else highlighted

                binding.progressHighlights.visibility = View.GONE
                if (finalList.isEmpty()) {
                    binding.tvHighlightsEmpty.visibility = View.VISIBLE
                } else {
                    highlightAdapter.submitList(finalList)
                    binding.rvHighlights.visibility = View.VISIBLE
                }
            } catch (e: Exception) {
                binding.progressHighlights.visibility = View.GONE
                binding.tvHighlightsEmpty.visibility = View.VISIBLE
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
