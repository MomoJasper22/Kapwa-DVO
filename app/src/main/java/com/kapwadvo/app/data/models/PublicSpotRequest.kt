package com.kapwadvo.app.data.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PublicSpotRequest(
    val id: String = "",
    @SerialName("user_id") val userId: String = "",
    val name: String = "",
    val address: String = "",
    val latitude: Double? = null,
    val longitude: Double? = null,
    val comments: String? = null,
    @SerialName("photo_url") val photoUrl: String? = null,
    val status: String = "pending",
    @SerialName("created_at") val createdAt: String = ""
)

@Serializable
data class PublicSpotRequestInsert(
    @SerialName("user_id") val userId: String,
    val name: String,
    val address: String,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val comments: String? = null,
    @SerialName("photo_url") val photoUrl: String? = null,
    val status: String = "pending"
)
