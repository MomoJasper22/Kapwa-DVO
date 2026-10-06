package com.kapwadvo.app.ui.main.explore

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.kapwadvo.app.UserSession
import com.kapwadvo.app.data.models.PublicSpotRequestInsert
import com.kapwadvo.app.data.repository.ListingRepository
import com.kapwadvo.app.data.repository.PublicSpotRepository
import com.kapwadvo.app.databinding.FragmentRequestPublicSpotBinding
import com.kapwadvo.app.ui.map.MapPickerActivity
import kotlinx.coroutines.launch
import java.util.UUID

class RequestPublicSpotFragment : Fragment() {

    private var _binding: FragmentRequestPublicSpotBinding? = null
    private val binding get() = _binding!!

    private var selectedLat: Double? = null
    private var selectedLng: Double? = null
    private var selectedPhotoUri: Uri? = null

    private val mapPickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data = result.data ?: return@registerForActivityResult
            val lat = data.getDoubleExtra(MapPickerActivity.EXTRA_LAT, Double.NaN)
            val lng = data.getDoubleExtra(MapPickerActivity.EXTRA_LNG, Double.NaN)
            val address = data.getStringExtra(MapPickerActivity.EXTRA_ADDRESS) ?: ""
            if (!lat.isNaN() && !lng.isNaN()) {
                selectedLat = lat
                selectedLng = lng
                binding.tvCoordinates.text = "Coordinates: ${"%.6f".format(lat)}, ${"%.6f".format(lng)}"
                if (address.isNotEmpty() && binding.etAddress.text.isNullOrEmpty()) {
                    binding.etAddress.setText(address)
                }
            }
        }
    }

    private val photoPickerLauncher = registerForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            selectedPhotoUri = uri
            binding.ivPreview.visibility = View.VISIBLE
            Glide.with(this).load(uri).centerCrop().into(binding.ivPreview)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRequestPublicSpotBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        binding.btnPinLocation.setOnClickListener {
            val intent = Intent(requireContext(), MapPickerActivity::class.java)
            mapPickerLauncher.launch(intent)
        }

        binding.flPhotoUpload.setOnClickListener {
            photoPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }

        binding.btnSubmit.setOnClickListener {
            submitRequest()
        }
    }

    private fun submitRequest() {
        val uid = UserSession.userId
        if (uid == null) {
            Toast.makeText(requireContext(), "You must be logged in to request a spot.", Toast.LENGTH_SHORT).show()
            return
        }

        val name = binding.etName.text.toString().trim()
        val address = binding.etAddress.text.toString().trim()
        val comments = binding.etComments.text.toString().trim()

        if (name.isEmpty() || address.isEmpty()) {
            Toast.makeText(requireContext(), "Name and address are required.", Toast.LENGTH_SHORT).show()
            return
        }
        if (selectedLat == null || selectedLng == null) {
            Toast.makeText(requireContext(), "Please pin the location on the map.", Toast.LENGTH_SHORT).show()
            return
        }

        binding.btnSubmit.isEnabled = false
        binding.pbUpload.visibility = View.VISIBLE

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                var photoUrl: String? = null
                val uri = selectedPhotoUri
                val isOnline = com.kapwadvo.app.KapwaDVOApp.networkMonitor.isOnline.value
                if (uri != null && isOnline) {
                    val bytes = requireContext().contentResolver.openInputStream(uri)?.readBytes()
                    if (bytes != null) {
                        val fileName = "${UUID.randomUUID()}.jpg"
                        photoUrl = ListingRepository.uploadPhoto(bytes, fileName)
                    }
                }

                val insert = PublicSpotRequestInsert(
                    userId = uid,
                    name = name,
                    address = address,
                    latitude = selectedLat,
                    longitude = selectedLng,
                    comments = comments.ifEmpty { null },
                    photoUrl = photoUrl,
                    status = "pending"
                )

                if (!isOnline) {
                    val payload = kotlinx.serialization.json.Json.encodeToString(insert)
                    com.kapwadvo.app.data.repository.SyncQueueRepository.queueAction(uid, "SUBMIT_PUBLIC_SPOT", payload)
                    Toast.makeText(requireContext(), "Changes Made Will Apply Once Online", Toast.LENGTH_LONG).show()
                } else {
                    PublicSpotRepository.createRequest(insert)
                    Toast.makeText(requireContext(), "Public Spot requested! Waiting for admin approval.", Toast.LENGTH_LONG).show()
                }
                parentFragmentManager.popBackStack()

            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Failed to submit request.", Toast.LENGTH_SHORT).show()
                binding.btnSubmit.isEnabled = true
                binding.pbUpload.visibility = View.GONE
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
