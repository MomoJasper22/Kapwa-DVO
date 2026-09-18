package com.kapwadvo.app.data.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Profile(
    val id: String = "",
    val role: String = "user",
    val name: String? = null,
    val email: String? = null,
    val status: String? = null
)
