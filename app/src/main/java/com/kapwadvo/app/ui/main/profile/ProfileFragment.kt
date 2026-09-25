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
import kotlinx.coroutines.launch

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        renderProfile()
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
        binding.btnLogout.visibility = View.GONE
        binding.btnEditProfile.visibility = View.GONE

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

        // Hide guest-only views
        binding.tvGuestTitle.visibility = View.GONE
        binding.tvGuestDesc.visibility = View.GONE
        binding.btnSignIn.visibility = View.GONE
        binding.btnLogout.visibility = View.VISIBLE
        binding.btnEditProfile.visibility = View.VISIBLE

        val isOwnerActivity = requireActivity() is BusinessOwnerActivity

        if (isOwnerActivity) {
            // We are in Business Owner Mode
            binding.btnBecomeOwner.visibility = View.GONE
            binding.tvApplicationPending.visibility = View.GONE
            binding.btnSwitchMode.visibility = View.VISIBLE
            binding.btnSwitchMode.text = getString(R.string.switch_to_user)
            binding.btnSwitchMode.setOnClickListener { switchModeToUser() }
        } else if (UserSession.isAdmin()) {
            // Admins: hide become owner entirely
            binding.btnBecomeOwner.visibility = View.GONE
            binding.tvApplicationPending.visibility = View.GONE
            binding.btnSwitchMode.visibility = View.GONE
        } else {
            // We are in User Mode
            if (UserSession.isOwner()) {
                // DB role is owner, meaning they are approved
                binding.btnBecomeOwner.visibility = View.GONE
                binding.tvApplicationPending.visibility = View.GONE
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
            Toast.makeText(context, "Edit profile coming soon", Toast.LENGTH_SHORT).show()
        }
        binding.btnLogout.setOnClickListener {
            lifecycleScope.launch {
                try {
                    AuthRepository.logout()
                    startActivity(Intent(requireContext(), AuthActivity::class.java))
                    requireActivity().finish()
                } catch (e: Exception) {
                    Toast.makeText(context, "Logout error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun switchModeToUser() {
        binding.btnSwitchMode.isEnabled = false
        lifecycleScope.launch {
            try {
                if (UserSession.role != "user") {
                    AuthRepository.switchRole("user")
                }
                Toast.makeText(context, "Switched to User mode", Toast.LENGTH_SHORT).show()
                startActivity(Intent(requireContext(), com.kapwadvo.app.ui.main.MainActivity::class.java))
                requireActivity().finish()
            } catch (e: Exception) {
                Toast.makeText(context, "Switch failed: ${e.message}", Toast.LENGTH_SHORT).show()
                binding.btnSwitchMode.isEnabled = true
            }
        }
    }

    private fun switchModeToOwner() {
        binding.btnSwitchMode.isEnabled = false
        lifecycleScope.launch {
            try {
                if (UserSession.role != "owner") {
                    AuthRepository.switchRole("owner")
                }
                Toast.makeText(context, "Switched to Business Owner mode", Toast.LENGTH_SHORT).show()
                startActivity(Intent(requireContext(), BusinessOwnerActivity::class.java))
                requireActivity().finish()
            } catch (e: Exception) {
                Toast.makeText(context, "Switch failed: ${e.message}", Toast.LENGTH_SHORT).show()
                binding.btnSwitchMode.isEnabled = true
            }
        }
    }

    private fun checkApplicationStatus() {
        lifecycleScope.launch {
            val app = ApplicationRepository.getUserApplication(UserSession.userId!!)
            when (app?.status) {
                "pending" -> {
                    binding.tvApplicationPending.visibility = View.VISIBLE
                    binding.btnBecomeOwner.visibility = View.GONE
                    binding.btnSwitchMode.visibility = View.GONE
                }
                "approved" -> {
                    // Approved but currently in user mode — show switch button
                    binding.tvApplicationPending.visibility = View.GONE
                    binding.btnBecomeOwner.visibility = View.GONE
                    binding.btnSwitchMode.visibility = View.VISIBLE
                    binding.btnSwitchMode.text = getString(R.string.switch_to_owner)
                    binding.btnSwitchMode.setOnClickListener { switchModeToOwner() }
                }
                null -> {
                    binding.btnBecomeOwner.visibility = View.VISIBLE
                    binding.tvApplicationPending.visibility = View.GONE
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
