package com.kapwadvo.app.ui.main.profile

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.kapwadvo.app.UserSession
import com.kapwadvo.app.data.repository.AuthRepository
import com.kapwadvo.app.databinding.FragmentEditProfileBinding
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.activity.result.contract.ActivityResultContracts
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.canhub.cropper.CropImageContract
import com.canhub.cropper.CropImageContractOptions
import com.canhub.cropper.CropImageOptions
import java.util.Calendar

class EditProfileFragment : Fragment() {

    private var _binding: FragmentEditProfileBinding? = null
    private val binding get() = _binding!!

    private val cropImage = registerForActivityResult(CropImageContract()) { result ->
        if (result.isSuccessful) {
            val uriContent = result.uriContent
            if (uriContent != null) {
                uploadAvatar(uriContent)
            }
        }
    }

    private fun launchCropper() {
        cropImage.launch(
            CropImageContractOptions(
                uri = null,
                cropImageOptions = CropImageOptions(
                    imageSourceIncludeCamera = true,
                    imageSourceIncludeGallery = true,
                    aspectRatioX = 1,
                    aspectRatioY = 1,
                    fixAspectRatio = true
                )
            )
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEditProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        populateCurrentValues()
        setupDatePicker()
        setupClickListeners()
    }

    /** Pre-fill fields with current session values. */
    private fun populateCurrentValues() {
        binding.etFirstName.setText(UserSession.firstName)
        binding.etLastName.setText(UserSession.lastName)
        binding.etDob.setText(UserSession.dob)
        binding.etPhone.setText(UserSession.phoneNumber)
        binding.etAddress.setText(UserSession.address)
        
        loadAvatar(UserSession.avatarUrl)
    }

    private fun loadAvatar(url: String?) {
        val name = UserSession.fullName.ifEmpty { UserSession.email }
        binding.tvAvatarLetter.text = name.firstOrNull()?.uppercase() ?: "?"

        if (url.isNullOrEmpty()) {
            binding.ivAvatar.visibility = View.GONE
            binding.tvAvatarLetter.visibility = View.VISIBLE
        } else {
            binding.ivAvatar.visibility = View.VISIBLE
            binding.tvAvatarLetter.visibility = View.GONE
            Glide.with(this)
                .load(url)
                .circleCrop()
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .into(binding.ivAvatar)
        }
    }
    
    private fun uploadAvatar(uri: android.net.Uri) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                binding.pbAvatar.visibility = View.VISIBLE
                val bytes = withContext(Dispatchers.IO) {
                    requireContext().contentResolver.openInputStream(uri)?.readBytes()
                } ?: throw Exception("Could not read image file")
                
                val publicUrl = AuthRepository.uploadAvatar(bytes)
                loadAvatar(publicUrl)
                Toast.makeText(requireContext(), "Profile photo updated", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Failed to upload photo", Toast.LENGTH_LONG).show()
            } finally {
                binding.pbAvatar.visibility = View.GONE
            }
        }
    }

    private fun setupDatePicker() {
        val showPicker = View.OnClickListener {
            val cal = Calendar.getInstance()
            DatePickerDialog(
                requireContext(),
                { _, year, month, day ->
                    val formatted = "%04d-%02d-%02d".format(year, month + 1, day)
                    binding.etDob.setText(formatted)
                },
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH)
            ).show()
        }
        binding.etDob.setOnClickListener(showPicker)
        binding.tilDob.setEndIconOnClickListener(showPicker)
    }

    private fun setupClickListeners() {
        binding.btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        binding.flAvatar.setOnClickListener { launchCropper() }
        
        binding.ivEditAvatar.setOnClickListener { launchCropper() }

        binding.btnSave.setOnClickListener {
            saveChanges()
        }
    }

    private fun saveChanges() {
        val firstName = binding.etFirstName.text.toString().trim()
        val lastName = binding.etLastName.text.toString().trim()
        val dob = binding.etDob.text.toString().trim()
        val phone = binding.etPhone.text.toString().trim()
        val address = binding.etAddress.text.toString().trim()

        if (firstName.isBlank() || lastName.isBlank()) {
            Toast.makeText(context, "First name and last name cannot be empty", Toast.LENGTH_SHORT).show()
            return
        }

        setLoading(true)

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                // 1. Update profile table fields
                AuthRepository.updateProfile(
                    firstName = firstName,
                    lastName = lastName,
                    dob = dob,
                    phoneNumber = phone,
                    address = address
                )

                setLoading(false)

                Toast.makeText(context, "Profile updated successfully", Toast.LENGTH_SHORT).show()

                parentFragmentManager.popBackStack()

            } catch (e: Exception) {
                setLoading(false)
                Toast.makeText(context, "Failed to update profile. Please try again", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun setLoading(isLoading: Boolean) {
        binding.btnSave.isEnabled = !isLoading
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
