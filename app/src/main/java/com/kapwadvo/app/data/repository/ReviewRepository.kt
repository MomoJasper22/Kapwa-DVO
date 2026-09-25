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
        val reviews = try {
            supabase.from("reviews")
                .select { filter { eq("listing_id", listingId) } }
                .decodeList<Review>()
        } catch (e: Exception) { emptyList() }

        val profiles = reviews.map { it.userId }.distinct()
            .mapNotNull { AuthRepository.getProfile(it) }
            .associateBy { it.id }

        return reviews.map { r ->
            val p = profiles[r.userId]
            val name = p?.fullName?.ifEmpty { "User" } ?: "User"
            val initials = listOfNotNull(
                p?.firstName?.firstOrNull()?.uppercaseChar()?.toString(),
                p?.lastName?.firstOrNull()?.uppercaseChar()?.toString()
            ).joinToString("").ifEmpty { "U" }
            ReviewWithAuthor(r.id, r.userId, r.rating, r.comment, r.createdAt, name, initials)
        }
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
    }

    // ── Comments (unlimited per user) ────────────────────────────────────────

    suspend fun getCommentsForListing(listingId: String): List<CommentWithAuthor> {
        val comments = try {
            supabase.from("review_comments")
                .select { filter { eq("listing_id", listingId) } }
                .decodeList<ReviewComment>()
        } catch (e: Exception) { emptyList() }

        val profiles = comments.map { it.userId }.distinct()
            .mapNotNull { AuthRepository.getProfile(it) }
            .associateBy { it.id }

        return comments.map { c ->
            val p = profiles[c.userId]
            val name = p?.fullName?.ifEmpty { "User" } ?: "User"
            val initials = listOfNotNull(
                p?.firstName?.firstOrNull()?.uppercaseChar()?.toString(),
                p?.lastName?.firstOrNull()?.uppercaseChar()?.toString()
            ).joinToString("").ifEmpty { "U" }
            CommentWithAuthor(c.id, c.userId, c.content, c.createdAt, name, initials)
        }
    }

    suspend fun addComment(insert: ReviewCommentInsert) {
        supabase.from("review_comments").insert(insert)
    }

    suspend fun deleteReview(id: String) {
        supabase.from("reviews").delete { filter { eq("id", id) } }
    }

    suspend fun deleteComment(id: String) {
        supabase.from("review_comments").delete { filter { eq("id", id) } }
    }
}
