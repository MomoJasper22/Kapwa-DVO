package com.kapwadvo.app.ui.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.kapwadvo.app.UserSession
import com.kapwadvo.app.data.repository.AuthRepository
import com.kapwadvo.app.databinding.FragmentSignupBinding
import kotlinx.coroutines.launch

class SignupFragment : Fragment() {

    private var _binding: FragmentSignupBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSignupBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnSignup.setOnClickListener { performSignup() }
        binding.tvLogin.setOnClickListener {
            (activity as? AuthActivity)?.binding?.tabLayout?.getTabAt(0)?.select()
        }
    }

    private fun performSignup() {
        val firstName = binding.etFirstName.text.toString().trim()
        val lastName = binding.etLastName.text.toString().trim()
        val email = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString()

        if (firstName.isEmpty() || lastName.isEmpty() || email.isEmpty() || password.isEmpty()) {
            Toast.makeText(context, "Please fill in all fields", Toast.LENGTH_SHORT).show()
            return
        }
        if (password.length < 6) {
            Toast.makeText(context, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show()
            return
        }

        binding.btnSignup.isEnabled = false

        lifecycleScope.launch {
            try {
                AuthRepository.signup(email, password, firstName, lastName)
                if (UserSession.userId != null) {
                    (activity as? AuthActivity)?.navigateToMain()
                } else {
                    Toast.makeText(
                        context,
                        "Account created! Please check your email to confirm, then log in.",
                        Toast.LENGTH_LONG
                    ).show()
                    binding.btnSignup.isEnabled = true
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Signup failed: ${e.message}", Toast.LENGTH_LONG).show()
                binding.btnSignup.isEnabled = true
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
