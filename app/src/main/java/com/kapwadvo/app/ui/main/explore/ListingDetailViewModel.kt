package com.kapwadvo.app.ui.main.explore

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kapwadvo.app.UserSession
import com.kapwadvo.app.data.models.ReviewComment
import com.kapwadvo.app.data.models.ReviewCommentInsert
import com.kapwadvo.app.data.models.ReviewInsert
import com.kapwadvo.app.data.models.ReviewWithAuthor
import com.kapwadvo.app.data.models.CommentWithAuthor
import com.kapwadvo.app.data.repository.ReviewRepository
import com.kapwadvo.app.data.repository.SavedRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ListingDetailUiState(
    val isLoading: Boolean = true,
    val isSaved: Boolean = false,
    val hasBooked: Boolean = false,
    val reviews: List<ReviewWithAuthor> = emptyList(),
    val comments: List<CommentWithAuthor> = emptyList(),
    val avgRating: Double = 0.0,
    val reviewCount: Int = 0,
    val error: String? = null
)

class ListingDetailViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(ListingDetailUiState())
    val uiState: StateFlow<ListingDetailUiState> = _uiState.asStateFlow()

    fun loadData(listingId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val uid = UserSession.userId

                val savedDeferred = async {
                    if (UserSession.isLoggedIn() && uid != null) {
                        SavedRepository.getSavedForUser(uid)
                    } else {
                        emptyList()
                    }
                }
                val bookingsDeferred = async {
                    if (UserSession.isLoggedIn() && uid != null) {
                        com.kapwadvo.app.data.repository.BookingRepository.getBookingsForUser(uid)
                    } else {
                        emptyList()
                    }
                }
                val reviewsDeferred = async { ReviewRepository.getReviewsForListing(listingId) }
                val commentsDeferred = async { ReviewRepository.getCommentsForListing(listingId) }

                val saved = savedDeferred.await()
                val bookings = bookingsDeferred.await()
                val reviews = reviewsDeferred.await()
                val comments = commentsDeferred.await()

                val isSaved = listingId in saved.map { it.listingId }.toSet()
                val hasBooked = bookings.any { it.listingId == listingId && it.status.lowercase() !in listOf("cancelled", "rejected", "declined") }
                val avgRating = if (reviews.isEmpty()) 0.0 else reviews.map { it.rating }.average()

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isSaved = isSaved,
                    hasBooked = hasBooked,
                    reviews = reviews,
                    comments = comments,
                    avgRating = avgRating,
                    reviewCount = reviews.size
                )

                // Background sync
                launch {
                    try {
                        ReviewRepository.syncReviewsForListing(listingId)
                        ReviewRepository.syncCommentsForListing(listingId)
                        
                        val uid = UserSession.userId
                        if (UserSession.isLoggedIn() && uid != null) {
                            SavedRepository.syncSavedForUser(uid)
                            com.kapwadvo.app.data.repository.BookingRepository.syncUserBookings(uid)
                        }
                        
                        val updatedReviews = ReviewRepository.getReviewsForListing(listingId)
                        val updatedComments = ReviewRepository.getCommentsForListing(listingId)
                        val newAvg = if (updatedReviews.isEmpty()) 0.0 else updatedReviews.map { it.rating }.average()
                        
                        val updatedSaved = if (UserSession.isLoggedIn() && uid != null) SavedRepository.getSavedForUser(uid) else emptyList()
                        val updatedBookings = if (UserSession.isLoggedIn() && uid != null) com.kapwadvo.app.data.repository.BookingRepository.getBookingsForUser(uid) else emptyList()
                        val newIsSaved = listingId in updatedSaved.map { it.listingId }.toSet()
                        val newHasBooked = updatedBookings.any { it.listingId == listingId && it.status.lowercase() !in listOf("cancelled", "rejected", "declined") }
                        
                        _uiState.value = _uiState.value.copy(
                            reviews = updatedReviews,
                            comments = updatedComments,
                            avgRating = newAvg,
                            reviewCount = updatedReviews.size,
                            isSaved = newIsSaved,
                            hasBooked = newHasBooked
                        )
                    } catch (e: Exception) {}
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to load details"
                )
            }
        }
    }

    fun toggleSave(listingId: String, onSuccess: (Boolean) -> Unit, onError: () -> Unit) {
        viewModelScope.launch {
            val uid = UserSession.userId ?: return@launch
            val currentState = _uiState.value
            try {
                if (currentState.isSaved) {
                    SavedRepository.removeSaved(uid, listingId)
                    _uiState.value = currentState.copy(isSaved = false)
                    onSuccess(false)
                } else {
                    SavedRepository.saveLocation(uid, listingId)
                    _uiState.value = currentState.copy(isSaved = true)
                    onSuccess(true)
                }
            } catch (e: Exception) {
                onError()
            }
        }
    }

    fun submitReview(insert: ReviewInsert, onSuccess: () -> Unit, onError: () -> Unit) {
        viewModelScope.launch {
            try {
                ReviewRepository.submitReview(insert)
                // Reload reviews after submission
                val reviews = ReviewRepository.getReviewsForListing(insert.listingId)
                val avgRating = if (reviews.isEmpty()) 0.0 else reviews.map { it.rating }.average()
                _uiState.value = _uiState.value.copy(
                    reviews = reviews,
                    avgRating = avgRating,
                    reviewCount = reviews.size
                )
                onSuccess()
            } catch (e: Exception) {
                onError()
            }
        }
    }

    fun postComment(insert: ReviewCommentInsert, onSuccess: () -> Unit, onError: () -> Unit) {
        viewModelScope.launch {
            try {
                ReviewRepository.addComment(insert)
                val comments = ReviewRepository.getCommentsForListing(insert.listingId)
                _uiState.value = _uiState.value.copy(comments = comments)
                onSuccess()
            } catch (e: Exception) {
                onError()
            }
        }
    }
}
