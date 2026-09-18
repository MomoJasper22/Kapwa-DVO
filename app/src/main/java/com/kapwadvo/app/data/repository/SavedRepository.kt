package com.kapwadvo.app.data.repository

import com.kapwadvo.app.data.models.SavedLocation
import com.kapwadvo.app.data.models.SavedLocationInsert
import com.kapwadvo.app.supabase
import io.github.jan.supabase.postgrest.from

object SavedRepository {

    suspend fun getSavedForUser(userId: String): List<SavedLocation> = try {
        supabase.from("saved_locations")
            .select { filter { eq("user_id", userId) } }
            .decodeList<SavedLocation>()
    } catch (e: Exception) { emptyList() }

    suspend fun saveLocation(userId: String, listingId: String) {
        supabase.from("saved_locations").insert(
            SavedLocationInsert(userId = userId, listingId = listingId)
        )
    }

    suspend fun removeSaved(userId: String, listingId: String) {
        supabase.from("saved_locations").delete {
            filter {
                eq("user_id", userId)
                eq("listing_id", listingId)
            }
        }
    }

    suspend fun isSaved(userId: String, listingId: String): Boolean = try {
        supabase.from("saved_locations")
            .select { filter { eq("user_id", userId); eq("listing_id", listingId) } }
            .decodeList<SavedLocation>()
            .isNotEmpty()
    } catch (e: Exception) { false }
}
