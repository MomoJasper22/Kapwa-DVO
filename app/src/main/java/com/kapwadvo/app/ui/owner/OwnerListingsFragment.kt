package com.kapwadvo.app.ui.owner

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.kapwadvo.app.UserSession
import com.kapwadvo.app.data.models.Listing
import com.kapwadvo.app.data.repository.ListingRepository
import com.kapwadvo.app.databinding.FragmentOwnerListingsBinding
import com.kapwadvo.app.ui.admin.ListingFormFragment
import kotlinx.coroutines.launch

class OwnerListingsFragment : Fragment() {

    private var _binding: FragmentOwnerListingsBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: OwnerListingAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentOwnerListingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        adapter = OwnerListingAdapter(
            onEdit = { openForm(it) },
            onDelete = { confirmDelete(it) }
        )
        binding.rvListings.layoutManager = LinearLayoutManager(requireContext())
        binding.rvListings.adapter = adapter
        binding.fab.setOnClickListener { openForm(null) }
        load()
    }

    override fun onResume() {
        super.onResume()
        load()
    }

    private fun load() {
        binding.progressBar.visibility = View.VISIBLE
        binding.tvEmpty.visibility = View.GONE
        lifecycleScope.launch {
            val listings = ListingRepository.getListingsByOwner(UserSession.userId!!)
            binding.progressBar.visibility = View.GONE
            adapter.submitList(listings)
            binding.tvEmpty.visibility = if (listings.isEmpty()) View.VISIBLE else View.GONE
        }
    }

    private fun openForm(listing: Listing?) {
        requireActivity().supportFragmentManager.beginTransaction()
            .add(android.R.id.content, ListingFormFragment.newInstance(listing))
            .addToBackStack(null)
            .commit()
    }

    private fun confirmDelete(listing: Listing) {
        AlertDialog.Builder(requireContext())
            .setTitle("Delete ${listing.name}?")
            .setPositiveButton("Delete") { _, _ ->
                lifecycleScope.launch {
                    try {
                        ListingRepository.deleteListing(listing.id)
                        Toast.makeText(context, "Deleted", Toast.LENGTH_SHORT).show()
                        load()
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
