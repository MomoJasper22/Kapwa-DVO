package com.kapwadvo.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.kapwadvo.app.data.models.CommentWithAuthor

@Entity(tableName = "cached_review_comments")
data class CachedReviewComment(
    @PrimaryKey val id: String,
    val listingId: String,
    val userId: String,
    val content: String,
    val createdAt: String?,
    // Denormalized author data
    val authorName: String,
    val authorInitials: String,
    val authorAvatarUrl: String?
) {
    fun toCommentWithAuthor() = CommentWithAuthor(
        id = id,
        userId = userId,
        content = content,
        createdAt = createdAt,
        authorName = authorName,
        authorInitials = authorInitials,
        authorAvatarUrl = authorAvatarUrl
    )

    companion object {
        fun from(comment: CommentWithAuthor, listingId: String) = CachedReviewComment(
            id = comment.id,
            listingId = listingId,
            userId = comment.userId,
            content = comment.content,
            createdAt = comment.createdAt,
            authorName = comment.authorName,
            authorInitials = comment.authorInitials,
            authorAvatarUrl = comment.authorAvatarUrl
        )
    }
}
