package com.kapwadvo.app.ui.main.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.kapwadvo.app.UserSession
import com.kapwadvo.app.data.repository.ApplicationRepository
import com.kapwadvo.app.databinding.FragmentOwnerApplicationBinding
import kotlinx.coroutines.launch

class OwnerApplicationFragment : Fragment() {

    private var _binding: FragmentOwnerApplicationBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentOwnerApplicationBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.btnSubmit.setOnClickListener { submitApplication() }
    }

    private fun submitApplication() {
        binding.btnSubmit.isEnabled = false
        lifecycleScope.launch {
            try {
                ApplicationRepository.submitApplication(UserSession.userId!!)
                Toast.makeText(context, "Application submitted! We'll review it shortly.", Toast.LENGTH_LONG).show()
                parentFragmentManager.popBackStack()
            } catch (e: Exception) {
                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                binding.btnSubmit.isEnabled = true
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
