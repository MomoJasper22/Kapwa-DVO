package com.kapwadvo.app.data.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Review(
    val id: String = "",
    @SerialName("listing_id") val listingId: String = "",
    @SerialName("user_id") val userId: String = "",
    val rating: Int = 0,
    val comment: String? = null,
    @SerialName("created_at") val createdAt: String? = null
)

@Serializable
data class ReviewInsert(
    @SerialName("listing_id") val listingId: String,
    @SerialName("user_id") val userId: String,
    val rating: Int,
    val comment: String? = null
)

@Serializable
data class ReviewComment(
    val id: String = "",
    @SerialName("listing_id") val listingId: String = "",
    @SerialName("user_id") val userId: String = "",
    val content: String = "",
    @SerialName("created_at") val createdAt: String? = null
)

@Serializable
data class ReviewCommentInsert(
    @SerialName("listing_id") val listingId: String,
    @SerialName("user_id") val userId: String,
    val content: String
)

/** UI model — review merged with author's profile data */
data class ReviewWithAuthor(
    val id: String,
    val userId: String,
    val rating: Int,
    val comment: String?,
    val createdAt: String?,
    val authorName: String,
    val authorInitials: String
)

/** UI model — comment merged with author's profile data */
data class CommentWithAuthor(
    val id: String,
    val userId: String,
    val content: String,
    val createdAt: String?,
    val authorName: String,
    val authorInitials: String
)
