package com.kapwadvo.app.data.repository

import com.kapwadvo.app.data.models.Profile
import com.kapwadvo.app.supabase
import io.github.jan.supabase.postgrest.from
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

object AdminRepository {

    suspend fun getAllProfiles(): List<Profile> = try {
        supabase.from("profiles").select().decodeList<Profile>()
    } catch (e: Exception) { emptyList() }

    suspend fun setUserRole(userId: String, role: String) {
        supabase.from("profiles").update(
            buildJsonObject { put("role", role) }
        ) { filter { eq("id", userId) } }
    }
}
