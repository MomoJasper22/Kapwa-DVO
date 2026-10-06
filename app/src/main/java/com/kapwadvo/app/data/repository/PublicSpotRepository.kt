package com.kapwadvo.app.data.repository

import com.kapwadvo.app.data.models.PublicSpotRequest
import com.kapwadvo.app.data.models.PublicSpotRequestInsert
import com.kapwadvo.app.supabase
import io.github.jan.supabase.postgrest.from
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

object PublicSpotRepository {

    suspend fun createRequest(insert: PublicSpotRequestInsert) {
        supabase.from("public_spot_requests").insert(insert)
    }

    suspend fun getPendingRequests(): List<PublicSpotRequest> = try {
        supabase.from("public_spot_requests")
            .select { filter { eq("status", "pending") } }
            .decodeList<PublicSpotRequest>()
    } catch (e: Exception) { emptyList() }

    suspend fun updateRequestStatus(id: String, status: String) {
        supabase.from("public_spot_requests").update(
            buildJsonObject { put("status", status) }
        ) { filter { eq("id", id) } }
    }
}
