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
import androidx.lifecycle.lifecycleScope
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
import com.kapwadvo.app.data.repository.BookingRepository
import com.kapwadvo.app.data.repository.ReviewRepository
import com.kapwadvo.app.data.repository.SavedRepository
import com.kapwadvo.app.databinding.BottomSheetListingDetailBinding
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
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

    private var isSaved = false
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
            behavior.state = BottomSheetBehavior.STATE_COLLAPSED
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
        loadData()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onDismiss(dialog: android.content.DialogInterface) {
        super.onDismiss(dialog)
        parentFragmentManager.setFragmentResult("detail_dismissed", Bundle())
    }

    // ── Setup ─────────────────────────────────────────────────────────────────

    private fun setupBasicInfo() {
        binding.tvName.text = listing.name
        binding.tvCategory.text = listing.category
        binding.tvDescription.text = listing.description
        binding.tvPhotoLabel.text = "[ Photo: ${listing.name} ]"

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

        lifecycleScope.launch {
            val ownerId = listing.ownerId
            if (ownerId != null) {
                if (listing.ownerName != null) {
                    binding.tvOwnerName.text = "By ${listing.ownerName}"
                    binding.tvOwnerName.visibility = View.VISIBLE
                } else {
                    val profile = com.kapwadvo.app.data.repository.AuthRepository.getProfile(ownerId)
                    if (profile != null && profile.fullName.isNotBlank()) {
                        listing.ownerName = profile.fullName
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
                lifecycleScope.launch {
                    try {
                        ReviewRepository.deleteReview(review.id)
                        Toast.makeText(context, "Review deleted", Toast.LENGTH_SHORT).show()
                        loadData()
                    } catch (e: Exception) {
                        Toast.makeText(context, "Failed to delete: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        )
        commentAdapter = ReviewCommentAdapter(
            isAdmin = isAdmin,
            onDeleteClick = { comment ->
                lifecycleScope.launch {
                    try {
                        ReviewRepository.deleteComment(comment.id)
                        Toast.makeText(context, "Comment deleted", Toast.LENGTH_SHORT).show()
                        loadData()
                    } catch (e: Exception) {
                        Toast.makeText(context, "Failed to delete: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        )
        binding.rvReviews.layoutManager = LinearLayoutManager(requireContext())
        binding.rvReviews.adapter = reviewAdapter
        binding.rvComments.layoutManager = LinearLayoutManager(requireContext())
        binding.rvComments.adapter = commentAdapter
    }

    private fun showReplyBar(review: com.kapwadvo.app.data.models.ReviewWithAuthor) {
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


    // ── Data Loading ──────────────────────────────────────────────────────────

    private fun loadData() {
        lifecycleScope.launch {
            // Saved status
            if (UserSession.isLoggedIn()) {
                val saved = SavedRepository.getSavedForUser(UserSession.userId!!)
                isSaved = listing.id in saved.map { it.listingId }.toSet()
            }
            setupActionButtons()

            // Reviews
            val reviews = ReviewRepository.getReviewsForListing(listing.id)
            val comments = ReviewRepository.getCommentsForListing(listing.id)

            // Average rating
            if (reviews.isNotEmpty()) {
                val avg = reviews.map { it.rating }.average()
                binding.tvAverageScore.text = String.format("%.1f", avg)
                binding.ratingAvg.rating = avg.toFloat()
                binding.tvReviewCount.text = "(${reviews.size})"
            } else {
                binding.tvAverageScore.text = "–"
                binding.tvReviewCount.text = "(0 reviews)"
            }

            // Reviews list
            binding.tvReviewsLabel.visibility = View.VISIBLE
            if (reviews.isEmpty()) {
                binding.tvNoReviews.visibility = View.VISIBLE
            } else {
                binding.tvNoReviews.visibility = View.GONE
                reviewAdapter.submitList(reviews)
            }
            commentAdapter.submitList(comments)

            // User review form
            binding.layoutYourReview.visibility = View.VISIBLE
            if (UserSession.isLoggedIn()) {
                // Pre-fill if user already has a review
                val existing = reviews.find { it.userId == UserSession.userId }
                if (existing != null) {
                    binding.ratingInput.rating = existing.rating.toFloat()
                    binding.etReviewComment.setText(existing.comment ?: "")
                    binding.btnSubmitReview.text = "Update Review"
                }
            }

            setupReviewSubmit()
            setupCommentPost()
        }
    }

    private fun setupActionButtons() {
        binding.btnSave.visibility = View.VISIBLE
        binding.btnSave.text = if (isSaved && UserSession.isLoggedIn()) getString(R.string.remove_saved) else getString(R.string.save_location)
        binding.btnSave.setOnClickListener {
            if (!UserSession.isLoggedIn()) {
                startActivity(android.content.Intent(requireContext(), com.kapwadvo.app.ui.auth.AuthActivity::class.java))
                return@setOnClickListener
            }
            val uid = UserSession.userId ?: return@setOnClickListener
            lifecycleScope.launch {
                try {
                    if (isSaved) {
                        SavedRepository.removeSaved(uid, listing.id)
                        isSaved = false
                        binding.btnSave.text = getString(R.string.save_location)
                        Toast.makeText(context, "Removed from saved", Toast.LENGTH_SHORT).show()
                    } else {
                        SavedRepository.saveLocation(uid, listing.id)
                        isSaved = true
                        binding.btnSave.text = getString(R.string.remove_saved)
                        Toast.makeText(context, "Saved!", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }

        if (listing.bookingsEnabled) {
            binding.btnBook.visibility = View.VISIBLE
            binding.btnBook.setOnClickListener { 
                if (!UserSession.isLoggedIn()) {
                    startActivity(android.content.Intent(requireContext(), com.kapwadvo.app.ui.auth.AuthActivity::class.java))
                } else {
                    showBookingDialog()
                }
            }
        } else {
            binding.btnBook.visibility = View.GONE
        }
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
            lifecycleScope.launch {
                try {
                    ReviewRepository.submitReview(
                        ReviewInsert(
                            listingId = listing.id,
                            userId = UserSession.userId!!,
                            rating = rating,
                            comment = comment.ifEmpty { null }
                        )
                    )
                    Toast.makeText(context, "Review submitted!", Toast.LENGTH_SHORT).show()
                    // Reload reviews
                    val reviews = ReviewRepository.getReviewsForListing(listing.id)
                    reviewAdapter.submitList(reviews)
                    binding.tvNoReviews.visibility = View.GONE
                    binding.btnSubmitReview.text = "Update Review"
                    if (reviews.isNotEmpty()) {
                        val avg = reviews.map { it.rating }.average()
                        binding.tvAverageScore.text = String.format("%.1f", avg)
                        binding.ratingAvg.rating = avg.toFloat()
                        binding.tvReviewCount.text = "(${reviews.size})"
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
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
            lifecycleScope.launch {
                try {
                    ReviewRepository.addComment(
                        ReviewCommentInsert(
                            listingId = listing.id,
                            userId = UserSession.userId!!,
                            content = text
                        )
                    )
                    binding.etComment.setText("")
                    Toast.makeText(context, "Comment posted!", Toast.LENGTH_SHORT).show()
                    val comments = ReviewRepository.getCommentsForListing(listing.id)
                    commentAdapter.submitList(comments)
                } catch (e: Exception) {
                    Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // ── Booking dialog (same logic as before) ─────────────────────────────────

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

        dialogBinding.etDate.setOnClickListener {
            DatePickerDialog(requireContext(), { _, y, m, day ->
                cal.set(y, m, day)
                selectedDate = String.format("%04d-%02d-%02d", y, m + 1, day)
                dialogBinding.etDate.setText(selectedDate)
            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
        }

        val range = parseHoursRange(listing.hours)
        dialogBinding.etTime.setOnClickListener {
            TimePickerDialog(requireContext(), { _, h, min ->
                val totalMin = h * 60 + min
                if (range != null && (totalMin < range.first || totalMin > range.second)) {
                    Toast.makeText(context, "Select a time within operating hours: ${listing.hours}", Toast.LENGTH_LONG).show()
                } else {
                    val ap = if (h >= 12) "PM" else "AM"
                    val dh = if (h == 0) 12 else if (h > 12) h - 12 else h
                    selectedTime = String.format("%02d:%02d %s", dh, min, ap)
                    dialogBinding.etTime.setText(selectedTime)
                }
            }, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), false).show()
        }

        dialogBinding.btnCancel.setOnClickListener { d.dismiss() }
        dialogBinding.btnConfirm.setOnClickListener {
            val date = selectedDate; val time = selectedTime
            val notes = dialogBinding.etNotes.text.toString().trim()
            if (date == null || time == null) {
                Toast.makeText(context, "Please select both date and time", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            lifecycleScope.launch {
                try {
                    BookingRepository.createBooking(
                        BookingInsert(listing.id, UserSession.userId!!, "$date $time", notes = notes.ifEmpty { null })
                    )
                    Toast.makeText(context, "Booking requested!", Toast.LENGTH_SHORT).show()
                    d.dismiss()
                } catch (e: Exception) {
                    Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
        d.show()
    }
}
