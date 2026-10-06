package com.kapwadvo.app.data.repository

import com.kapwadvo.app.KapwaDVOApp
import com.kapwadvo.app.data.models.SavedLocation
import com.kapwadvo.app.data.models.SavedLocationInsert
import com.kapwadvo.app.supabase
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.Flow

object SavedRepository {

    fun observeSavedLocations(userId: String): Flow<List<SavedLocation>> {
        return KapwaDVOApp.database.savedLocationDao().observeSavedLocations(userId)
            .map { list -> list.map { it.toSavedLocation() } }
    }

    suspend fun getSavedForUser(userId: String): List<SavedLocation> {
        val cached = com.kapwadvo.app.KapwaDVOApp.database.savedLocationDao().getSavedLocations(userId)
        return cached.map { it.toSavedLocation() }
    }

    suspend fun syncSavedForUser(userId: String) {
        try {
            val saved = supabase.from("saved_locations")
                .select { filter { eq("user_id", userId) } }
                .decodeList<SavedLocation>()

            val cached = saved.map { com.kapwadvo.app.data.local.entity.CachedSavedLocation.from(it) }
            com.kapwadvo.app.KapwaDVOApp.database.savedLocationDao().clearForUser(userId)
            com.kapwadvo.app.KapwaDVOApp.database.savedLocationDao().insertAll(cached)
        } catch (e: Exception) {
        }
    }

    suspend fun saveLocation(userId: String, listingId: String) {
        val insert = SavedLocationInsert(userId = userId, listingId = listingId)
        supabase.from("saved_locations").insert(insert)

        // Optimistic update of local cache
        val fakeId = java.util.UUID.randomUUID()
            .toString() // Wait, actual ID is created by DB. It's ok to use random for cache because sync will overwrite
        val saved = SavedLocation(id = fakeId, userId = userId, listingId = listingId)
        com.kapwadvo.app.KapwaDVOApp.database.savedLocationDao()
            .insert(com.kapwadvo.app.data.local.entity.CachedSavedLocation.from(saved))
        syncSavedForUser(userId)
    }

    suspend fun removeSaved(userId: String, listingId: String) {
        supabase.from("saved_locations").delete {
            filter {
                eq("user_id", userId)
                eq("listing_id", listingId)
            }
        }
        com.kapwadvo.app.KapwaDVOApp.database.savedLocationDao().delete(userId, listingId)
    }

    suspend fun isSaved(userId: String, listingId: String): Boolean {
        val cached = com.kapwadvo.app.KapwaDVOApp.database.savedLocationDao().getSavedLocations(userId)
        return cached.any { it.listingId == listingId }
    }
}
