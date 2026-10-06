package com.kapwadvo.app.ui.owner

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.kapwadvo.app.R
import com.kapwadvo.app.data.models.Booking
import com.kapwadvo.app.data.repository.AuthRepository
import com.kapwadvo.app.data.repository.BookingRepository
import com.kapwadvo.app.databinding.FragmentBookingDetailBinding
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class BookingDetailFragment : Fragment() {

    companion object {
        private const val ARG_BOOKING_JSON = "booking_json"
        private const val ARG_LISTING_NAME = "listing_name"
        private val json = Json { ignoreUnknownKeys = true }

        fun newInstance(booking: Booking, listingName: String): BookingDetailFragment {
            return BookingDetailFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_BOOKING_JSON, json.encodeToString(booking))
                    putString(ARG_LISTING_NAME, listingName)
                }
            }
        }
    }

    private var _binding: FragmentBookingDetailBinding? = null
    private val binding get() = _binding!!
    

    private lateinit var booking: Booking
    private val listingName: String by lazy {
        arguments?.getString(ARG_LISTING_NAME) ?: "Listing"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            val jsonStr = arguments?.getString(ARG_BOOKING_JSON)
            if (jsonStr != null) {
                booking = json.decodeFromString<Booking>(jsonStr)
            }
        } catch (e: Exception) {}
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBookingDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        binding.toolbar.setNavigationOnClickListener {
            parentFragmentManager.popBackStack()
        }

        if (!::booking.isInitialized) {
            Toast.makeText(context, "Invalid booking data", Toast.LENGTH_SHORT).show()
            parentFragmentManager.popBackStack()
            return
        }

        setupBookingInfo()
        loadCustomerProfile()
        setupActions()
    }

    private fun setupBookingInfo() {
        binding.tvListingName.text = listingName
        binding.tvDate.text = "Date: ${booking.date}"
        
        if (!booking.notes.isNullOrEmpty()) {
            binding.tvNotes.visibility = View.VISIBLE
            binding.tvNotes.text = "Notes: ${booking.notes}"
        } else {
            binding.tvNotes.visibility = View.GONE
        }

        binding.tvStatus.text = booking.status.replaceFirstChar { it.uppercase() }
        val (bg, textColor) = when (booking.status) {
            "accepted" -> R.drawable.bg_badge_approved to R.color.approved_text
            "declined" -> R.drawable.bg_badge_rejected to R.color.rejected_text
            else -> R.drawable.bg_badge_pending to R.color.pending_text
        }
        binding.tvStatus.setBackgroundResource(bg)
        binding.tvStatus.setTextColor(ContextCompat.getColor(requireContext(), textColor))
    }

    private fun loadCustomerProfile() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val profile = AuthRepository.getProfile(booking.userId)
                binding.pbCustomerLoading.visibility = View.GONE
                
                if (profile != null) {
                    binding.layoutCustomerInfo.visibility = View.VISIBLE
                    
                    binding.tvCustomerName.text = profile.fullName.ifEmpty { "User" }
                    binding.tvCustomerEmail.text = "Email: ${profile.email ?: "N/A"}"
                    
                    if (!profile.phoneNumber.isNullOrBlank()) {
                        binding.tvCustomerPhone.visibility = View.VISIBLE
                        binding.tvCustomerPhone.text = "Phone: ${profile.phoneNumber}"
                    } else {
                        binding.tvCustomerPhone.visibility = View.GONE
                    }
                    
                    if (!profile.address.isNullOrBlank()) {
                        binding.tvCustomerAddress.visibility = View.VISIBLE
                        binding.tvCustomerAddress.text = "Address: ${profile.address}"
                    } else {
                        binding.tvCustomerAddress.visibility = View.GONE
                    }
                } else {
                    binding.pbCustomerLoading.visibility = View.GONE
                    Toast.makeText(context, "Could not load customer details", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                binding.pbCustomerLoading.visibility = View.GONE
                Toast.makeText(context, "Failed to load customer details", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun setupActions() {
        if (booking.status == "pending") {
            binding.layoutActions.visibility = View.VISIBLE
            
            viewLifecycleOwner.lifecycleScope.launch {
                com.kapwadvo.app.KapwaDVOApp.networkMonitor.isOnline.collect { isOnline ->
                    // Removed disabled offline behavior for buttons
                }
            }
            
            binding.btnAccept.setOnClickListener {
                updateBookingStatus("accepted")
            }
            binding.btnDecline.setOnClickListener {
                updateBookingStatus("declined")
            }
        } else {
            binding.layoutActions.visibility = View.GONE
        }
    }

    private fun updateBookingStatus(newStatus: String) {
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("${newStatus.replaceFirstChar { it.uppercase() }} Booking")
            .setMessage("Are you sure you want to $newStatus this booking request?")
            .setPositiveButton(newStatus.replaceFirstChar { it.uppercase() }) { _, _ ->
                val isOnline = com.kapwadvo.app.KapwaDVOApp.networkMonitor.isOnline.value
                viewLifecycleOwner.lifecycleScope.launch {
                    try {
                        if (isOnline) {
                            BookingRepository.updateBookingStatus(booking.id, booking.listingId, newStatus)
                            Toast.makeText(context, "Booking ${newStatus.replaceFirstChar { it.uppercase() }}", Toast.LENGTH_SHORT).show()
                        } else {
                            val dao = com.kapwadvo.app.KapwaDVOApp.database.bookingDao()
                            val cached = dao.getBookingById(booking.id)
                            if (cached != null) {
                                dao.insert(cached.copy(status = newStatus))
                            }
                            
                            val payload = org.json.JSONObject().apply {
                                put("id", booking.id)
                                put("listingId", booking.listingId)
                                put("status", newStatus)
                            }.toString()
                            
                            com.kapwadvo.app.data.repository.SyncQueueRepository.queueAction(
                                com.kapwadvo.app.UserSession.userId ?: "",
                                "UPDATE_RESERVATION_STATUS",
                                payload
                            )
                            Toast.makeText(context, "Changes Made Will Apply Once Online", Toast.LENGTH_LONG).show()
                        }
                        parentFragmentManager.popBackStack() // Go back to the list
                    } catch (e: Exception) {
                        if (e is kotlinx.coroutines.CancellationException) throw e
                        Toast.makeText(context, "Failed to update booking. Please try again", Toast.LENGTH_LONG).show()
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
