package com.kapwadvo.app.ui.admin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.kapwadvo.app.data.models.Listing
import com.kapwadvo.app.data.repository.ListingRepository
import com.kapwadvo.app.databinding.FragmentAdminListingsBinding
import com.kapwadvo.app.ui.main.explore.ListingDetailSheet
import kotlinx.coroutines.launch

class AdminListingsFragment : Fragment() {

    private var _binding: FragmentAdminListingsBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: AdminListingAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentAdminListingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        adapter = AdminListingAdapter(
            onCardClick = { listing -> openDetailSheet(listing) },
            onEdit = { listing -> openForm(listing) },
            onDelete = { listing -> confirmDelete(listing) }
        )
        binding.rvListings.layoutManager = LinearLayoutManager(requireContext())
        binding.rvListings.adapter = adapter
        binding.fab.setOnClickListener { openForm(null) }
        loadListings()
    }

    private fun loadListings() {
        binding.progressBar.visibility = View.VISIBLE
        binding.tvEmpty.visibility = View.GONE
        lifecycleScope.launch {
            val listings = ListingRepository.getAllListings()
            binding.progressBar.visibility = View.GONE
            adapter.submitList(listings)
            binding.tvEmpty.visibility = if (listings.isEmpty()) View.VISIBLE else View.GONE
        }
    }

    private fun openDetailSheet(listing: Listing) {
        ListingDetailSheet.newInstance(listing, expandImmediately = true)
            .show(childFragmentManager, "detail")
    }

    private fun openForm(listing: Listing?) {
        val fragment = ListingFormFragment.newInstance(listing)
        requireActivity().supportFragmentManager.beginTransaction()
            .add(android.R.id.content, fragment)
            .addToBackStack(null)
            .commit()
    }

    private fun confirmDelete(listing: Listing) {
        AlertDialog.Builder(requireContext())
            .setTitle("Delete ${listing.name}?")
            .setMessage("This action cannot be undone.")
            .setPositiveButton("Delete") { _, _ ->
                lifecycleScope.launch {
                    try {
                        ListingRepository.deleteListing(listing.id)
                        Toast.makeText(context, "Deleted", Toast.LENGTH_SHORT).show()
                        loadListings()
                    } catch (e: Exception) {
                        Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
