package com.kapwadvo.app.ui.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.kapwadvo.app.data.repository.AuthRepository
import com.kapwadvo.app.databinding.FragmentVerifyEmailBinding
import com.kapwadvo.app.ui.onboarding.OnboardingActivity
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import android.content.Intent

class VerifyEmailFragment : Fragment() {

    private var _binding: FragmentVerifyEmailBinding? = null
    private val binding get() = _binding!!

    private var pollingJob: Job? = null
    
    object PendingStore {
        var email: String = ""
        var pass: String = ""
        
        fun clear() {
            email = ""
            pass = ""
        }
    }

    companion object {
        fun newInstance(): VerifyEmailFragment {
            return VerifyEmailFragment()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentVerifyEmailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.tvEmail.text = PendingStore.email

        startPolling()

        binding.btnResend.setOnClickListener { resendEmail() }
        binding.btnBackToLogin.setOnClickListener {
            pollingJob?.cancel()
            (activity as? AuthActivity)?.binding?.tabLayout?.getTabAt(0)?.select()
        }
    }

    private fun startPolling() {
        pollingJob?.cancel()
        pollingJob = viewLifecycleOwner.lifecycleScope.launch {
            while (isActive) {
                delay(4000) // poll every 4 seconds
                checkConfirmation()
            }
        }
    }

    private suspend fun checkConfirmation() {
        try {
            // Because we don't have a session yet, the only reliable way to check 
            // if the email was confirmed is to attempt to log in.
            // If it succeeds, the email has been confirmed!
            AuthRepository.login(PendingStore.email, PendingStore.pass)
            
            pollingJob?.cancel()
            proceedToOnboarding()
        } catch (_: Exception) { 
            // Login fails if the email isn't confirmed yet. 
            // Ignore and wait for the next poll.
        }
    }

    private fun proceedToOnboarding() {
        val activity = activity ?: return
        activity.startActivity(Intent(activity, OnboardingActivity::class.java))
        activity.finish()
    }

    private fun resendEmail() {
        binding.btnResend.isEnabled = false
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                AuthRepository.resendConfirmationEmail(PendingStore.email)
                Toast.makeText(context, "Confirmation email resent", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to resend confirmation email", Toast.LENGTH_LONG).show()
            } finally {
                // Re-enable after 30 seconds to prevent spam
                delay(30_000)
                _binding?.btnResend?.isEnabled = true
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        pollingJob?.cancel()
        _binding = null
        if (requireActivity().isFinishing) {
            PendingStore.clear()
        }
    }
}
