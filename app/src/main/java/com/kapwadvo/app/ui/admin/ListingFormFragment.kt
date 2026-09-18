package com.kapwadvo.app.ui.admin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.kapwadvo.app.UserSession
import com.kapwadvo.app.data.models.Listing
import com.kapwadvo.app.data.models.ListingInsert
import com.kapwadvo.app.data.repository.ListingRepository
import com.kapwadvo.app.databinding.FragmentListingFormBinding
import kotlinx.coroutines.launch

class ListingFormFragment : Fragment() {

    private var _binding: FragmentListingFormBinding? = null
    private val binding get() = _binding!!

    private var editingListing: Listing? = null

    companion object {
        private const val ARG_LISTING_ID = "listing_id"
        private const val ARG_LISTING_NAME = "listing_name"
        private const val ARG_LISTING_DESC = "listing_desc"
        private const val ARG_LISTING_CAT = "listing_cat"
        private const val ARG_LISTING_ADDR = "listing_addr"
        private const val ARG_LISTING_HOURS = "listing_hours"
        private const val ARG_LISTING_CONTACT = "listing_contact"
        private const val ARG_LISTING_LAT = "listing_lat"
        private const val ARG_LISTING_LNG = "listing_lng"
        private const val ARG_LISTING_STATUS = "listing_status"

        fun newInstance(listing: Listing?): ListingFormFragment {
            return ListingFormFragment().apply {
                if (listing != null) {
                    arguments = Bundle().apply {
                        putString(ARG_LISTING_ID, listing.id)
                        putString(ARG_LISTING_NAME, listing.name)
                        putString(ARG_LISTING_DESC, listing.description)
                        putString(ARG_LISTING_CAT, listing.category)
                        putString(ARG_LISTING_ADDR, listing.address)
                        putString(ARG_LISTING_HOURS, listing.hours)
                        putString(ARG_LISTING_CONTACT, listing.contact)
                        listing.lat?.let { putDouble(ARG_LISTING_LAT, it) }
                        listing.lng?.let { putDouble(ARG_LISTING_LNG, it) }
                        putString(ARG_LISTING_STATUS, listing.status)
                    }
                }
            }
        }
    }

    private val categories = listOf("Tourist Spot", "Business", "Hidden Gem", "Food", "Accommodation")

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentListingFormBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val catAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, categories)
        binding.spinnerCategory.adapter = catAdapter

        arguments?.getString(ARG_LISTING_ID)?.let { id ->
            editingListing = Listing(
                id = id,
                name = arguments?.getString(ARG_LISTING_NAME) ?: "",
                description = arguments?.getString(ARG_LISTING_DESC) ?: "",
                category = arguments?.getString(ARG_LISTING_CAT) ?: "",
                address = arguments?.getString(ARG_LISTING_ADDR),
                hours = arguments?.getString(ARG_LISTING_HOURS),
                contact = arguments?.getString(ARG_LISTING_CONTACT),
                lat = if (arguments?.containsKey(ARG_LISTING_LAT) == true) arguments?.getDouble(ARG_LISTING_LAT) else null,
                lng = if (arguments?.containsKey(ARG_LISTING_LNG) == true) arguments?.getDouble(ARG_LISTING_LNG) else null,
                status = arguments?.getString(ARG_LISTING_STATUS) ?: "pending"
            )
            binding.tvFormTitle.text = "Edit Listing"
            binding.etName.setText(editingListing?.name)
            binding.etDescription.setText(editingListing?.description)
            binding.etAddress.setText(editingListing?.address)
            binding.etHours.setText(editingListing?.hours)
            binding.etContact.setText(editingListing?.contact)
            binding.etLat.setText(editingListing?.lat?.toString() ?: "")
            binding.etLng.setText(editingListing?.lng?.toString() ?: "")
            val catIdx = categories.indexOf(editingListing?.category)
            if (catIdx >= 0) binding.spinnerCategory.setSelection(catIdx)
        }

        binding.btnPickPin.setOnClickListener {
            Toast.makeText(context, "Enter lat/lng manually for now", Toast.LENGTH_SHORT).show()
        }

        binding.btnSave.setOnClickListener { saveListing() }
    }

    private fun saveListing() {
        val name = binding.etName.text.toString().trim()
        val desc = binding.etDescription.text.toString().trim()
        val cat = binding.spinnerCategory.selectedItem.toString()
        val addr = binding.etAddress.text.toString().trim().ifEmpty { null }
        val hours = binding.etHours.text.toString().trim().ifEmpty { null }
        val contact = binding.etContact.text.toString().trim().ifEmpty { null }
        val lat = binding.etLat.text.toString().toDoubleOrNull()
        val lng = binding.etLng.text.toString().toDoubleOrNull()

        if (name.isEmpty() || desc.isEmpty()) {
            Toast.makeText(context, "Name and description are required", Toast.LENGTH_SHORT).show()
            return
        }

        val ownerId = editingListing?.ownerId ?: UserSession.userId ?: ""
        val insert = ListingInsert(
            ownerId = ownerId,
            name = name,
            description = desc,
            category = cat,
            address = addr,
            hours = hours,
            contact = contact,
            lat = lat,
            lng = lng,
            status = editingListing?.status ?: "pending"
        )

        binding.btnSave.isEnabled = false
        lifecycleScope.launch {
            try {
                val editId = editingListing?.id
                if (editId != null) {
                    ListingRepository.updateListing(editId, insert)
                } else {
                    ListingRepository.createListing(insert)
                }
                Toast.makeText(context, "Saved!", Toast.LENGTH_SHORT).show()
                requireActivity().supportFragmentManager.popBackStack()
            } catch (e: Exception) {
                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                binding.btnSave.isEnabled = true
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
