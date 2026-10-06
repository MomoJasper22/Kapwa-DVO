package com.kapwadvo.app.ui.main.explore

import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.view.GestureDetector
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.kapwadvo.app.R
import com.kapwadvo.app.UserSession
import com.kapwadvo.app.data.models.BookingInsert
import com.kapwadvo.app.data.models.Listing
import com.kapwadvo.app.data.models.ReviewCommentInsert
import com.kapwadvo.app.data.models.ReviewInsert
import com.kapwadvo.app.data.models.ReviewWithAuthor
import com.kapwadvo.app.data.repository.BookingRepository
import com.kapwadvo.app.data.repository.ReviewRepository
import com.kapwadvo.app.databinding.BottomSheetListingDetailBinding
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.overlay.Marker
import java.util.Calendar

class ListingDetailSheet : BottomSheetDialogFragment() {

    companion object {
        private const val ARG_JSON = "listing_json"
        private const val ARG_EXPAND = "expand_immediately"
        private val json = Json { ignoreUnknownKeys = true }

        fun newInstance(listing: Listing, expandImmediately: Boolean = false): ListingDetailSheet {
            return ListingDetailSheet().apply {
                arguments = Bundle().apply {
                    putString(ARG_JSON, json.encodeToString(listing))
                    putBoolean(ARG_EXPAND, expandImmediately)
                }
            }
        }
    }

    private var _binding: BottomSheetListingDetailBinding? = null
    private val binding get() = _binding!!

    private val listing: Listing by lazy {
        json.decodeFromString<Listing>(requireArguments().getString(ARG_JSON)!!)
    }

    private val viewModel: ListingDetailViewModel by viewModels()

    private lateinit var reviewAdapter: ReviewAdapter
    private lateinit var commentAdapter: ReviewCommentAdapter
    

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetListingDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onStart() {
        super.onStart()
        val d = dialog as? BottomSheetDialog ?: return
        val sheet = d.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet) ?: return
        sheet.layoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT

        val behavior = BottomSheetBehavior.from(sheet)
        behavior.peekHeight = (420 * resources.displayMetrics.density).toInt()
        behavior.isFitToContents = false
        behavior.isHideable = true
        behavior.skipCollapsed = false

        if (arguments?.getBoolean(ARG_EXPAND) == true) {
            behavior.state = BottomSheetBehavior.STATE_EXPANDED
        }

        // Double-tap drag handle → toggle full screen
        val gesture = GestureDetector(requireContext(), object : GestureDetector.SimpleOnGestureListener() {
            override fun onDoubleTap(e: MotionEvent): Boolean {
                behavior.state = if (behavior.state == BottomSheetBehavior.STATE_EXPANDED)
                    BottomSheetBehavior.STATE_COLLAPSED
                else
                    BottomSheetBehavior.STATE_EXPANDED
                return true
            }
        })
        binding.dragHandle.setOnTouchListener { _, ev ->
            gesture.onTouchEvent(ev)
            false
        }

        // Back button logic
        binding.btnBack.setOnClickListener {
            dismiss()
        }

