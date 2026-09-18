package com.kapwadvo.app.ui.admin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.kapwadvo.app.data.repository.AdminRepository
import com.kapwadvo.app.data.repository.ApplicationRepository
import com.kapwadvo.app.databinding.FragmentAdminAppsBinding
import kotlinx.coroutines.launch

class AdminAppsFragment : Fragment() {

    private var _binding: FragmentAdminAppsBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: AdminAppAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentAdminAppsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        adapter = AdminAppAdapter(
            onApprove = { app ->
                lifecycleScope.launch {
                    try {
                        ApplicationRepository.updateApplicationStatus(app.id, "approved")
                        AdminRepository.setUserRole(app.userId, "owner")
                        Toast.makeText(context, "Approved — user is now a Business Owner", Toast.LENGTH_SHORT).show()
                        load()
                    } catch (e: Exception) {
                        Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            },
            onReject = { app ->
                lifecycleScope.launch {
                    try {
                        ApplicationRepository.updateApplicationStatus(app.id, "rejected")
                        Toast.makeText(context, "Rejected", Toast.LENGTH_SHORT).show()
                        load()
                    } catch (e: Exception) {
                        Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        )
        binding.rvApps.layoutManager = LinearLayoutManager(requireContext())
        binding.rvApps.adapter = adapter
        load()
    }

    private fun load() {
        binding.progressBar.visibility = View.VISIBLE
        binding.tvEmpty.visibility = View.GONE
        lifecycleScope.launch {
            val apps = ApplicationRepository.getPendingApplications()
            binding.progressBar.visibility = View.GONE
            adapter.submitList(apps)
            binding.tvEmpty.visibility = if (apps.isEmpty()) View.VISIBLE else View.GONE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
