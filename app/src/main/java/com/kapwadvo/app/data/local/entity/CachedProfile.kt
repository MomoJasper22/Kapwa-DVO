package com.kapwadvo.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.kapwadvo.app.data.models.Profile

@Entity(tableName = "cached_profile")
data class CachedProfile(
    @PrimaryKey
    val id: String,
    val role: String,
    val firstName: String?,
    val lastName: String?,
    val email: String?,
    val status: String?,
    val dob: String?,
    val phoneNumber: String?,
    val address: String?,
    val avatarUrl: String?
) {
    fun toProfile(): Profile = Profile(
        id = id,
        role = role,
        firstName = firstName,
        lastName = lastName,
        email = email,
        status = status,
        dob = dob,
        phoneNumber = phoneNumber,
        address = address,
        avatarUrl = avatarUrl
    )

    companion object {
        fun fromProfile(profile: Profile): CachedProfile {
            return CachedProfile(
                id = profile.id,
                role = profile.role,
                firstName = profile.firstName,
                lastName = profile.lastName,
                email = profile.email,
                status = profile.status,
                dob = profile.dob,
                phoneNumber = profile.phoneNumber,
                address = profile.address,
                avatarUrl = profile.avatarUrl
            )
        }
    }
}
