package com.kapwadvo.app.ui.admin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.bumptech.glide.Glide
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.kapwadvo.app.data.CategoryManager
import com.kapwadvo.app.data.models.Listing
import com.kapwadvo.app.databinding.BottomSheetAdminListingDetailBinding
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.overlay.Marker

class AdminListingDetailSheet : BottomSheetDialogFragment() {

    companion object {
        private const val ARG_JSON = "listing_json"
        private val json = Json { ignoreUnknownKeys = true }

        fun newInstance(listing: Listing): AdminListingDetailSheet {
            return AdminListingDetailSheet().apply {
                arguments = Bundle().apply {
                    putString(ARG_JSON, json.encodeToString(listing))
                }
            }
        }
    }

    private var _binding: BottomSheetAdminListingDetailBinding? = null
    private val binding get() = _binding!!

    private val listing: Listing by lazy {
        json.decodeFromString<Listing>(requireArguments().getString(ARG_JSON)!!)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetAdminListingDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onStart() {
        super.onStart()
        val d = dialog as? BottomSheetDialog ?: return
        val sheet = d.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet) ?: return
        sheet.layoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT

        val behavior = BottomSheetBehavior.from(sheet)
        behavior.peekHeight = (500 * resources.displayMetrics.density).toInt()
        behavior.isFitToContents = false
        behavior.isHideable = true
        behavior.skipCollapsed = false
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupBasicInfo()
        setupPendingUpdates()
        setupMap()
    }

    private fun setupBasicInfo() {
        binding.tvName.text = listing.name
        binding.tvCategory.text = CategoryManager.toDisplayString(listing.category)
        binding.tvDescription.text = listing.description
        
        if (listing.photoUrls.isNotEmpty()) {
            binding.ivPhoto.visibility = View.VISIBLE
            binding.tvPhotoLabel.visibility = View.GONE
            Glide.with(this)
                .load(listing.photoUrls.first())
                .diskCacheStrategy(com.bumptech.glide.load.engine.DiskCacheStrategy.ALL)
                .placeholder(android.R.color.darker_gray)
                .error(android.R.color.darker_gray)
                .into(binding.ivPhoto)
        } else {
            binding.ivPhoto.visibility = View.GONE
            binding.tvPhotoLabel.visibility = View.VISIBLE
            binding.tvPhotoLabel.text = "[ Photo: ${listing.name} ]"
        }

        if (!listing.address.isNullOrEmpty()) {
            binding.tvAddress.text = listing.address
            binding.layoutAddress.visibility = View.VISIBLE
        }
        if (!listing.hours.isNullOrEmpty()) {
            binding.tvHours.text = listing.hours
            binding.layoutHours.visibility = View.VISIBLE
        }
        if (!listing.contact.isNullOrEmpty()) {
            binding.tvContact.text = listing.contact
            binding.layoutContact.visibility = View.VISIBLE
        }
    }

    private fun setupPendingUpdates() {
        val updates = listing.pendingUpdates
        if (updates != null) {
            binding.layoutUpdates.visibility = View.VISIBLE
            val updatesText = StringBuilder()
            updates.forEach { (key, value) ->
                val cleanVal = value.toString().removeSurrounding("\"")
                updatesText.append("• ${key.replaceFirstChar { it.uppercase() }}: $cleanVal\n")
            }
            binding.tvUpdatesText.text = updatesText.toString().trim()
        } else {
            binding.layoutUpdates.visibility = View.GONE
        }
    }

    private fun setupMap() {
        binding.mapView.setTileSource(TileSourceFactory.MAPNIK)
        binding.mapView.setMultiTouchControls(true)
        
        val lat = listing.lat ?: 7.1907
        val lng = listing.lng ?: 125.4553
        val point = GeoPoint(lat, lng)
        binding.mapView.controller.setZoom(18.0)
        binding.mapView.controller.setCenter(point)
        
        val marker = Marker(binding.mapView)
        marker.position = point
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        marker.title = listing.name
        binding.mapView.overlays.add(marker)
        binding.mapView.invalidate()
    }

    override fun onResume() {
        super.onResume()
        binding.mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        binding.mapView.onPause()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.mapView.onDetach()
        _binding = null
    }
}
