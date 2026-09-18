package com.kapwadvo.app.ui.main.profile

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
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
        val name = UserSession.name.ifEmpty { UserSession.email }
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

        // Role-based buttons
        when {
            UserSession.isOwner() -> {
                binding.btnOwnerDashboard.visibility = View.VISIBLE
                binding.btnBecomeOwner.visibility = View.GONE
                binding.tvApplicationPending.visibility = View.GONE
            }
            UserSession.isUser() -> {
                checkApplicationStatus()
            }
        }

        binding.btnOwnerDashboard.setOnClickListener {
            startActivity(Intent(requireContext(), BusinessOwnerActivity::class.java))
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

    private fun checkApplicationStatus() {
        lifecycleScope.launch {
            val app = ApplicationRepository.getUserApplication(UserSession.userId!!)
            when (app?.status) {
                "pending" -> {
                    binding.tvApplicationPending.visibility = View.VISIBLE
                    binding.btnBecomeOwner.visibility = View.GONE
                }
                null -> {
                    binding.btnBecomeOwner.visibility = View.VISIBLE
                    binding.tvApplicationPending.visibility = View.GONE
                }
                else -> {
                    binding.btnBecomeOwner.visibility = View.GONE
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
