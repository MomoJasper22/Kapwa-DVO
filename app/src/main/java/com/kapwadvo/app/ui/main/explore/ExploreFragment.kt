package com.kapwadvo.app.ui.main.explore

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import java.util.Calendar
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.chip.Chip
import com.kapwadvo.app.R
import com.kapwadvo.app.UserSession
import com.kapwadvo.app.data.models.Booking
import com.kapwadvo.app.data.models.BookingInsert
import com.kapwadvo.app.data.models.Listing
import com.kapwadvo.app.data.repository.BookingRepository
import com.kapwadvo.app.data.repository.ListingRepository
import com.kapwadvo.app.data.repository.SavedRepository
import com.kapwadvo.app.databinding.BottomSheetListingDetailBinding
import com.kapwadvo.app.databinding.DialogBookingBinding
import com.kapwadvo.app.databinding.FragmentExploreBinding
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class ExploreFragment : Fragment() {

    private var _binding: FragmentExploreBinding? = null
    private val binding get() = _binding!!

    private val categories = listOf("All", "Tourist Spot", "Business", "Hidden Gem", "Food", "Accommodation")
    private var selectedCategory = "All"
    private var allListings = emptyList<Listing>()
    private var savedIds = mutableSetOf<String>()
    private var searchJob: Job? = null
    private lateinit var adapter: ListingAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentExploreBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupChips()
        setupRecyclerView()
        setupSearch()
        loadData()

        parentFragmentManager.setFragmentResultListener("detail_dismissed", viewLifecycleOwner) { _, _ ->
            if (UserSession.isLoggedIn()) {
                lifecycleScope.launch {
                    val saved = SavedRepository.getSavedForUser(UserSession.userId!!)
                    savedIds = saved.map { it.listingId }.toMutableSet()
                    adapter.setSavedIds(savedIds)
                }
            }
        }
    }

    private fun setupChips() {
        categories.forEach { cat ->
            val chip = Chip(requireContext()).apply {
                text = cat
                isCheckable = true
                isChecked = cat == selectedCategory
            }
            chip.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    selectedCategory = cat
                    filterListings()
                }
            }
            binding.chipGroupCategories.addView(chip)
        }
        // Check "All" chip by default
        (binding.chipGroupCategories.getChildAt(0) as? Chip)?.isChecked = true
    }

    private fun setupRecyclerView() {
        adapter = ListingAdapter(
            onItemClick = { showDetail(it, false) },
            onItemDoubleClick = { showDetail(it, true) },
            onSaveClick = { listing, isSaved -> toggleSave(listing, isSaved) },
            isLoggedIn = UserSession.isLoggedIn()
        )
        binding.rvListings.layoutManager = LinearLayoutManager(requireContext())
        binding.rvListings.adapter = adapter
    }

    private fun setupSearch() {
        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                searchJob?.cancel()
                searchJob = lifecycleScope.launch {
                    delay(300)
                    filterListings()
                }
            }
        })
    }

    private fun loadData() {
        binding.progressBar.visibility = View.VISIBLE
        binding.tvEmpty.visibility = View.GONE

        lifecycleScope.launch {
            allListings = ListingRepository.getApprovedListings()

            if (UserSession.isLoggedIn()) {
                val saved = SavedRepository.getSavedForUser(UserSession.userId!!)
                savedIds = saved.map { it.listingId }.toMutableSet()
                adapter.setSavedIds(savedIds)
            }

            binding.progressBar.visibility = View.GONE
            filterListings()
        }
    }

    private fun filterListings() {
        val query = binding.etSearch.text.toString().trim().lowercase()
        val filtered = allListings.filter { listing ->
            val matchCat = selectedCategory == "All" || listing.category == selectedCategory
            val matchSearch = query.isEmpty() || listing.name.lowercase().contains(query) ||
                listing.description.lowercase().contains(query)
            matchCat && matchSearch
        }

        adapter.submitList(filtered)
        binding.tvEmpty.visibility = if (filtered.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun showDetail(listing: Listing, expandImmediately: Boolean = false) {
        val sheet = ListingDetailSheet.newInstance(listing, expandImmediately)
        sheet.show(parentFragmentManager, "ListingDetailSheet")
    }

    private fun toggleSave(listing: Listing, isSaved: Boolean) {
        val uid = UserSession.userId ?: return
        lifecycleScope.launch {
            try {
                if (isSaved) {
                    SavedRepository.removeSaved(uid, listing.id)
                    savedIds.remove(listing.id)
                    Toast.makeText(context, "Removed from saved", Toast.LENGTH_SHORT).show()
                } else {
                    SavedRepository.saveLocation(uid, listing.id)
                    savedIds.add(listing.id)
                    Toast.makeText(context, "Saved!", Toast.LENGTH_SHORT).show()
                }
                adapter.setSavedIds(savedIds)
            } catch (e: Exception) {
                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }



    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
