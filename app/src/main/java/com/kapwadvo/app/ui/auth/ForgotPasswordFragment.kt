package com.kapwadvo.app.ui.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.kapwadvo.app.R
import com.kapwadvo.app.data.repository.AuthRepository
import com.kapwadvo.app.databinding.FragmentForgotPasswordBinding
import kotlinx.coroutines.launch

class ForgotPasswordFragment : Fragment() {

    private var _binding: FragmentForgotPasswordBinding? = null
    private val binding get() = _binding!!

    private enum class State { EMAIL, CODE, NEW_PASSWORD }
    private var currentState = State.EMAIL
    private var email = ""
    private var resendJob: kotlinx.coroutines.Job? = null

    companion object {
        fun newInstance() = ForgotPasswordFragment()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentForgotPasswordBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        binding.btnSubmit.setOnClickListener {
            when (currentState) {
                State.EMAIL -> sendCode()
                State.CODE -> verifyCode()
                State.NEW_PASSWORD -> updatePassword()
            }
        }

        binding.btnResend.setOnClickListener {
            viewLifecycleOwner.lifecycleScope.launch {
                try {
                    AuthRepository.sendPasswordResetCode(email)
                    startResendTimer()
                    Toast.makeText(context, "Code resent", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(context, "Failed to resend code: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun startResendTimer() {
        resendJob?.cancel()
        binding.btnResend.isEnabled = false
        resendJob = viewLifecycleOwner.lifecycleScope.launch {
            for (i in 60 downTo 1) {
                if (!isAdded) break
                binding.btnResend.text = getString(R.string.resend_code_countdown, i)
                kotlinx.coroutines.delay(1000)
            }
            if (isAdded) {
                binding.btnResend.isEnabled = true
                binding.btnResend.text = getString(R.string.resend_code)
            }
        }
    }

    private fun sendCode() {
        email = binding.etEmail.text.toString().trim()
        if (email.isEmpty()) {
            Toast.makeText(context, "Please enter your email", Toast.LENGTH_SHORT).show()
            return
        }

        binding.btnSubmit.isEnabled = false
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                AuthRepository.sendPasswordResetCode(email)
                currentState = State.CODE
                binding.tilEmail.visibility = View.GONE
                binding.tvMessage.visibility = View.VISIBLE
                binding.tilCode.visibility = View.VISIBLE
                binding.btnResend.visibility = View.VISIBLE
                binding.btnSubmit.text = getString(R.string.verify_code)
                startResendTimer()
                Toast.makeText(context, "Code sent", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to send code: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                binding.btnSubmit.isEnabled = true
            }
        }
    }

    private fun verifyCode() {
        val code = binding.etCode.text.toString().trim()
        if (code.isEmpty()) {
            Toast.makeText(context, "Please enter the code", Toast.LENGTH_SHORT).show()
            return
        }

        binding.btnSubmit.isEnabled = false
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                AuthRepository.verifyResetCode(email, code)
                currentState = State.NEW_PASSWORD
                binding.tvMessage.visibility = View.GONE
                binding.tilCode.visibility = View.GONE
                binding.btnResend.visibility = View.GONE
                resendJob?.cancel()
                binding.tilNewPassword.visibility = View.VISIBLE
                binding.tilConfirmPassword.visibility = View.VISIBLE
                binding.btnSubmit.text = getString(R.string.update_password)
            } catch (e: Exception) {
                Toast.makeText(context, "Invalid code: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                binding.btnSubmit.isEnabled = true
            }
        }
    }

    private fun updatePassword() {
        val newPassword = binding.etNewPassword.text.toString()
        val confirmPassword = binding.etConfirmPassword.text.toString()

        if (newPassword.isEmpty() || confirmPassword.isEmpty()) {
            Toast.makeText(context, "Please fill in all fields", Toast.LENGTH_SHORT).show()
            return
        }
        
        if (newPassword != confirmPassword) {
            Toast.makeText(context, getString(R.string.passwords_do_not_match), Toast.LENGTH_SHORT).show()
            return
        }

        binding.btnSubmit.isEnabled = false
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                AuthRepository.setNewPassword(newPassword)
                Toast.makeText(context, "Password updated successfully!", Toast.LENGTH_LONG).show()
                parentFragmentManager.popBackStack()
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to update password: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                binding.btnSubmit.isEnabled = true
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        resendJob?.cancel()
        _binding = null
    }
}
