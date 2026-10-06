package com.kapwadvo.app.data.repository

import com.kapwadvo.app.data.models.*
import com.kapwadvo.app.supabase
import io.github.jan.supabase.postgrest.from
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.put

object ReviewRepository {

    // ── Reviews ─────────────────────────────────────────────────────────────

    suspend fun getReviewsForListing(listingId: String): List<ReviewWithAuthor> {
        val cached = com.kapwadvo.app.KapwaDVOApp.database.reviewDao().getReviewsForListing(listingId)
        return cached.map { it.toReviewWithAuthor() }
    }

    suspend fun syncReviewsForListing(listingId: String) {
        try {
            val reviews = supabase.from("reviews")
                .select { filter { eq("listing_id", listingId) } }
                .decodeList<Review>()
            
            val userIds = reviews.map { it.userId }.distinct()
            val profiles = AuthRepository.getProfiles(userIds).associateBy { it.id }

            val cachedReviews = reviews.map { r ->
                val p = profiles[r.userId]
                val name = p?.fullName?.ifEmpty { "User" } ?: "User"
                val initials = listOfNotNull(
                    p?.firstName?.firstOrNull()?.uppercaseChar()?.toString(),
                    p?.lastName?.firstOrNull()?.uppercaseChar()?.toString()
                ).joinToString("").ifEmpty { "U" }
                com.kapwadvo.app.data.local.entity.CachedReview(
                    id = r.id,
                    listingId = r.listingId,
                    userId = r.userId,
                    rating = r.rating,
                    comment = r.comment,
                    createdAt = r.createdAt,
                    authorName = name,
                    authorInitials = initials,
                    authorAvatarUrl = p?.avatarUrl
                )
            }
            com.kapwadvo.app.KapwaDVOApp.database.reviewDao().deleteReviewsForListing(listingId)
            com.kapwadvo.app.KapwaDVOApp.database.reviewDao().insertReviews(cachedReviews)
        } catch (e: Exception) {
            // Silently fail sync
        }
    }
    suspend fun getRawReviewsForListings(listingIds: List<String>): List<Review> {
        if (listingIds.isEmpty()) return emptyList()
        val cached = com.kapwadvo.app.KapwaDVOApp.database.reviewDao().getReviewsForListings(listingIds)
        return cached.map { it.toReview() }
    }

    suspend fun getUserReview(listingId: String, userId: String): Review? = try {
        supabase.from("reviews")
            .select { filter { eq("listing_id", listingId); eq("user_id", userId) } }
            .decodeSingleOrNull<Review>()
    } catch (e: Exception) { null }

    /** Insert or update the user's single review for this listing. */
    suspend fun submitReview(insert: ReviewInsert) {
        val existing = getUserReview(insert.listingId, insert.userId)
        if (existing == null) {
            supabase.from("reviews").insert(insert)
        } else {
            supabase.from("reviews").update(
                buildJsonObject {
                    put("rating", insert.rating)
                    if (insert.comment != null) put("comment", insert.comment)
                    else put("comment", JsonNull)
                }
            ) { filter { eq("listing_id", insert.listingId); eq("user_id", insert.userId) } }
        }
        syncReviewsForListing(insert.listingId)
    }

    // ── Comments (unlimited per user) ────────────────────────────────────────

    suspend fun getCommentsForListing(listingId: String): List<CommentWithAuthor> {
        val cached = com.kapwadvo.app.KapwaDVOApp.database.reviewDao().getCommentsForListing(listingId)
        return cached.map { it.toCommentWithAuthor() }
    }

    suspend fun syncCommentsForListing(listingId: String) {
        try {
            val comments = supabase.from("review_comments")
                .select { filter { eq("listing_id", listingId) } }
                .decodeList<ReviewComment>()
            
            val userIds = comments.map { it.userId }.distinct()
            val profiles = AuthRepository.getProfiles(userIds).associateBy { it.id }

            val cachedComments = comments.map { c ->
                val p = profiles[c.userId]
                val name = p?.fullName?.ifEmpty { "User" } ?: "User"
                val initials = listOfNotNull(
                    p?.firstName?.firstOrNull()?.uppercaseChar()?.toString(),
                    p?.lastName?.firstOrNull()?.uppercaseChar()?.toString()
                ).joinToString("").ifEmpty { "U" }
                com.kapwadvo.app.data.local.entity.CachedReviewComment(
                    id = c.id,
                    listingId = c.listingId,
                    userId = c.userId,
                    content = c.content,
                    createdAt = c.createdAt,
                    authorName = name,
                    authorInitials = initials,
                    authorAvatarUrl = p?.avatarUrl
                )
            }
            com.kapwadvo.app.KapwaDVOApp.database.reviewDao().deleteCommentsForListing(listingId)
            com.kapwadvo.app.KapwaDVOApp.database.reviewDao().insertComments(cachedComments)
        } catch (e: Exception) {
            // Silently fail sync
        }
    }

    suspend fun addComment(insert: ReviewCommentInsert) {
        supabase.from("review_comments").insert(insert)
        syncCommentsForListing(insert.listingId)
    }

    suspend fun deleteReview(id: String) {
        val review = supabase.from("reviews").select { filter { eq("id", id) } }.decodeSingleOrNull<Review>()
        supabase.from("reviews").delete { filter { eq("id", id) } }
        if (review != null) {
            syncReviewsForListing(review.listingId)
        }
    }

    suspend fun deleteComment(id: String) {
        val comment = supabase.from("review_comments").select { filter { eq("id", id) } }.decodeSingleOrNull<ReviewComment>()
        supabase.from("review_comments").delete { filter { eq("id", id) } }
        if (comment != null) {
            syncCommentsForListing(comment.listingId)
        }
    }
}
