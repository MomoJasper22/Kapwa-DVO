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
import com.kapwadvo.app.data.repository.ListingRepository
import com.kapwadvo.app.databinding.FragmentAdminReviewBinding
import kotlinx.coroutines.launch
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonPrimitive

class AdminReviewFragment : Fragment() {

    private var _binding: FragmentAdminReviewBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: AdminReviewAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentAdminReviewBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        adapter = AdminReviewAdapter(
            onApprove = { listing ->
                lifecycleScope.launch {
                    try {
                        if (listing.pendingUpdates != null) {
                            // It's an update request: merge pending updates into the listing
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
                                pendingUpdates = null // Clear pending updates
                            )
                            ListingRepository.updateListing(listing.id, insert)
                        } else {
                            // It's a new listing
                            ListingRepository.updateStatus(listing.id, "approved")
                        }
                        Toast.makeText(context, "Approved!", Toast.LENGTH_SHORT).show()
                        load()
                    } catch (e: Exception) {
                        Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            },
            onReject = { listing ->
                lifecycleScope.launch {
                    try {
                        if (listing.pendingUpdates != null) {
                            // Reject update request: clear pending updates but keep listing approved
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
                            // Reject new listing: just update status (or delete, but status="rejected" is safer)
                            ListingRepository.updateStatus(listing.id, "rejected")
                        }
                        Toast.makeText(context, "Rejected", Toast.LENGTH_SHORT).show()
                        load()
                    } catch (e: Exception) {
                        Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        )
        binding.rvReview.layoutManager = LinearLayoutManager(requireContext())
        binding.rvReview.adapter = adapter
        load()
    }

    private fun load() {
        binding.progressBar.visibility = View.VISIBLE
        binding.tvEmpty.visibility = View.GONE
        lifecycleScope.launch {
            val pendingListings = ListingRepository.getPendingListings()
            val updateRequests = ListingRepository.getPendingUpdatesListings()
            
            // Combine both lists. Use distinctBy to avoid duplicates just in case (though unlikely a pending listing also has pending updates)
            val combined = (pendingListings + updateRequests).distinctBy { it.id }
            
            binding.progressBar.visibility = View.GONE
            adapter.submitList(combined)
            binding.tvEmpty.visibility = if (combined.isEmpty()) View.VISIBLE else View.GONE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
