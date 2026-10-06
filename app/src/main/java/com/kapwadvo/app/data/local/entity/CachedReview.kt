package com.kapwadvo.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.kapwadvo.app.data.models.ReviewWithAuthor
import com.kapwadvo.app.data.models.Review

@Entity(tableName = "cached_reviews")
data class CachedReview(
    @PrimaryKey val id: String,
    val listingId: String,
    val userId: String,
    val rating: Int,
    val comment: String?,
    val createdAt: String?,
    // Denormalized author data
    val authorName: String,
    val authorInitials: String,
    val authorAvatarUrl: String?
) {
    fun toReviewWithAuthor() = ReviewWithAuthor(
        id = id,
        userId = userId,
        rating = rating,
        comment = comment,
        createdAt = createdAt,
        authorName = authorName,
        authorInitials = authorInitials,
        authorAvatarUrl = authorAvatarUrl
    )

    fun toReview() = Review(
        id = id,
        listingId = listingId,
        userId = userId,
        rating = rating,
        comment = comment,
        createdAt = createdAt
    )

    companion object {
        fun from(review: ReviewWithAuthor, listingId: String) = CachedReview(
            id = review.id,
            listingId = listingId,
            userId = review.userId,
            rating = review.rating,
            comment = review.comment,
            createdAt = review.createdAt,
            authorName = review.authorName,
            authorInitials = review.authorInitials,
            authorAvatarUrl = review.authorAvatarUrl
        )
    }
}
