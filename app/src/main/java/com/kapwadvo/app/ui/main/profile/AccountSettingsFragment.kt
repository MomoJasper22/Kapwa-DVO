package com.kapwadvo.app.ui.main.profile

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.kapwadvo.app.UserSession
import com.kapwadvo.app.data.repository.AuthRepository
import com.kapwadvo.app.databinding.FragmentAccountSettingsBinding
import kotlinx.coroutines.launch

class AccountSettingsFragment : Fragment() {

    private var _binding: FragmentAccountSettingsBinding? = null
    private val binding get() = _binding!!

    private val PREFS_NAME = "KapwaDVOPrefs"
    private val PREF_NOTIFICATIONS = "notifications_enabled"

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            saveNotificationPreference(true)
            Toast.makeText(context, "Push notifications enabled", Toast.LENGTH_SHORT).show()
        } else {
            binding.switchNotifications.isChecked = false
            saveNotificationPreference(false)
            Toast.makeText(context, "Permission denied. Notifications are disabled.", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAccountSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        binding.etEmail.setText(UserSession.email)
        
        setupNotificationSwitch()
        setupClickListeners()
    }

    private fun setupNotificationSwitch() {
        val prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        binding.switchNotifications.isChecked = prefs.getBoolean(PREF_NOTIFICATIONS, false)

        binding.switchNotifications.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    when {
                        ContextCompat.checkSelfPermission(
                            requireContext(),
                            Manifest.permission.POST_NOTIFICATIONS
                        ) == PackageManager.PERMISSION_GRANTED -> {
                            saveNotificationPreference(true)
                        }
                        shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS) -> {
                            showPermissionRationale()
                        }
                        else -> {
                            requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    }
                } else {
                    saveNotificationPreference(true)
                }
            } else {
                saveNotificationPreference(false)
            }
        }
    }

    private fun showPermissionRationale() {
        AlertDialog.Builder(requireContext())
            .setTitle("Notification Permission Needed")
            .setMessage("We need this permission to send you updates about your bookings and listings.")
            .setPositiveButton("OK") { _, _ ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
            .setNegativeButton("Cancel") { _, _ ->
                binding.switchNotifications.isChecked = false
                saveNotificationPreference(false)
            }
            .show()
    }

    private fun saveNotificationPreference(enabled: Boolean) {
        val prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(PREF_NOTIFICATIONS, enabled).apply()
        // Here you would typically also update a backend flag or subscribe/unsubscribe to push topics
    }

    private fun setupClickListeners() {
        binding.btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }
        
        binding.btnSave.setOnClickListener {
            saveSettings()
        }

        binding.tvTerms.setOnClickListener {
            showTermsDialog()
        }

        binding.tvPrivacy.setOnClickListener {
            showPrivacyDialog()
        }
    }

    private fun showTermsDialog() {
        val termsText = """
            Welcome to KapwaDVO! By using our app, you agree to these terms:
            
            1. Use of Service: You must use the app responsibly and provide accurate information for listings and bookings.
            2. Business Owners: Are responsible for fulfilling accepted bookings and maintaining accurate listing information.
            3. Users: Must honor their bookings and provide fair and honest reviews.
            4. Content: We reserve the right to remove inappropriate listings, reviews, or comments at our discretion.
            5. Liability: KapwaDVO acts as a directory and booking facilitator. We are not liable for disputes between users and business owners.
        """.trimIndent()

        AlertDialog.Builder(requireContext())
            .setTitle("Terms of Service")
            .setMessage(termsText)
            .setPositiveButton("Close", null)
            .show()
    }

    private fun showPrivacyDialog() {
        val privacyText = """
            Your privacy is important to us.
            
            1. Data Collection: We collect your name, email, and optionally your date of birth, phone number, and address to facilitate bookings and listings.
            2. Location Data: We may use your location to show nearby businesses if you grant permission.
            3. Data Sharing: Your public profile and review content are visible to others. Your contact details are shared with business owners only when you make a booking.
            4. Security: We use Supabase to securely store and protect your data.
            5. Deletion: You can request account deletion at any time by contacting support.
        """.trimIndent()

        AlertDialog.Builder(requireContext())
            .setTitle("Privacy Policy")
            .setMessage(privacyText)
            .setPositiveButton("Close", null)
            .show()
    }

    private fun saveSettings() {
        val email = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString()
        
        if (email.isBlank()) {
            Toast.makeText(context, "Email cannot be empty", Toast.LENGTH_SHORT).show()
            return
        }
        if (password.isNotBlank() && password.length < 6) {
            Toast.makeText(context, "New password must be at least 6 characters", Toast.LENGTH_SHORT).show()
            return
        }
        
        setLoading(true)

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                // Update email / password if changed — returns true if a confirmation is needed
                val confirmationRequired = AuthRepository.updateAuthCredentials(
                    newEmail = email,
                    newPassword = password
                )
                
                setLoading(false)

                if (confirmationRequired) {
                    Toast.makeText(
                        context,
                        "Check your inbox to confirm your new email or password.",
                        Toast.LENGTH_LONG
                    ).show()
                } else {
                    Toast.makeText(context, "Settings updated successfully", Toast.LENGTH_SHORT).show()
                }
                
                parentFragmentManager.popBackStack()
            } catch (e: Exception) {
                setLoading(false)
                Toast.makeText(context, "Failed to update settings. Please try again", Toast.LENGTH_LONG).show()
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

