package com.kapwadvo.app.data.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class OwnerApplication(
    val id: String = "",
    @SerialName("user_id") val userId: String = "",
    val status: String = "pending",
    @SerialName("business_name") val businessName: String? = null,
    val category: String? = null,
    @SerialName("contact_info") val contactInfo: String? = null,
    @SerialName("business_address") val businessAddress: String? = null,
    @SerialName("permit_number") val permitNumber: String? = null,
    val reason: String? = null,
    @SerialName("submitted_at") val submittedAt: String? = null
)

@Serializable
data class OwnerApplicationInsert(
    @SerialName("user_id") val userId: String,
    val status: String = "pending",
    @SerialName("business_name") val businessName: String? = null,
    val category: String? = null,
    @SerialName("contact_info") val contactInfo: String? = null,
    @SerialName("business_address") val businessAddress: String? = null,
    @SerialName("permit_number") val permitNumber: String? = null,
    val reason: String? = null
)
