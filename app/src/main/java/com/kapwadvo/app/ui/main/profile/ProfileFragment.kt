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
    private var isSwitchingMode = false

    @Suppress("DEPRECATION")
    private val cropImage = registerForActivityResult(CropImageContract()) { result ->
        if (result.isSuccessful) {
            val uriContent = result.uriContent
            if (uriContent != null) {
                uploadAvatar(uriContent)
            }
        }
    }

    @Suppress("DEPRECATION")
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
            val b = _binding ?: return@launch
            try {
                b.pbAvatar.visibility = View.VISIBLE
                val bytes = withContext(Dispatchers.IO) {
                    requireContext().contentResolver.openInputStream(uri)?.readBytes()
                } ?: throw Exception("Could not read image file")
                
                val publicUrl = AuthRepository.uploadAvatar(bytes)
                loadAvatar(publicUrl)
                if (isAdded) {
                    Toast.makeText(requireContext(), "Profile photo updated", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) { if (e is kotlinx.coroutines.CancellationException) throw e;
                if (isAdded) {
                    Toast.makeText(requireContext(), "Failed to upload photo", Toast.LENGTH_LONG).show()
                }
            } finally {
                _binding?.pbAvatar?.visibility = View.GONE
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
                val activeMode = requireContext().getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE)
                    .getString("active_mode", "user")
                    
                if (activeMode == "user") {
                    binding.btnSwitchMode.text = "Switch to Business Mode"
                    binding.btnSwitchMode.setOnClickListener { switchModeToOwner() }
                } else {
                    binding.btnSwitchMode.text = "Switch to User Mode"
                    binding.btnSwitchMode.setOnClickListener { switchModeToUser() }
                }
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
                val b = _binding ?: return@collect
                b.btnBecomeOwner.isEnabled = true
                b.btnEditProfile.isEnabled = true
                b.btnListPublicSpot.isEnabled = true
                b.ivEditAvatar.isEnabled = true
                b.flAvatar.isEnabled = true
            }
        }

        binding.btnLogout.setOnClickListener {
            viewLifecycleOwner.lifecycleScope.launch {
                val b = _binding ?: return@launch
                try {
                    AuthRepository.logout()
                    if (isAdded) {
                        val intent = Intent(requireContext(), AuthActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        startActivity(intent)
                        requireActivity().finish()
                    }
                } catch (e: Exception) { if (e is kotlinx.coroutines.CancellationException) throw e;
                    if (isAdded) {
                        Toast.makeText(context, "Failed to log out. Please try again", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    private fun switchModeToUser() {
        if (isSwitchingMode) return
        isSwitchingMode = true
        binding.btnSwitchMode.isEnabled = false
        requireContext().getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE)
            .edit().putString("active_mode", "user").apply()
        Toast.makeText(context, "Switched to User mode", Toast.LENGTH_SHORT).show()
        val intent = Intent(requireContext(), com.kapwadvo.app.ui.main.MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        requireActivity().finish()
    }

    private fun switchModeToOwner() {
        if (isSwitchingMode) return
        isSwitchingMode = true
        binding.btnSwitchMode.isEnabled = false
        requireContext().getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE)
            .edit().putString("active_mode", "owner").apply()
        Toast.makeText(context, "Switched to Business Owner mode", Toast.LENGTH_SHORT).show()
        val intent = Intent(requireContext(), BusinessOwnerActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        requireActivity().finish()
    }

    private fun checkApplicationStatus() {
        viewLifecycleOwner.lifecycleScope.launch {
            val uid = UserSession.userId ?: return@launch
            val prefs = requireContext().getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE)
            val cacheKey = "app_status_$uid"
            val cachedStatus = prefs.getString(cacheKey, null)
            val isOnline = com.kapwadvo.app.KapwaDVOApp.networkMonitor.isOnline.value

            val b = _binding ?: return@launch

            // Apply cache first for instant UI without flickering
            if (cachedStatus != null) {
                applyApplicationStatus(cachedStatus)
                // We are done! Only check network if we have NO cache.
                return@launch
            }
            
            val originalText = b.btnBecomeOwner.text
            
            // Otherwise, we check the network since there's no cache
            b.btnBecomeOwner.isEnabled = false
            b.btnBecomeOwner.text = "Checking..."
            b.btnBecomeOwner.visibility = View.VISIBLE
            
            try {
                val app = ApplicationRepository.getUserApplication(uid)
                val status = app?.status ?: "none"
                prefs.edit().putString(cacheKey, status).apply()
                
                val finalBinding = _binding ?: return@launch
                if (cachedStatus == null) {
                    finalBinding.btnBecomeOwner.text = originalText
                    finalBinding.btnBecomeOwner.isEnabled = true
                }
                applyApplicationStatus(status)
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                val finalBinding = _binding ?: return@launch
                finalBinding.btnBecomeOwner.text = originalText
                finalBinding.btnBecomeOwner.isEnabled = true
                applyApplicationStatus("error")
            }
        }
    }

    private fun applyApplicationStatus(status: String) {
        val finalBinding = _binding ?: return
        when (status) {
            "pending" -> {
                finalBinding.cardApplicationPending.visibility = View.VISIBLE
                finalBinding.btnBecomeOwner.visibility = View.GONE
                finalBinding.btnSwitchMode.visibility = View.GONE
            }
            "approved" -> {
                finalBinding.cardApplicationPending.visibility = View.GONE
                finalBinding.btnBecomeOwner.visibility = View.GONE
                finalBinding.btnSwitchMode.visibility = View.VISIBLE
                finalBinding.btnSwitchMode.text = getString(R.string.switch_to_owner)
                finalBinding.btnSwitchMode.setOnClickListener { switchModeToOwner() }
            }
            "none" -> {
                finalBinding.btnBecomeOwner.visibility = View.VISIBLE
                finalBinding.cardApplicationPending.visibility = View.GONE
                finalBinding.btnSwitchMode.visibility = View.GONE
            }
            else -> {
                finalBinding.btnBecomeOwner.visibility = View.GONE
                finalBinding.btnSwitchMode.visibility = View.GONE
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
