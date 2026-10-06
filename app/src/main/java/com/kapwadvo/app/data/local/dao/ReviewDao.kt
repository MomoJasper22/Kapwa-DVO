package com.kapwadvo.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.kapwadvo.app.data.local.entity.CachedReview
import com.kapwadvo.app.data.local.entity.CachedReviewComment
import kotlinx.coroutines.flow.Flow

@Dao
interface ReviewDao {
    @Query("SELECT * FROM cached_reviews WHERE listingId = :listingId ORDER BY createdAt DESC")
    suspend fun getReviewsForListing(listingId: String): List<CachedReview>

    @Query("SELECT * FROM cached_reviews WHERE listingId IN (:listingIds)")
    suspend fun getReviewsForListings(listingIds: List<String>): List<CachedReview>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReviews(reviews: List<CachedReview>)

    @Query("DELETE FROM cached_reviews WHERE listingId = :listingId")
    suspend fun deleteReviewsForListing(listingId: String)

    @Query("SELECT * FROM cached_review_comments WHERE listingId = :listingId ORDER BY createdAt DESC")
    suspend fun getCommentsForListing(listingId: String): List<CachedReviewComment>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertComments(comments: List<CachedReviewComment>)

    @Query("DELETE FROM cached_review_comments WHERE listingId = :listingId")
    suspend fun deleteCommentsForListing(listingId: String)
}
