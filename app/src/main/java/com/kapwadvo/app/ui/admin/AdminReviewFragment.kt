package com.kapwadvo.app.ui.admin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.kapwadvo.app.data.models.ListingInsert
import com.kapwadvo.app.data.models.Listing
import com.kapwadvo.app.data.models.PublicSpotRequest
import com.kapwadvo.app.data.repository.ListingRepository
import com.kapwadvo.app.data.repository.PublicSpotRepository
import com.kapwadvo.app.databinding.FragmentAdminReviewBinding
import com.kapwadvo.app.UserSession
import kotlinx.coroutines.launch
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonPrimitive
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import com.kapwadvo.app.supabase
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import io.github.jan.supabase.realtime.realtime
import io.github.jan.supabase.realtime.RealtimeChannel

class AdminReviewFragment : Fragment() {

    private var _binding: FragmentAdminReviewBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapterListings: AdminReviewAdapter
    private lateinit var adapterPublicSpots: AdminPublicSpotAdapter
    private var realtimeChannel: RealtimeChannel? = null
    
    private var currentMode = 0 // 0 = Listings, 1 = Public Spots

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentAdminReviewBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        adapterListings = AdminReviewAdapter(
            onItemClick = { listing ->
                val sheet = AdminListingDetailSheet.newInstance(listing)
                sheet.show(childFragmentManager, "admin_listing_detail")
            },
            onApprove = { listing ->
                handleApproveListing(listing)
            },
            onReject = { listing ->
                handleRejectListing(listing)
            }
        )

        adapterPublicSpots = AdminPublicSpotAdapter(
            onAddListing = { spotReq ->
                // Mark spot req as added, then route to ListingFormFragment pre-populated
                lifecycleScope.launch {
                    try {
                        PublicSpotRepository.updateRequestStatus(spotReq.id, "added")
                        
                        val initialListing = Listing(
                            id = "",
                            ownerId = UserSession.userId,
                            name = spotReq.name,
                            address = spotReq.address ?: "",
                            lat = spotReq.latitude,
                            lng = spotReq.longitude,
                            description = spotReq.comments ?: "",
                            photoUrls = if (spotReq.photoUrl != null) listOf(spotReq.photoUrl) else emptyList()
                        )
                        
                        val formFragment = ListingFormFragment.newInstance(initialListing)
                        requireActivity().supportFragmentManager.beginTransaction()
                            .replace(com.kapwadvo.app.R.id.fragmentContainer, formFragment, "listing_form")
                            .addToBackStack(null)
                            .commit()
                            
                    } catch (e: Exception) {
                        Toast.makeText(context, "Failed to transition", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        )
        
        binding.rvReview.layoutManager = LinearLayoutManager(requireContext())
        binding.rvReview.adapter = adapterListings

        binding.toggleGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                when (checkedId) {
                    com.kapwadvo.app.R.id.btnListings -> {
                        currentMode = 0
                        binding.rvReview.adapter = adapterListings
                        load()
                    }
                    com.kapwadvo.app.R.id.btnPublicSpots -> {
                        currentMode = 1
                        binding.rvReview.adapter = adapterPublicSpots
                        load()
                    }
                }
            }
        }

        load()
        setupRealtime()
    }
    
    private fun handleApproveListing(listing: Listing) {
        lifecycleScope.launch {
            try {
                if (listing.pendingUpdates != null) {
                    val updates = listing.pendingUpdates
                    val insert = ListingInsert(
                        ownerId = listing.ownerId ?: "",
                        name = updates["name"]?.jsonPrimitive?.content ?: listing.name,
                        description = updates["description"]?.jsonPrimitive?.content ?: listing.description,
                        category = updates["category"]?.jsonPrimitive?.content ?: listing.category,
                        address = updates["address"]?.jsonPrimitive?.content ?: listing.address,
                        hours = updates["hours"]?.jsonPrimitive?.content ?: listing.hours,
                        contact = updates["contact"]?.jsonPrimitive?.content ?: listing.contact,
                        lat = updates["lat"]?.jsonPrimitive?.doubleOrNull ?: listing.lat,
                        lng = updates["lng"]?.jsonPrimitive?.doubleOrNull ?: listing.lng,
                        status = "approved",
                        pendingUpdates = null
                    )
                    ListingRepository.updateListing(listing.id, insert)
                } else {
                    ListingRepository.updateStatus(listing.id, "approved")
                }
                Toast.makeText(context, "Approved", Toast.LENGTH_SHORT).show()
                load()
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to update status. Please try again", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun handleRejectListing(listing: Listing) {
        lifecycleScope.launch {
            try {
                if (listing.pendingUpdates != null) {
                    val insert = ListingInsert(
                        ownerId = listing.ownerId ?: "",
                        name = listing.name,
                        description = listing.description,
                        category = listing.category,
                        address = listing.address,
                        hours = listing.hours,
                        contact = listing.contact,
                        lat = listing.lat,
                        lng = listing.lng,
                        status = "approved",
                        pendingUpdates = null
                    )
                    ListingRepository.updateListing(listing.id, insert)
                } else {
                    ListingRepository.updateStatus(listing.id, "rejected")
                }
                Toast.makeText(context, "Rejected", Toast.LENGTH_SHORT).show()
                load()
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to update status. Please try again", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun setupRealtime() {
        realtimeChannel = supabase.channel("admin_review_listings")
        val changes = realtimeChannel!!.postgresChangeFlow<PostgresAction>("public") { table = "listings" }
        
        changes.onEach {
            load()
        }.launchIn(lifecycleScope)
        
        lifecycleScope.launch {
            try {
                supabase.realtime.connect()
                realtimeChannel!!.subscribe()
            } catch (e: Exception) {
                // Ignore connection errors if already connected
            }
        }
    }

    private fun load() {
        binding.progressBar.visibility = View.VISIBLE
        binding.tvEmpty.visibility = View.GONE
        lifecycleScope.launch {
            if (currentMode == 0) {
                // Load Listings
                val pendingListings = ListingRepository.getPendingListings()
                val updateRequests = ListingRepository.getPendingUpdatesListings()
                val combined = (pendingListings + updateRequests).distinctBy { it.id }
                binding.progressBar.visibility = View.GONE
                adapterListings.submitList(combined)
                binding.tvEmpty.visibility = if (combined.isEmpty()) View.VISIBLE else View.GONE
            } else {
                // Load Public Spots
                val pendingSpots = PublicSpotRepository.getPendingRequests()
                binding.progressBar.visibility = View.GONE
                adapterPublicSpots.submitList(pendingSpots)
                binding.tvEmpty.visibility = if (pendingSpots.isEmpty()) View.VISIBLE else View.GONE
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        lifecycleScope.launch {
            try {
                realtimeChannel?.unsubscribe()
            } catch (e: Exception) { }
        }
        _binding = null
    }
}
