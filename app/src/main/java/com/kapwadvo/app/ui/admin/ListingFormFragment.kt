package com.kapwadvo.app.ui.admin

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.kapwadvo.app.UserSession
import com.kapwadvo.app.data.models.Listing
import com.kapwadvo.app.data.models.ListingInsert
import com.kapwadvo.app.data.repository.ListingRepository
import com.kapwadvo.app.databinding.FragmentListingFormBinding
import com.kapwadvo.app.ui.map.MapPickerActivity
import kotlinx.coroutines.launch
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.util.UUID

class ListingFormFragment : Fragment() {

    private var _binding: FragmentListingFormBinding? = null
    private val binding get() = _binding!!

    private var editingListing: Listing? = null

    private val selectedPhotoUris = mutableListOf<Uri>()

    // ActivityResultLauncher for the map pin picker
    private val mapPickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data = result.data ?: return@registerForActivityResult
            val lat = data.getDoubleExtra(MapPickerActivity.EXTRA_LAT, Double.NaN)
            val lng = data.getDoubleExtra(MapPickerActivity.EXTRA_LNG, Double.NaN)
            val address = data.getStringExtra(MapPickerActivity.EXTRA_ADDRESS) ?: ""
            if (!lat.isNaN() && !lng.isNaN()) {
                binding.etLat.setText("%.6f".format(lat))
                binding.etLng.setText("%.6f".format(lng))
                if (address.isNotEmpty()) {
                    binding.etAddress.setText(address)
                }
            }
        }
    }

    // Photo picker launcher (multiple photos)
    private val photoPickerLauncher = registerForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia()
    ) { uris ->
        if (uris.isNotEmpty()) {
            selectedPhotoUris.addAll(uris)
            renderPhotoPreviews()
        }
    }

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

        // This fragment is added to android.R.id.content (full screen), so we must
        // manually apply the status bar inset as top padding on the root view.
        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(view) { v, insets ->
            val systemBars = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars())
            v.setPadding(0, systemBars.top, 0, systemBars.bottom)
            insets
        }
        view.requestApplyInsets()

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

            if (editingListing?.status == "approved" && UserSession.isOwner()) {
                binding.btnSave.text = "Request Update"
                binding.tvSecurityNotice.visibility = View.VISIBLE
            }
        } ?: run {
            binding.tvFormTitle.text = "Add Listing"
            binding.btnSave.text = "Create listing"
        }

        binding.btnPickPin.setOnClickListener {
            val intent = Intent(requireContext(), MapPickerActivity::class.java)
            mapPickerLauncher.launch(intent)
        }

        binding.btnBack.setOnClickListener {
            requireActivity().supportFragmentManager.popBackStack()
        }

        binding.btnUploadPhotos.setOnClickListener {
            photoPickerLauncher.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
        }

        // Pre-load existing photos if editing
        editingListing?.photoUrls?.forEach { url ->
            if (url.isNotEmpty()) addPhotoPreview(url)
        }

        binding.btnSave.setOnClickListener { saveListing() }
    }

    private fun renderPhotoPreviews() {
        binding.layoutPhotoPreviews.removeAllViews()
        selectedPhotoUris.forEach { uri -> addPhotoPreview(uri.toString()) }
    }

    private fun addPhotoPreview(uriOrUrl: String) {
        val iv = ImageView(requireContext()).apply {
            layoutParams = android.widget.LinearLayout.LayoutParams(80.dpToPx(), 80.dpToPx()).also {
                it.marginEnd = 8.dpToPx()
            }
            scaleType = ImageView.ScaleType.CENTER_CROP
            setBackgroundColor(android.graphics.Color.parseColor("#E0E0E0"))
        }
        try {
            if (uriOrUrl.startsWith("http")) {
                // Existing remote URL — load with Glide if available, else tint placeholder
                iv.setImageResource(android.R.drawable.ic_menu_gallery)
            } else {
                iv.setImageURI(android.net.Uri.parse(uriOrUrl))
            }
        } catch (_: Exception) {}
        binding.layoutPhotoPreviews.addView(iv)
    }

    private fun Int.dpToPx(): Int = (this * resources.displayMetrics.density).toInt()

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
        
        var finalStatus = editingListing?.status ?: "pending"
        var pendingUpdatesJson: kotlinx.serialization.json.JsonObject? = null

        if (editingListing?.status == "approved" && UserSession.isOwner()) {
            pendingUpdatesJson = buildJsonObject {
                if (name != editingListing?.name) put("name", name)
                if (desc != editingListing?.description) put("description", desc)
                if (cat != editingListing?.category) put("category", cat)
                if (addr != editingListing?.address) put("address", addr ?: "")
                if (hours != editingListing?.hours) put("hours", hours ?: "")
                if (contact != editingListing?.contact) put("contact", contact ?: "")
                if (lat != editingListing?.lat) put("lat", lat ?: 0.0)
                if (lng != editingListing?.lng) put("lng", lng ?: 0.0)
            }
        }

        binding.btnSave.isEnabled = false
        lifecycleScope.launch {
            try {
                // Upload new photos and merge with existing URLs
                val existingUrls = editingListing?.photoUrls ?: emptyList()
                val newUrls = selectedPhotoUris.map { uri ->
                    val bytes = requireContext().contentResolver.openInputStream(uri)?.readBytes() ?: return@map null
                    val fileName = "${UUID.randomUUID()}.jpg"
                    ListingRepository.uploadPhoto(bytes, fileName)
                }.filterNotNull()
                val allPhotoUrls = existingUrls + newUrls

                val insert = ListingInsert(
                    ownerId = ownerId,
                    name = if (pendingUpdatesJson != null) editingListing!!.name else name,
                    description = if (pendingUpdatesJson != null) editingListing!!.description else desc,
                    category = if (pendingUpdatesJson != null) editingListing!!.category else cat,
                    address = if (pendingUpdatesJson != null) editingListing!!.address else addr,
                    hours = if (pendingUpdatesJson != null) editingListing!!.hours else hours,
                    contact = if (pendingUpdatesJson != null) editingListing!!.contact else contact,
                    lat = if (pendingUpdatesJson != null) editingListing!!.lat else lat,
                    lng = if (pendingUpdatesJson != null) editingListing!!.lng else lng,
                    status = finalStatus,
                    pendingUpdates = pendingUpdatesJson,
                    photoUrls = allPhotoUrls
                )

                val editId = editingListing?.id
                if (editId != null) {
                    ListingRepository.updateListing(editId, insert)
                } else {
                    ListingRepository.createListing(insert)
                }
                Toast.makeText(context, "Saved!", Toast.LENGTH_SHORT).show()
                (requireActivity().supportFragmentManager.findFragmentByTag("listings") as? com.kapwadvo.app.ui.owner.OwnerListingsFragment)?.reload()
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
