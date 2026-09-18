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
            onItemClick = { showDetail(it) },
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

    private fun showDetail(listing: Listing) {
        val dialog = BottomSheetDialog(requireContext())
        val sheetBinding = BottomSheetListingDetailBinding.inflate(layoutInflater)
        sheetBinding.tvName.text = listing.name
        sheetBinding.tvCategory.text = listing.category
        sheetBinding.tvDescription.text = listing.description
        sheetBinding.tvPhotoLabel.text = "[ Photo: ${listing.name} ]"

        if (!listing.address.isNullOrEmpty()) {
            sheetBinding.tvAddress.text = listing.address
            sheetBinding.layoutAddress.visibility = View.VISIBLE
        }
        if (!listing.hours.isNullOrEmpty()) {
            sheetBinding.tvHours.text = listing.hours
            sheetBinding.layoutHours.visibility = View.VISIBLE
        }
        if (!listing.contact.isNullOrEmpty()) {
            sheetBinding.tvContact.text = listing.contact
            sheetBinding.layoutContact.visibility = View.VISIBLE
        }

        if (!UserSession.isLoggedIn()) {
            sheetBinding.btnSave.visibility = View.GONE
            sheetBinding.btnBook.visibility = View.GONE
        } else {
            val isSaved = listing.id in savedIds
            sheetBinding.btnSave.text = if (isSaved) getString(R.string.remove_saved) else getString(R.string.save_location)

            sheetBinding.btnSave.setOnClickListener {
                lifecycleScope.launch {
                    toggleSave(listing, isSaved)
                    dialog.dismiss()
                }
            }
            sheetBinding.btnBook.setOnClickListener {
                dialog.dismiss()
                showBookingDialog(listing)
            }
        }

        dialog.setContentView(sheetBinding.root)
        dialog.show()
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

    private fun showBookingDialog(listing: Listing) {
        val dialogBinding = DialogBookingBinding.inflate(layoutInflater)
        val dialog = androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setView(dialogBinding.root)
            .create()

        dialogBinding.btnCancel.setOnClickListener { dialog.dismiss() }
        dialogBinding.btnConfirm.setOnClickListener {
            val date = dialogBinding.etDate.text.toString().trim()
            val notes = dialogBinding.etNotes.text.toString().trim()
            if (date.isEmpty()) {
                Toast.makeText(context, "Please enter a date", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            lifecycleScope.launch {
                try {
                    BookingRepository.createBooking(
                        BookingInsert(
                            listingId = listing.id,
                            userId = UserSession.userId!!,
                            date = date,
                            notes = notes.ifEmpty { null }
                        )
                    )
                    Toast.makeText(context, "Booking requested!", Toast.LENGTH_SHORT).show()
                    dialog.dismiss()
                } catch (e: Exception) {
                    Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
        dialog.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
