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
        } catch (e: Exception) { if (e is kotlinx.coroutines.CancellationException) throw e;
        }
    }

    suspend fun saveLocation(userId: String, listingId: String) {
        val fakeId = java.util.UUID.randomUUID().toString()
        val saved = SavedLocation(id = fakeId, userId = userId, listingId = listingId)
        com.kapwadvo.app.KapwaDVOApp.database.savedLocationDao()
            .insert(com.kapwadvo.app.data.local.entity.CachedSavedLocation.from(saved))

        val isOnline = com.kapwadvo.app.KapwaDVOApp.networkMonitor.isOnline.value
        try {
            if (isOnline) {
                val insert = SavedLocationInsert(userId = userId, listingId = listingId)
                supabase.from("saved_locations").insert(insert)
                syncSavedForUser(userId)
            } else {
                throw Exception("Offline")
            }
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            val payload = org.json.JSONObject().apply {
                put("listingId", listingId)
                put("saved", true)
            }.toString()
            com.kapwadvo.app.data.repository.SyncQueueRepository.queueAction(
                userId,
                "TOGGLE_SAVE",
                payload
            )
        }
    }

    suspend fun removeSaved(userId: String, listingId: String) {
        com.kapwadvo.app.KapwaDVOApp.database.savedLocationDao().delete(userId, listingId)

        val isOnline = com.kapwadvo.app.KapwaDVOApp.networkMonitor.isOnline.value
        try {
            if (isOnline) {
                supabase.from("saved_locations").delete {
                    filter {
                        eq("user_id", userId)
                        eq("listing_id", listingId)
                    }
                }
            } else {
                throw Exception("Offline")
            }
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            val payload = org.json.JSONObject().apply {
                put("listingId", listingId)
                put("saved", false)
            }.toString()
            com.kapwadvo.app.data.repository.SyncQueueRepository.queueAction(
                userId,
                "TOGGLE_SAVE",
                payload
            )
        }
    }

    suspend fun isSaved(userId: String, listingId: String): Boolean {
        val cached = com.kapwadvo.app.KapwaDVOApp.database.savedLocationDao().getSavedLocations(userId)
        return cached.any { it.listingId == listingId }
    }
}
