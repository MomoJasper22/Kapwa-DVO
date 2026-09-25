package com.kapwadvo.app.data.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Profile(
    val id: String = "",
    val role: String = "user",
    @SerialName("first_name") val firstName: String? = null,
    @SerialName("last_name") val lastName: String? = null,
    val email: String? = null,
    val status: String? = null,
    @SerialName("dob") val dob: String? = null,
    @SerialName("phone_number") val phoneNumber: String? = null,
    val address: String? = null
) {
    val fullName: String
        get() = listOfNotNull(firstName, lastName).joinToString(" ").ifEmpty { "" }
}
