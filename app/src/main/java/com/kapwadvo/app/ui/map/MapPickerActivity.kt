package com.kapwadvo.app.ui.map

import android.app.Activity
import android.content.Intent
import android.location.Geocoder
import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.kapwadvo.app.databinding.ActivityMapPickerBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import java.util.Locale

/**
 * Full-screen OSMdroid map that lets the user tap to drop a pin.
 * Returns EXTRA_LAT, EXTRA_LNG, and EXTRA_ADDRESS via setResult.
 */
class MapPickerActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_LAT = "lat"
        const val EXTRA_LNG = "lng"
        const val EXTRA_ADDRESS = "address"
    }

    private lateinit var binding: ActivityMapPickerBinding

    private val davaoCenter = GeoPoint(7.1907, 125.4553)
    private var pinMarker: Marker? = null
    private var selectedLat: Double? = null
    private var selectedLng: Double? = null
    private var selectedAddress: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMapPickerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            // Push the back button card below the status bar
            val extraMarginPx = (12 * resources.displayMetrics.density).toInt()
            val params = binding.cardToolbar.layoutParams as androidx.constraintlayout.widget.ConstraintLayout.LayoutParams
            params.topMargin = bars.top + extraMarginPx
            binding.cardToolbar.layoutParams = params
            binding.cardConfirm.setPadding(0, 0, 0, bars.bottom)
            insets
        }

        setupMap()

        binding.btnBackPicker.setOnClickListener { finish() }

        binding.btnConfirmPin.setOnClickListener {
            val lat = selectedLat ?: return@setOnClickListener
            val lng = selectedLng ?: return@setOnClickListener
            val result = Intent().apply {
                putExtra(EXTRA_LAT, lat)
                putExtra(EXTRA_LNG, lng)
                putExtra(EXTRA_ADDRESS, selectedAddress)
            }
            setResult(Activity.RESULT_OK, result)
            finish()
        }
    }

    private fun setupMap() {
        binding.mapView.apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            controller.setZoom(14.0)
            controller.setCenter(davaoCenter)
        }

        val tapReceiver = object : MapEventsReceiver {
            override fun singleTapConfirmedHelper(p: GeoPoint): Boolean {
                placePin(p)
                return true
            }
            override fun longPressHelper(p: GeoPoint) = false
        }
        binding.mapView.overlays.add(0, MapEventsOverlay(tapReceiver))
    }

    private fun placePin(point: GeoPoint) {
        selectedLat = point.latitude
        selectedLng = point.longitude
        selectedAddress = ""

        // Remove old marker
        pinMarker?.let { binding.mapView.overlays.remove(it) }

        // Add new marker
        pinMarker = Marker(binding.mapView).apply {
            position = point
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            title = "Selected Location"
        }
        binding.mapView.overlays.add(pinMarker)
        binding.mapView.invalidate()

        // Show the confirm card and start geocoding
        binding.cardConfirm.visibility = View.VISIBLE
        binding.tvSelectedAddress.text = "Fetching address…"
        binding.btnConfirmPin.isEnabled = false

        reverseGeocode(point.latitude, point.longitude)
    }

    private fun reverseGeocode(lat: Double, lng: Double) {
        CoroutineScope(Dispatchers.IO).launch {
            val address = try {
                if (Geocoder.isPresent()) {
                    val geocoder = Geocoder(this@MapPickerActivity, Locale.getDefault())
                    @Suppress("DEPRECATION")
                    val results = geocoder.getFromLocation(lat, lng, 1)
                    if (!results.isNullOrEmpty()) {
                        val a = results[0]
                        listOfNotNull(
                            a.featureName,
                            a.thoroughfare,
                            a.subLocality,
                            a.locality,
                            a.adminArea
                        ).distinct().joinToString(", ")
                    } else {
                        "Lat: ${"%.5f".format(lat)}, Lng: ${"%.5f".format(lng)}"
                    }
                } else {
                    "Lat: ${"%.5f".format(lat)}, Lng: ${"%.5f".format(lng)}"
                }
            } catch (e: Exception) {
                "Lat: ${"%.5f".format(lat)}, Lng: ${"%.5f".format(lng)}"
            }

            withContext(Dispatchers.Main) {
                selectedAddress = address
                binding.tvSelectedAddress.text = address
                binding.btnConfirmPin.isEnabled = true
            }
        }
    }

    override fun onResume() {
        super.onResume()
        binding.mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        binding.mapView.onPause()
    }
}
