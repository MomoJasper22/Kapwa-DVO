package com.kapwadvo.app.data.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class OwnerApplication(
    val id: String = "",
    @SerialName("user_id") val userId: String = "",
    val status: String = "pending",
    @SerialName("submitted_at") val submittedAt: String? = null
)

@Serializable
data class OwnerApplicationInsert(
    @SerialName("user_id") val userId: String,
    val status: String = "pending"
)
