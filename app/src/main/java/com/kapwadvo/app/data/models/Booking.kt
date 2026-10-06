package com.kapwadvo.app.data.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Booking(
    val id: String = "",
    @SerialName("listing_id") val listingId: String = "",
    @SerialName("user_id") val userId: String = "",
    val date: String = "",
    val guests: Int = 1,
    val status: String = "pending",
    val notes: String? = null,
    @SerialName("decline_reason") val declineReason: String? = null
)

@Serializable
data class BookingInsert(
    @SerialName("listing_id") val listingId: String,
    @SerialName("user_id") val userId: String,
    val date: String,
    val guests: Int = 1,
    val status: String = "pending",
    val notes: String? = null
)
