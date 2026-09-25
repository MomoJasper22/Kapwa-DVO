package com.kapwadvo.app.data.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
data class Listing(
    val id: String = "",
    @SerialName("owner_id") val ownerId: String? = null,
    val name: String = "",
    val description: String = "",
    val category: String = "",
    val lat: Double? = null,
    val lng: Double? = null,
    val status: String = "pending",
    val address: String? = null,
    val hours: String? = null,
    val contact: String? = null,
    @SerialName("pending_updates") val pendingUpdates: JsonObject? = null,
    @SerialName("bookings_enabled") val bookingsEnabled: Boolean = false,
    @SerialName("photo_urls") val photoUrls: List<String> = emptyList(),
    @SerialName("created_at") val createdAt: String? = null
)

/** Used for INSERT — no id or created_at (Supabase auto-generates them). */
@Serializable
data class ListingInsert(
    @SerialName("owner_id") val ownerId: String,
    val name: String,
    val description: String,
    val category: String,
    val lat: Double? = null,
    val lng: Double? = null,
    val status: String = "pending",
    val address: String? = null,
    val hours: String? = null,
    val contact: String? = null,
    @SerialName("pending_updates") val pendingUpdates: JsonObject? = null,
    @SerialName("bookings_enabled") val bookingsEnabled: Boolean = false,
    @SerialName("photo_urls") val photoUrls: List<String> = emptyList()
)
