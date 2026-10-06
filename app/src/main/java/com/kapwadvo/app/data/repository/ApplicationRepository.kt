package com.kapwadvo.app.data.repository

import com.kapwadvo.app.data.models.OwnerApplication
import com.kapwadvo.app.data.models.OwnerApplicationInsert
import com.kapwadvo.app.supabase
import io.github.jan.supabase.postgrest.from
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

object ApplicationRepository {

    suspend fun submitApplication(application: OwnerApplicationInsert) {
        supabase.from("owner_applications").insert(application)
    }

    suspend fun getPendingApplications(): List<OwnerApplication> = try {
        supabase.from("owner_applications")
            .select { filter { eq("status", "pending") } }
            .decodeList<OwnerApplication>()
    } catch (e: Exception) { 
        if (e is kotlinx.coroutines.CancellationException) throw e
        emptyList() 
    }

    suspend fun getUserApplication(userId: String): OwnerApplication? {
        return supabase.from("owner_applications")
            .select { filter { eq("user_id", userId) } }
            .decodeSingleOrNull<OwnerApplication>()
    }

    suspend fun updateApplicationStatus(id: String, status: String) {
        supabase.from("owner_applications").update(
            buildJsonObject { put("status", status) }
        ) { filter { eq("id", id) } }
    }
}
