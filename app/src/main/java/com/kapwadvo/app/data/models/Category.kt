package com.kapwadvo.app.data.models

import kotlinx.serialization.Serializable

@Serializable
data class Category(
    val id: String = "",
    val name: String,
    val created_at: String = ""
)
