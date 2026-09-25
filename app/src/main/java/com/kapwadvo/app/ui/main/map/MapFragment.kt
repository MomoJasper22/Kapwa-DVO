package com.kapwadvo.app.ui.main.map

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.widget.Toast
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.kapwadvo.app.R
import com.kapwadvo.app.UserSession
import com.kapwadvo.app.data.models.Listing
import com.kapwadvo.app.data.repository.ListingRepository
import com.kapwadvo.app.data.repository.SavedRepository
import com.kapwadvo.app.ui.main.explore.ListingDetailSheet
import com.kapwadvo.app.databinding.BottomSheetListingDetailBinding
import com.kapwadvo.app.databinding.DialogBookingBinding
import com.kapwadvo.app.databinding.FragmentMapBinding
import kotlinx.coroutines.launch
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.overlay.Marker
import java.util.Calendar

class MapFragment : Fragment() {

    private var _binding: FragmentMapBinding? = null
    private val binding get() = _binding!!

    // Davao City center
    private val davaoCenter = GeoPoint(7.1907, 125.4553)
    private var savedIds = mutableSetOf<String>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentMapBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupMap()
        loadPins()

        parentFragmentManager.setFragmentResultListener("detail_dismissed", viewLifecycleOwner) { _, _ ->
            if (UserSession.isLoggedIn()) {
                lifecycleScope.launch {
                    val saved = SavedRepository.getSavedForUser(UserSession.userId!!)
                    savedIds = saved.map { it.listingId }.toMutableSet()
                }
            }
        }
    }

    private fun setupMap() {
        binding.mapView.apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            controller.setZoom(13.5)
            controller.setCenter(davaoCenter)
        }
    }

    private fun loadPins() {
        binding.progressBar.visibility = View.VISIBLE
        lifecycleScope.launch {
            val listings = ListingRepository.getApprovedListings()
            
            if (UserSession.isLoggedIn()) {
                val saved = SavedRepository.getSavedForUser(UserSession.userId!!)
                savedIds = saved.map { it.listingId }.toMutableSet()
            }
            
            binding.progressBar.visibility = View.GONE
            listings.forEach { addPin(it) }
            binding.mapView.invalidate()
        }
    }

    private fun addPin(listing: Listing) {
        val lat = listing.lat ?: return
        val lng = listing.lng ?: return

        val marker = Marker(binding.mapView).apply {
            position = GeoPoint(lat, lng)
            title = listing.name
            snippet = listing.category
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            setOnMarkerClickListener { _, _ ->
                showDetail(listing)
                true
            }
        }
        binding.mapView.overlays.add(marker)
    }

    override fun onResume() {
        super.onResume()
        binding.mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        binding.mapView.onPause()
    }

    private fun showDetail(listing: Listing) {
        val sheet = ListingDetailSheet.newInstance(listing)
        sheet.show(parentFragmentManager, "ListingDetailSheet")
    }



    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
