package com.kapwadvo.app.ui.main.profile

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.kapwadvo.app.R
import com.kapwadvo.app.UserSession
import com.kapwadvo.app.data.repository.ApplicationRepository
import com.kapwadvo.app.data.repository.AuthRepository
import com.kapwadvo.app.databinding.FragmentProfileBinding
import com.kapwadvo.app.ui.auth.AuthActivity
import com.kapwadvo.app.ui.owner.BusinessOwnerActivity
import com.kapwadvo.app.ui.main.explore.MyReservationsFragment
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.activity.result.contract.ActivityResultContracts
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.canhub.cropper.CropImageContract
import com.canhub.cropper.CropImageContractOptions
import com.canhub.cropper.CropImageOptions

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
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
        if (!UserSession.isLoggedIn()) return
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

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupBackButton()
        renderProfile()
    }

    private fun setupBackButton() {
        binding.btnBack.setOnClickListener {
            val activity = requireActivity()
            when (activity) {
                is com.kapwadvo.app.ui.main.MainActivity -> {
                    activity.findViewById<com.google.android.material.bottomnavigation.BottomNavigationView>(com.kapwadvo.app.R.id.bottomNav)?.selectedItemId = com.kapwadvo.app.R.id.nav_explore
                }
                is BusinessOwnerActivity -> {
                    activity.findViewById<com.google.android.material.bottomnavigation.BottomNavigationView>(com.kapwadvo.app.R.id.bottomNav)?.selectedItemId = com.kapwadvo.app.R.id.nav_owner_listings
                }
                is com.kapwadvo.app.ui.admin.AdminActivity -> {
                    activity.findViewById<com.google.android.material.bottomnavigation.BottomNavigationView>(com.kapwadvo.app.R.id.bottomNav)?.selectedItemId = com.kapwadvo.app.R.id.nav_admin_listings
                }
            }
        }
        
        binding.ivEditAvatar.setOnClickListener { launchCropper() }
        binding.flAvatar.setOnClickListener { launchCropper() }
    }

    private fun uploadAvatar(uri: android.net.Uri) {
        if (!com.kapwadvo.app.KapwaDVOApp.networkMonitor.isOnline.value) {
            Toast.makeText(requireContext(), "You must be online to update your profile photo", Toast.LENGTH_SHORT).show()
            return
        }
        
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

    private fun loadAvatar(url: String?) {
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

    private fun renderProfile() {
        if (!UserSession.isLoggedIn()) {
            renderGuestState()
            return
        }
        renderLoggedInState()
    }

    private fun renderGuestState() {
        binding.tvGuestTitle.visibility = View.VISIBLE
        binding.tvGuestDesc.visibility = View.VISIBLE
        binding.btnSignIn.visibility = View.VISIBLE
        binding.tvName.text = "Guest"
        binding.tvEmail.text = ""
        binding.tvRole.text = ""
        binding.tvAvatarLetter.text = "G"
        binding.ivAvatar.visibility = View.GONE
        binding.ivEditAvatar.visibility = View.GONE
        binding.btnLogout.visibility = View.GONE
        binding.btnEditProfile.visibility = View.GONE
        binding.btnMyReservations.visibility = View.GONE
        binding.btnListPublicSpot.visibility = View.GONE
        binding.btnNotificationSettings.visibility = View.GONE

        binding.btnSignIn.setOnClickListener {
            startActivity(Intent(requireContext(), AuthActivity::class.java))
            requireActivity().finish()
        }
    }

    private fun renderLoggedInState() {
        val name = UserSession.fullName.ifEmpty { UserSession.email }
        binding.tvName.text = name
        binding.tvEmail.text = UserSession.email
        binding.tvRole.text = UserSession.role.replaceFirstChar { it.uppercase() }
        binding.tvAvatarLetter.text = name.firstOrNull()?.uppercase() ?: "?"
        binding.ivEditAvatar.visibility = View.VISIBLE
        loadAvatar(UserSession.avatarUrl)

        // Hide guest-only views
        binding.tvGuestTitle.visibility = View.GONE
        binding.tvGuestDesc.visibility = View.GONE
        binding.btnSignIn.visibility = View.GONE
        binding.btnLogout.visibility = View.VISIBLE
        binding.btnEditProfile.visibility = View.VISIBLE
        binding.btnMyReservations.visibility = View.VISIBLE
        binding.btnListPublicSpot.visibility = View.VISIBLE
        binding.btnNotificationSettings.visibility = View.VISIBLE

        val isOwnerActivity = requireActivity() is BusinessOwnerActivity

        if (isOwnerActivity) {
            // We are in Business Owner Mode
            binding.btnBecomeOwner.visibility = View.GONE
            binding.cardApplicationPending.visibility = View.GONE
            binding.btnSwitchMode.visibility = View.VISIBLE
            binding.btnSwitchMode.text = getString(R.string.switch_to_user)
            binding.btnSwitchMode.setOnClickListener { switchModeToUser() }
            binding.btnMyReservations.visibility = View.GONE
            binding.btnListPublicSpot.visibility = View.GONE
        } else if (UserSession.isAdmin()) {
            // Admins: hide become owner entirely
            binding.btnBecomeOwner.visibility = View.GONE
            binding.cardApplicationPending.visibility = View.GONE
            binding.btnSwitchMode.visibility = View.GONE
            binding.btnMyReservations.visibility = View.GONE
            binding.btnListPublicSpot.visibility = View.GONE
        } else {
            // We are in User Mode
            if (UserSession.isOwner()) {
                // DB role is owner, meaning they are approved
                binding.btnBecomeOwner.visibility = View.GONE
                binding.cardApplicationPending.visibility = View.GONE
                binding.btnSwitchMode.visibility = View.VISIBLE
                binding.btnSwitchMode.text = getString(R.string.switch_to_owner)
                binding.btnSwitchMode.setOnClickListener { switchModeToOwner() }
            } else {
                // DB role is user, check application status
                checkApplicationStatus()
            }
        }

        binding.btnBecomeOwner.setOnClickListener {
            navigateToApplicationForm()
        }

        binding.btnEditProfile.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(com.kapwadvo.app.R.id.fragmentContainer, EditProfileFragment())
                .addToBackStack(null)
                .commit()
        }
        
        binding.btnMyReservations.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(com.kapwadvo.app.R.id.fragmentContainer, MyReservationsFragment())
                .addToBackStack(null)
                .commit()
        }
        
        binding.btnListPublicSpot.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(com.kapwadvo.app.R.id.fragmentContainer, com.kapwadvo.app.ui.main.explore.RequestPublicSpotFragment())
                .addToBackStack(null)
                .commit()
        }
        
        binding.btnNotificationSettings.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(com.kapwadvo.app.R.id.fragmentContainer, AccountSettingsFragment())
                .addToBackStack(null)
                .commit()
        }
        
        viewLifecycleOwner.lifecycleScope.launch {
            com.kapwadvo.app.KapwaDVOApp.networkMonitor.isOnline.collect { isOnline ->
                binding.btnBecomeOwner.isEnabled = isOnline
                binding.btnEditProfile.isEnabled = isOnline
                binding.btnListPublicSpot.isEnabled = isOnline
                binding.ivEditAvatar.isEnabled = isOnline
                binding.flAvatar.isEnabled = isOnline
                
                val offlineMsg = "Requires internet connection"
                if (!isOnline) {
                    binding.btnBecomeOwner.contentDescription = offlineMsg
                    binding.btnEditProfile.contentDescription = offlineMsg
                    binding.btnListPublicSpot.contentDescription = offlineMsg
                }
            }
        }

        binding.btnLogout.setOnClickListener {
            viewLifecycleOwner.lifecycleScope.launch {
                try {
                    AuthRepository.logout()
                    startActivity(Intent(requireContext(), AuthActivity::class.java))
                    requireActivity().finish()
                } catch (e: Exception) {
                    Toast.makeText(context, "Failed to log out. Please try again", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun switchModeToUser() {
        binding.btnSwitchMode.isEnabled = false
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                if (UserSession.role != "user") {
                    AuthRepository.switchRole("user")
                }
                Toast.makeText(context, "Switched to User mode", Toast.LENGTH_SHORT).show()
                startActivity(Intent(requireContext(), com.kapwadvo.app.ui.main.MainActivity::class.java))
                requireActivity().finish()
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to switch mode. Please try again", Toast.LENGTH_LONG).show()
                binding.btnSwitchMode.isEnabled = true
            }
        }
    }

    private fun switchModeToOwner() {
        binding.btnSwitchMode.isEnabled = false
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                if (UserSession.role != "owner") {
                    AuthRepository.switchRole("owner")
                }
                Toast.makeText(context, "Switched to Business Owner mode", Toast.LENGTH_SHORT).show()
                startActivity(Intent(requireContext(), BusinessOwnerActivity::class.java))
                requireActivity().finish()
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to switch mode. Please try again", Toast.LENGTH_LONG).show()
                binding.btnSwitchMode.isEnabled = true
            }
        }
    }

    private fun checkApplicationStatus() {
        viewLifecycleOwner.lifecycleScope.launch {
            val uid = UserSession.userId ?: return@launch
            binding.btnBecomeOwner.isEnabled = false
            val originalText = binding.btnBecomeOwner.text
            binding.btnBecomeOwner.text = "Checking..."
            binding.btnBecomeOwner.visibility = View.VISIBLE
            
            val app = ApplicationRepository.getUserApplication(uid)
            
            binding.btnBecomeOwner.text = originalText
            binding.btnBecomeOwner.isEnabled = true
            
            when (app?.status) {
                "pending" -> {
                    binding.cardApplicationPending.visibility = View.VISIBLE
                    binding.btnBecomeOwner.visibility = View.GONE
                    binding.btnSwitchMode.visibility = View.GONE
                }
                "approved" -> {
                    // Approved but currently in user mode — show switch button
                    binding.cardApplicationPending.visibility = View.GONE
                    binding.btnBecomeOwner.visibility = View.GONE
                    binding.btnSwitchMode.visibility = View.VISIBLE
                    binding.btnSwitchMode.text = getString(R.string.switch_to_owner)
                    binding.btnSwitchMode.setOnClickListener { switchModeToOwner() }
                }
                null -> {
                    binding.btnBecomeOwner.visibility = View.VISIBLE
                    binding.cardApplicationPending.visibility = View.GONE
                    binding.btnSwitchMode.visibility = View.GONE
                }
                else -> {
                    binding.btnBecomeOwner.visibility = View.GONE
                    binding.btnSwitchMode.visibility = View.GONE
                }
            }
        }
    }

    private fun navigateToApplicationForm() {
        parentFragmentManager.beginTransaction()
            .replace(com.kapwadvo.app.R.id.fragmentContainer, OwnerApplicationFragment())
            .addToBackStack(null)
            .commit()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
