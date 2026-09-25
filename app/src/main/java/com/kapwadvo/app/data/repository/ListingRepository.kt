package com.kapwadvo.app.data.repository

import com.kapwadvo.app.data.models.Listing
import com.kapwadvo.app.data.models.ListingInsert
import com.kapwadvo.app.supabase
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.storage.storage
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

object ListingRepository {

    suspend fun getApprovedListings(): List<Listing> = try {
        supabase.from("listings")
            .select { filter { eq("status", "approved") } }
            .decodeList<Listing>()
    } catch (e: Exception) { emptyList() }

    suspend fun getListingsByOwner(ownerId: String): List<Listing> = try {
        supabase.from("listings")
            .select { filter { eq("owner_id", ownerId) } }
            .decodeList<Listing>()
    } catch (e: Exception) { emptyList() }

    suspend fun getAllListings(): List<Listing> = try {
        supabase.from("listings").select().decodeList<Listing>()
    } catch (e: Exception) { emptyList() }

    suspend fun getPendingListings(): List<Listing> = try {
        supabase.from("listings")
            .select { filter { eq("status", "pending") } }
            .decodeList<Listing>()
    } catch (e: Exception) { emptyList() }

    suspend fun getPendingUpdatesListings(): List<Listing> = try {
        supabase.from("listings")
            .select { filter { neq("pending_updates", "null") } }
            .decodeList<Listing>()
    } catch (e: Exception) { emptyList() }

    suspend fun getListingById(id: String): Listing? = try {
        supabase.from("listings")
            .select { filter { eq("id", id) } }
            .decodeSingleOrNull<Listing>()
    } catch (e: Exception) { null }

    suspend fun getListingsByIds(ids: List<String>): List<Listing> =
        ids.mapNotNull { getListingById(it) }

    suspend fun createListing(insert: ListingInsert) {
        supabase.from("listings").insert(insert)
    }

    suspend fun updateListing(id: String, insert: ListingInsert) {
        supabase.from("listings").update(
            buildJsonObject {
                put("name", insert.name)
                put("description", insert.description)
                put("category", insert.category)
                insert.address?.let { put("address", it) }
                insert.hours?.let { put("hours", it) }
                insert.contact?.let { put("contact", it) }
                insert.lat?.let { put("lat", it) }
                insert.lng?.let { put("lng", it) }
                put("status", insert.status)
                // Always write pending_updates — null clears a previous request
                if (insert.pendingUpdates != null) {
                    put("pending_updates", insert.pendingUpdates)
                } else {
                    put("pending_updates", kotlinx.serialization.json.JsonNull)
                }
                
                val photoArray = kotlinx.serialization.json.buildJsonArray {
                    insert.photoUrls.forEach { add(kotlinx.serialization.json.JsonPrimitive(it)) }
                }
                put("photo_urls", photoArray)
            }
        ) { filter { eq("id", id) } }
    }

    suspend fun uploadPhoto(bytes: ByteArray, fileName: String): String {
        val bucket = supabase.storage.from("listing_photos")
        bucket.upload(fileName, bytes) { upsert = true }
        return bucket.publicUrl(fileName)
    }

    suspend fun updateStatus(id: String, status: String) {
        supabase.from("listings").update(
            buildJsonObject { put("status", status) }
        ) { filter { eq("id", id) } }
    }

    suspend fun updatePin(id: String, lat: Double, lng: Double) {
        supabase.from("listings").update(
            buildJsonObject { put("lat", lat); put("lng", lng) }
        ) { filter { eq("id", id) } }
    }

    suspend fun deleteListing(id: String) {
        supabase.from("listings").delete { filter { eq("id", id) } }
    }

    suspend fun toggleBookingEnabled(id: String, enabled: Boolean) {
        supabase.from("listings").update(
            buildJsonObject { put("bookings_enabled", enabled) }
        ) { filter { eq("id", id) } }
    }
}
