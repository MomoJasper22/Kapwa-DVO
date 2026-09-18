package com.kapwadvo.app.data.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SavedLocation(
    val id: String = "",
    @SerialName("user_id") val userId: String = "",
    @SerialName("listing_id") val listingId: String = ""
)

@Serializable
data class SavedLocationInsert(
    @SerialName("user_id") val userId: String,
    @SerialName("listing_id") val listingId: String
)
