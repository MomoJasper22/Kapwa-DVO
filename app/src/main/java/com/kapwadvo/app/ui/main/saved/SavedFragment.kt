package com.kapwadvo.app.ui.main.saved

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.kapwadvo.app.UserSession
import com.kapwadvo.app.data.repository.ListingRepository
import com.kapwadvo.app.data.repository.SavedRepository
import com.kapwadvo.app.databinding.FragmentSavedBinding
import com.kapwadvo.app.ui.auth.AuthActivity
import kotlinx.coroutines.launch

class SavedFragment : Fragment() {

    private var _binding: FragmentSavedBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: SavedAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentSavedBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (!UserSession.isLoggedIn()) {
            binding.layoutGuest.visibility = View.VISIBLE
            binding.layoutSaved.visibility = View.GONE
            binding.btnSignIn.setOnClickListener {
                startActivity(Intent(requireContext(), AuthActivity::class.java))
                requireActivity().finish()
            }
            return
        }

        binding.layoutGuest.visibility = View.GONE
        binding.layoutSaved.visibility = View.VISIBLE

        adapter = SavedAdapter(
            onItemClick = { /* detail can be shown via ExploreFragment */ },
            onRemoveClick = { listing ->
                lifecycleScope.launch {
                    try {
                        SavedRepository.removeSaved(UserSession.userId!!, listing.id)
                        Toast.makeText(context, "Removed", Toast.LENGTH_SHORT).show()
                        loadSaved()
                    } catch (e: Exception) {
                        Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        )
        binding.rvSaved.layoutManager = LinearLayoutManager(requireContext())
        binding.rvSaved.adapter = adapter
        loadSaved()
    }

    private fun loadSaved() {
        binding.progressBar.visibility = View.VISIBLE
        binding.tvEmpty.visibility = View.GONE
        lifecycleScope.launch {
            val saved = SavedRepository.getSavedForUser(UserSession.userId!!)
            val listings = ListingRepository.getListingsByIds(saved.map { it.listingId })
            binding.progressBar.visibility = View.GONE
            adapter.submitList(listings)
            binding.tvEmpty.visibility = if (listings.isEmpty()) View.VISIBLE else View.GONE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