        behavior.addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
            override fun onStateChanged(bottomSheet: View, newState: Int) {
                if (newState == BottomSheetBehavior.STATE_EXPANDED) {
                    binding.btnBack.visibility = View.VISIBLE
                    binding.dragHandle.visibility = View.GONE
                } else {
                    binding.btnBack.visibility = View.GONE
                    binding.dragHandle.visibility = View.VISIBLE
                }
            }
            override fun onSlide(bottomSheet: View, slideOffset: Float) {}
        })

        // Initial setup for back button visibility
        if (behavior.state == BottomSheetBehavior.STATE_EXPANDED) {
            binding.btnBack.visibility = View.VISIBLE
            binding.dragHandle.visibility = View.GONE
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupBasicInfo()
        setupAdapters()
        setupMap()
        setupActionButtons()
        setupReviewSubmit()
        setupCommentPost()

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collectLatest { state ->
                    updateUi(state)
                }
            }
        }
        
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                com.kapwadvo.app.KapwaDVOApp.networkMonitor.isOnline.collect { isOnline ->
                    binding.btnBook.isEnabled = isOnline
                    binding.btnSave.isEnabled = isOnline
                    binding.btnSubmitReview.isEnabled = isOnline
                    binding.btnPostComment.isEnabled = isOnline
                    
                    val offlineMsg = "Requires internet connection"
                    if (!isOnline) {
                        binding.btnBook.contentDescription = offlineMsg
                        binding.btnSave.contentDescription = offlineMsg
                    }
                }
            }
        }

        if (savedInstanceState == null) {
            viewModel.loadData(listing.id)
        }
    }

    private fun setupMap() {
        binding.mapView.setTileSource(TileSourceFactory.MAPNIK)
        binding.mapView.setMultiTouchControls(true)
        
        val lat = listing.lat ?: 7.1907
        val lng = listing.lng ?: 125.4553
        val point = GeoPoint(lat, lng)
        binding.mapView.controller.setZoom(18.0)
        binding.mapView.controller.setCenter(point)
        
        val marker = Marker(binding.mapView)
        marker.position = point
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        marker.title = listing.name
        binding.mapView.overlays.add(marker)
        binding.mapView.invalidate()
    }

    override fun onResume() {
        super.onResume()
        binding.mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        binding.mapView.onPause()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.mapView.onDetach()
        _binding = null
    }

    override fun onDismiss(dialog: android.content.DialogInterface) {
        super.onDismiss(dialog)
        parentFragmentManager.setFragmentResult("detail_dismissed", Bundle())
    }

    // ── Setup ─────────────────────────────────────────────────────────────────

    private fun setupBasicInfo() {
        binding.tvName.text = listing.name
        binding.tvCategory.text = com.kapwadvo.app.data.CategoryManager.toDisplayString(listing.category)
        binding.tvDescription.text = listing.description
        if (listing.photoUrls.isNotEmpty()) {
            binding.ivPhoto.visibility = View.VISIBLE
            binding.tvPhotoLabel.visibility = View.GONE
            com.bumptech.glide.Glide.with(this)
                .load(listing.photoUrls.first())
                .into(binding.ivPhoto)
        } else {
            binding.ivPhoto.visibility = View.GONE
            binding.tvPhotoLabel.visibility = View.VISIBLE
            binding.tvPhotoLabel.text = "[ Photo: ${listing.name} ]"
        }

        if (!listing.address.isNullOrEmpty()) {
            binding.tvAddress.text = listing.address
            binding.layoutAddress.visibility = View.VISIBLE
        }
        if (!listing.hours.isNullOrEmpty()) {
            binding.tvHours.text = listing.hours
            binding.layoutHours.visibility = View.VISIBLE
        }
        if (!listing.contact.isNullOrEmpty()) {
            binding.tvContact.text = listing.contact
            binding.layoutContact.visibility = View.VISIBLE
        }

        viewLifecycleOwner.lifecycleScope.launch {
            val ownerId = listing.ownerId
            if (ownerId != null) {
                if (listing.ownerName != null) {
                    binding.tvOwnerName.text = "By ${listing.ownerName}"
                    binding.tvOwnerName.visibility = View.VISIBLE
                } else {
                    val profile = com.kapwadvo.app.data.repository.AuthRepository.getProfile(ownerId)
                    if (profile != null && profile.fullName.isNotBlank()) {
                        binding.tvOwnerName.text = "By ${profile.fullName}"
                        binding.tvOwnerName.visibility = View.VISIBLE
                    } else {
                        binding.tvOwnerName.visibility = View.GONE
                    }
                }
            } else {
                binding.tvOwnerName.visibility = View.GONE
            }
        }
    }

    private fun setupAdapters() {
        val isAdmin = UserSession.isAdmin()
        reviewAdapter = ReviewAdapter(
            isAdmin = isAdmin,
            onReplyClick = { review -> showReplyBar(review) },
            onDeleteClick = { review -> 
                viewLifecycleOwner.lifecycleScope.launch {
                    try {
                        ReviewRepository.deleteReview(review.id)
                        Toast.makeText(context, "Review deleted", Toast.LENGTH_SHORT).show()
                        viewModel.loadData(listing.id)
                    } catch (e: Exception) {
                        Toast.makeText(context, "Failed to delete review. Please try again", Toast.LENGTH_LONG).show()
                    }
                }
            }
        )
        commentAdapter = ReviewCommentAdapter(
            isAdmin = isAdmin,
            onDeleteClick = { comment ->
                viewLifecycleOwner.lifecycleScope.launch {
                    try {
                        ReviewRepository.deleteComment(comment.id)
                        Toast.makeText(context, "Comment deleted", Toast.LENGTH_SHORT).show()
                        viewModel.loadData(listing.id)
                    } catch (e: Exception) {
                        Toast.makeText(context, "Failed to delete comment. Please try again", Toast.LENGTH_LONG).show()
                    }
                }
            }
        )
        binding.rvReviews.layoutManager = LinearLayoutManager(requireContext())
        binding.rvReviews.adapter = reviewAdapter
        binding.rvComments.layoutManager = LinearLayoutManager(requireContext())
        binding.rvComments.adapter = commentAdapter
    }

    private fun showReplyBar(review: ReviewWithAuthor) {
        if (!UserSession.isLoggedIn()) {
            startActivity(android.content.Intent(requireContext(), com.kapwadvo.app.ui.auth.AuthActivity::class.java))
            return
        }
        binding.layoutAddComment.visibility = View.VISIBLE
        binding.etComment.hint = "Reply to ${review.authorName}..."
        binding.etComment.requestFocus()
        val imm = requireContext().getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
        imm.showSoftInput(binding.etComment, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT)
    }

    // ── UI Updates ────────────────────────────────────────────────────────────

    private fun updateUi(state: ListingDetailUiState) {
        if (state.isLoading) return

        binding.btnSave.visibility = View.VISIBLE
        binding.btnSave.text = if (state.isSaved && UserSession.isLoggedIn()) getString(R.string.remove_saved) else getString(R.string.save_location)

        if (listing.bookingsEnabled) {
            binding.btnBook.visibility = View.VISIBLE
            if (state.hasBooked) {
                binding.btnBook.text = "View Booking"
                binding.btnBook.setOnClickListener {
                    dismiss()
                    parentFragmentManager.beginTransaction()
                        .replace(com.kapwadvo.app.R.id.fragmentContainer, MyReservationsFragment())
                        .addToBackStack(null)
                        .commit()
                }
            } else {
                binding.btnBook.text = "Book"
                binding.btnBook.setOnClickListener { 
                    if (!UserSession.isLoggedIn()) {
                        startActivity(android.content.Intent(requireContext(), com.kapwadvo.app.ui.auth.AuthActivity::class.java))
                    } else {
                        showBookingDialog()
                    }
                }
            }
        } else {
            binding.btnBook.visibility = View.GONE
        }

        if (state.reviews.isNotEmpty()) {
            binding.tvAverageScore.text = String.format("%.1f", state.avgRating)
            binding.ratingAvg.rating = state.avgRating.toFloat()
            binding.tvReviewCount.text = "(${state.reviewCount})"
        } else {
            binding.tvAverageScore.text = "–"
            binding.tvReviewCount.text = "(0 reviews)"
        }

        binding.tvReviewsLabel.visibility = View.VISIBLE
        if (state.reviews.isEmpty()) {
            binding.tvNoReviews.visibility = View.VISIBLE
        } else {
            binding.tvNoReviews.visibility = View.GONE
            reviewAdapter.submitList(state.reviews)
        }
        commentAdapter.submitList(state.comments)

        binding.layoutYourReview.visibility = View.VISIBLE
        if (UserSession.isLoggedIn()) {
            val existing = state.reviews.find { it.userId == UserSession.userId }
            if (existing != null) {
                binding.ratingInput.rating = existing.rating.toFloat()
                binding.etReviewComment.setText(existing.comment ?: "")
                binding.btnSubmitReview.text = "Update Review"
            }
        }
    }

    private fun setupActionButtons() {
        binding.btnSave.setOnClickListener {
            if (!UserSession.isLoggedIn()) {
                startActivity(android.content.Intent(requireContext(), com.kapwadvo.app.ui.auth.AuthActivity::class.java))
                return@setOnClickListener
            }
            binding.btnSave.isEnabled = false
            viewModel.toggleSave(listing.id,
                onSuccess = { binding.btnSave.isEnabled = true },
                onError = {
                    Toast.makeText(context, "Failed to update saved. Please try again", Toast.LENGTH_LONG).show()
                    binding.btnSave.isEnabled = true
                }
            )
        }

        // btnBook logic is now handled dynamically in updateUi() based on hasBooked state
    }

    private fun setupReviewSubmit() {
        binding.btnSubmitReview.setOnClickListener {
            if (!UserSession.isLoggedIn()) {
                startActivity(android.content.Intent(requireContext(), com.kapwadvo.app.ui.auth.AuthActivity::class.java))
                return@setOnClickListener
            }
            val rating = binding.ratingInput.rating.toInt()
            if (rating == 0) {
                Toast.makeText(context, "Please select a star rating", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val comment = binding.etReviewComment.text.toString().trim()
            val uid = UserSession.userId ?: return@setOnClickListener

            binding.btnSubmitReview.isEnabled = false
            viewModel.submitReview(
                ReviewInsert(listing.id, uid, rating, comment.ifEmpty { null }),
                onSuccess = {
                    Toast.makeText(context, "Review submitted", Toast.LENGTH_SHORT).show()
                    binding.btnSubmitReview.text = "Update Review"
                    binding.btnSubmitReview.isEnabled = true
                },
                onError = {
                    Toast.makeText(context, "Failed to submit review. Please try again", Toast.LENGTH_LONG).show()
                    binding.btnSubmitReview.isEnabled = true
                }
            )
        }
    }

    private fun setupCommentPost() {
        binding.btnPostComment.setOnClickListener {
            if (!UserSession.isLoggedIn()) {
                startActivity(android.content.Intent(requireContext(), com.kapwadvo.app.ui.auth.AuthActivity::class.java))
                return@setOnClickListener
            }
            val text = binding.etComment.text.toString().trim()
            if (text.isEmpty()) {
                Toast.makeText(context, "Please write something", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val uid = UserSession.userId ?: return@setOnClickListener

            binding.btnPostComment.isEnabled = false
            viewModel.postComment(
                ReviewCommentInsert(listing.id, uid, text),
                onSuccess = {
                    binding.etComment.setText("")
                    Toast.makeText(context, "Comment posted", Toast.LENGTH_SHORT).show()
                    binding.btnPostComment.isEnabled = true
                },
                onError = {
                    Toast.makeText(context, "Failed to post comment. Please try again", Toast.LENGTH_LONG).show()
                    binding.btnPostComment.isEnabled = true
                }
            )
        }
    }

    // ── Booking dialog ────────────────────────────────────────────────────────

    private fun parseHoursRange(hours: String?): Pair<Int, Int>? {
        if (hours.isNullOrBlank()) return null
        val regex = Regex("(\\d{1,2})(?::(\\d{2}))?\\s*(am|pm)?", RegexOption.IGNORE_CASE)
        val matches = regex.findAll(hours).toList()
        if (matches.size >= 2) {
            return try {
                val times = matches.map { m ->
                    var h = m.groupValues[1].toInt()
                    val min = m.groupValues[2].takeIf { it.isNotEmpty() }?.toInt() ?: 0
                    val ap = m.groupValues[3].lowercase()
                    if (ap == "pm" && h < 12) h += 12
                    if (ap == "am" && h == 12) h = 0
                    h * 60 + min
                }.sorted()
                Pair(times.first(), times.last())
            } catch (e: Exception) { null }
        }
        return null
    }

    private fun showBookingDialog() {
        val dialogBinding = com.kapwadvo.app.databinding.DialogBookingBinding.inflate(layoutInflater)
        val d = androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setView(dialogBinding.root).create()

        val cal = Calendar.getInstance()
        var selectedDate: String? = null
        var selectedTime: String? = null

        fun updateWarning() {
            if (selectedDate == null || selectedTime == null) {
                dialogBinding.tvWarning.visibility = View.GONE
                return
            }
            try {
                val sdf = java.text.SimpleDateFormat("yyyy-MM-dd hh:mm a", java.util.Locale.getDefault())
                val parsedDate = sdf.parse("$selectedDate $selectedTime")
                if (parsedDate != null) {
                    val diff = parsedDate.time - System.currentTimeMillis()
                    val hoursDiff = diff / (1000 * 60 * 60)
                    if (hoursDiff < 12) {
                        dialogBinding.tvWarning.visibility = View.VISIBLE
                        dialogBinding.tvWarning.text = "Short Notice: Bookings under 12 hours may be declined by the business owner."
                        dialogBinding.tvWarning.setTextColor(android.graphics.Color.parseColor("#D32F2F"))
                    } else if (hoursDiff < 24) {
                        dialogBinding.tvWarning.visibility = View.VISIBLE
                        dialogBinding.tvWarning.text = "Notice: Bookings made under 24 hours in advance are subject to owner availability."
                        dialogBinding.tvWarning.setTextColor(android.graphics.Color.parseColor("#F57C00"))
                    } else {
                        dialogBinding.tvWarning.visibility = View.GONE
                    }
                }
            } catch (e: Exception) {
                dialogBinding.tvWarning.visibility = View.GONE
            }
        }

        dialogBinding.etDate.setOnClickListener {
            val datePickerDialog = DatePickerDialog(requireContext(), { _, y, m, day ->
                cal.set(y, m, day)
                selectedDate = String.format("%04d-%02d-%02d", y, m + 1, day)
                dialogBinding.etDate.setText(selectedDate)
                updateWarning()
            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH))
            datePickerDialog.datePicker.minDate = System.currentTimeMillis()
            datePickerDialog.show()
        }

        val range = parseHoursRange(listing.hours)
        dialogBinding.etTime.setOnClickListener {
            TimePickerDialog(requireContext(), { _, h, min ->
                val totalMin = h * 60 + min
                if (range != null && (totalMin < range.first || totalMin > range.second)) {
                    Toast.makeText(context, "Select a time within operating hours: ${listing.hours}", Toast.LENGTH_LONG).show()
                    selectedTime = null
                    dialogBinding.etTime.setText("")
                    updateWarning()
                } else {
                    val ap = if (h >= 12) "PM" else "AM"
                    val dh = if (h == 0) 12 else if (h > 12) h - 12 else h
                    selectedTime = String.format("%02d:%02d %s", dh, min, ap)
                    dialogBinding.etTime.setText(selectedTime)
                    updateWarning()
                }
            }, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), false).show()
        }

        dialogBinding.btnCancel.setOnClickListener { d.dismiss() }
        dialogBinding.btnConfirm.setOnClickListener {
            val date = selectedDate; val time = selectedTime
            val guestsStr = dialogBinding.etGuests.text.toString().trim()
            val guests = guestsStr.toIntOrNull() ?: 1
            val notes = dialogBinding.etNotes.text.toString().trim()
            if (date == null || time == null) {
                Toast.makeText(context, "Please select both date and time", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (guests < 1) {
                Toast.makeText(context, "Please enter a valid number of guests", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            viewLifecycleOwner.lifecycleScope.launch {
                try {
                    val uid = UserSession.userId ?: return@launch
                    BookingRepository.createBooking(
                        BookingInsert(listing.id, uid, "$date $time", guests, notes = notes.ifEmpty { null })
                    )
                    d.dismiss()
                    dismiss() // dismiss the bottom sheet too

                    val confirmFragment = BookingConfirmedFragment.newInstance(
                        placeName = listing.name,
                        bookingDate = "$date at $time"
                    )
                    parentFragmentManager.beginTransaction()
                        .replace(com.kapwadvo.app.R.id.fragmentContainer, confirmFragment)
                        .addToBackStack(null)
                        .commit()
                } catch (e: Exception) {
                    Toast.makeText(context, "Failed to book. Please try again", Toast.LENGTH_LONG).show()
                }
            }
        }
        d.show()
    }
}
