package com.kapwadvo.app.ui.main.map

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.material.chip.Chip
import com.kapwadvo.app.UserSession
import com.kapwadvo.app.data.models.Listing
import com.kapwadvo.app.data.repository.ListingRepository
import com.kapwadvo.app.data.repository.SavedRepository
import com.kapwadvo.app.ui.main.explore.ListingDetailSheet
import com.kapwadvo.app.databinding.FragmentMapBinding
import com.kapwadvo.app.R
import kotlinx.coroutines.launch
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay

class MapFragment : Fragment() {

    private var _binding: FragmentMapBinding? = null
    private val binding get() = _binding!!

    private val davaoCenter = GeoPoint(7.1907, 125.4553)
    private var savedIds = mutableSetOf<String>()

    private val categories = listOf("All") + com.kapwadvo.app.data.CategoryManager.getCategories()
    private var selectedCategory = "All"
    private var allListings = emptyList<Listing>()

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var myLocationOverlay: MyLocationNewOverlay? = null

    // Runtime permission launcher
    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            enableMyLocationOverlay()
            zoomToCurrentLocation()
        } else {
            Toast.makeText(requireContext(), "Location permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMapBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())

        setupMap()
        setupChips()
        setupSearch()
        setupGpsFab()
        setupZoomControls()
        loadPins()

        parentFragmentManager.setFragmentResultListener("detail_dismissed", viewLifecycleOwner) { _, _ ->
            if (UserSession.isLoggedIn()) {
                viewLifecycleOwner.lifecycleScope.launch {
                    val b = _binding ?: return@launch
                    val uid = UserSession.userId ?: return@launch
                    val saved = SavedRepository.getSavedForUser(uid)
                    savedIds = saved.map { it.listingId }.toMutableSet()
                }
            }
        }
    }

    private fun setupMap() {
        binding.mapView.apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            setBuiltInZoomControls(false)
            controller.setZoom(13.5)
            controller.setCenter(davaoCenter)
        }
    }

    private fun setupZoomControls() {
        binding.fabZoomIn.setOnClickListener { binding.mapView.controller.zoomIn() }
        binding.fabZoomOut.setOnClickListener { binding.mapView.controller.zoomOut() }
    }

    private fun setupChips() {
        categories.forEach { cat ->
            val chip = Chip(requireContext()).apply {
                text = cat
                isCheckable = true
                isChecked = cat == selectedCategory
            }
            chip.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    selectedCategory = cat
                    refreshPins(zoomToFit = true)
                }
            }
            binding.chipGroupCategories.addView(chip)
        }
        (binding.chipGroupCategories.getChildAt(0) as? Chip)?.isChecked = true
    }

    private fun updateChipCounts() {
        for (i in 0 until binding.chipGroupCategories.childCount) {
            val chip = binding.chipGroupCategories.getChildAt(i) as? Chip ?: continue
            val cat = categories.getOrNull(i) ?: continue
            val count = if (cat == "All") allListings.size else allListings.count {
                com.kapwadvo.app.data.CategoryManager.decode(it.category).first.contains(cat)
            }
            chip.text = "$cat ($count)"
        }
    }

    private var searchJob: kotlinx.coroutines.Job? = null

    private fun setupSearch() {
        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                searchJob?.cancel()
                searchJob = viewLifecycleOwner.lifecycleScope.launch {
                    kotlinx.coroutines.delay(300)
                    val b = _binding ?: return@launch
                    refreshPins()
                }
            }
        })

        binding.etSearch.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus || !binding.etSearch.text.isNullOrEmpty()) {
                binding.tilSearch.hint = ""
            } else {
                binding.tilSearch.hint = getString(R.string.map_search_hint)
            }
        }

        binding.etSearch.setOnItemClickListener { _, _, position, _ ->
            val selectedName = binding.etSearch.adapter.getItem(position) as? String ?: return@setOnItemClickListener
            val listing = allListings.find { it.name == selectedName }
            if (listing != null && listing.lat != null && listing.lng != null) {
                binding.mapView.controller.apply {
                    setZoom(17.0)
                    animateTo(GeoPoint(listing.lat, listing.lng))
                }
                showDetail(listing)
                val imm = requireContext().getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
                imm.hideSoftInputFromWindow(binding.etSearch.windowToken, 0)
                binding.etSearch.clearFocus()
            }
        }

        binding.etSearch.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH) {
                val imm = requireContext().getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
                imm.hideSoftInputFromWindow(binding.etSearch.windowToken, 0)
                true
            } else {
                false
            }
        }
    }

    // performGeocodeSearch removed as search is now restricted to local business names

    private fun setupGpsFab() {
        binding.fabMyLocation.setOnClickListener {
            if (hasLocationPermission()) {
                enableMyLocationOverlay()
                zoomToCurrentLocation()
            } else {
                locationPermissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                )
            }
        }
    }

    private fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            requireContext(), Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(
            requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun enableMyLocationOverlay() {
        if (myLocationOverlay != null) return // already added

        myLocationOverlay = MyLocationNewOverlay(
            GpsMyLocationProvider(requireContext()), binding.mapView
        ).also { overlay ->
            overlay.enableMyLocation()
            overlay.enableFollowLocation()
            binding.mapView.overlays.add(overlay)
            binding.mapView.invalidate()
        }
    }

    private fun zoomToCurrentLocation() {
        if (!hasLocationPermission()) return

        try {
            fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                .addOnSuccessListener { location: Location? ->
                    location?.let {
                        binding.mapView.controller.apply {
                            setZoom(16.0)
                            animateTo(GeoPoint(it.latitude, it.longitude))
                        }
                    } ?: Toast.makeText(
                        requireContext(),
                        "Unable to get current location. Make sure GPS is enabled.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                .addOnFailureListener {
                    Toast.makeText(
                        requireContext(),
                        "Failed to get location: ${it.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
        } catch (e: SecurityException) {
            Toast.makeText(requireContext(), "Location permission required", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadPins() {
        binding.progressBar.visibility = View.VISIBLE
        viewLifecycleOwner.lifecycleScope.launch {
            allListings = ListingRepository.getApprovedListings()
            
            val b = _binding ?: return@launch
            val ctx = context ?: return@launch

            val names = allListings.map { it.name }
            val adapter = android.widget.ArrayAdapter(ctx, android.R.layout.simple_dropdown_item_1line, names)
            b.etSearch.setAdapter(adapter)

            if (UserSession.isLoggedIn()) {
                val uid = UserSession.userId
                if (uid != null) {
                    val saved = SavedRepository.getSavedForUser(uid)
                    savedIds = saved.map { it.listingId }.toMutableSet()
                }
            }

            b.progressBar.visibility = View.GONE
            updateChipCounts()
            refreshPins()
        }
    }

    private fun refreshPins(zoomToFit: Boolean = false) {
        val query = binding.etSearch.text?.toString()?.trim()?.lowercase() ?: ""

        val filtered = allListings.filter { listing ->
            val decodedCategories = com.kapwadvo.app.data.CategoryManager.decode(listing.category).first
            val matchCat = selectedCategory == "All" || decodedCategories.contains(selectedCategory)
            val matchSearch = query.isEmpty() ||
                listing.name.lowercase().contains(query) ||
                listing.description.lowercase().contains(query) ||
                listing.address?.lowercase()?.contains(query) == true
            matchCat && matchSearch
        }

        // Preserve the MyLocationOverlay when clearing
        val locationOverlay = myLocationOverlay
        binding.mapView.overlays.clear()
        if (locationOverlay != null) {
            binding.mapView.overlays.add(locationOverlay)
        }

        var minLat = Double.MAX_VALUE
        var maxLat = -Double.MAX_VALUE
        var minLng = Double.MAX_VALUE
        var maxLng = -Double.MAX_VALUE
        var hasValidPins = false

        filtered.forEach { listing -> 
            addPin(listing)
            if (listing.lat != null && listing.lng != null) {
                hasValidPins = true
                if (listing.lat < minLat) minLat = listing.lat
                if (listing.lat > maxLat) maxLat = listing.lat
                if (listing.lng < minLng) minLng = listing.lng
                if (listing.lng > maxLng) maxLng = listing.lng
            }
        }
        binding.mapView.invalidate()

        if (zoomToFit && hasValidPins) {
            if (minLat == maxLat && minLng == maxLng) {
                 binding.mapView.controller.animateTo(GeoPoint(minLat, minLng), 15.0, 1000L)
            } else {
                 val box = org.osmdroid.util.BoundingBox(maxLat, maxLng, minLat, minLng)
                 binding.mapView.zoomToBoundingBox(box, true, 100)
            }
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
        myLocationOverlay?.enableMyLocation()
    }

    override fun onPause() {
        super.onPause()
        binding.mapView.onPause()
        myLocationOverlay?.disableMyLocation()
    }

    private fun showDetail(listing: Listing) {
        val sheet = ListingDetailSheet.newInstance(listing)
        sheet.show(parentFragmentManager, "ListingDetailSheet")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        myLocationOverlay?.disableMyLocation()
        binding.mapView.onDetach()
        _binding = null
    }
}
