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
import java.util.Calendar

class EditProfileFragment : Fragment() {

    private var _binding: FragmentEditProfileBinding? = null
    private val binding get() = _binding!!

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
        binding.etEmail.setText(UserSession.email)
        binding.etDob.setText(UserSession.dob)
        binding.etPhone.setText(UserSession.phoneNumber)
        binding.etAddress.setText(UserSession.address)
        // Password left blank intentionally
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

        binding.btnSave.setOnClickListener {
            saveChanges()
        }
    }

    private fun saveChanges() {
        val firstName = binding.etFirstName.text.toString().trim()
        val lastName = binding.etLastName.text.toString().trim()
        val email = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString()
        val dob = binding.etDob.text.toString().trim()
        val phone = binding.etPhone.text.toString().trim()
        val address = binding.etAddress.text.toString().trim()

        if (firstName.isBlank() || lastName.isBlank()) {
            Toast.makeText(context, "First name and last name cannot be empty.", Toast.LENGTH_SHORT).show()
            return
        }
        if (email.isBlank()) {
            Toast.makeText(context, "Email cannot be empty.", Toast.LENGTH_SHORT).show()
            return
        }
        if (password.isNotBlank() && password.length < 6) {
            Toast.makeText(context, "New password must be at least 6 characters.", Toast.LENGTH_SHORT).show()
            return
        }

        setLoading(true)

        lifecycleScope.launch {
            try {
                // 1. Update profile table fields
                AuthRepository.updateProfile(
                    firstName = firstName,
                    lastName = lastName,
                    dob = dob,
                    phoneNumber = phone,
                    address = address
                )

                // 2. Update email / password if changed — returns true if a confirmation is needed
                val confirmationRequired = AuthRepository.updateAuthCredentials(
                    newEmail = email,
                    newPassword = password
                )

                setLoading(false)

                if (confirmationRequired) {
                    Toast.makeText(
                        context,
                        "Profile updated! Check your inbox to confirm the email/password change.",
                        Toast.LENGTH_LONG
                    ).show()
                } else {
                    Toast.makeText(context, "Profile updated successfully!", Toast.LENGTH_SHORT).show()
                }

                parentFragmentManager.popBackStack()

            } catch (e: Exception) {
                setLoading(false)
                Toast.makeText(context, "Update failed: ${e.message}", Toast.LENGTH_LONG).show()
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
