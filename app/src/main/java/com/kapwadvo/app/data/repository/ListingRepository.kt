package com.kapwadvo.app.data.repository

import com.kapwadvo.app.data.models.Listing
import com.kapwadvo.app.data.models.ListingInsert
import com.kapwadvo.app.supabase
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.storage.storage
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

import kotlinx.coroutines.flow.map

object ListingRepository {

    fun observeApprovedListings(): kotlinx.coroutines.flow.Flow<List<Listing>> {
        return com.kapwadvo.app.KapwaDVOApp.database.listingDao().observeApprovedListings()
            .map { list -> list.map { it.toListing() } }
    }

    suspend fun syncApprovedListings() {
        try {
            val listings = supabase.from("listings")
                .select { filter { eq("status", "approved") } }
                .decodeList<Listing>()
            val cached = listings.map { com.kapwadvo.app.data.local.entity.CachedListing.fromListing(it) }
            if (listings.isNotEmpty()) {
                com.kapwadvo.app.KapwaDVOApp.database.listingDao().deleteStaleApprovedListings(listings.map { it.id })
            }
            com.kapwadvo.app.KapwaDVOApp.database.listingDao().insertAll(cached)
        } catch (e: Exception) {
            // Silently fail sync, UI will still show cached data
        }
    }

    suspend fun getApprovedListings(): List<Listing> {
        return com.kapwadvo.app.KapwaDVOApp.database.listingDao().getApprovedListings().map { it.toListing() }
    }

    fun observeOwnerListings(ownerId: String): kotlinx.coroutines.flow.Flow<List<Listing>> {
        return com.kapwadvo.app.KapwaDVOApp.database.listingDao().observeOwnerListings(ownerId)
            .map { list -> list.map { it.toListing() } }
    }

    suspend fun getListingsByOwner(ownerId: String): List<Listing> {
        return com.kapwadvo.app.KapwaDVOApp.database.listingDao().getOwnerListings(ownerId).map { it.toListing() }
    }
    
    suspend fun syncOwnerListings(ownerId: String): List<String>? {
        return try {
            val listings = supabase.from("listings")
                .select { filter { eq("owner_id", ownerId) } }
                .decodeList<Listing>()
            val cached = listings.map { com.kapwadvo.app.data.local.entity.CachedListing.fromListing(it) }
            com.kapwadvo.app.KapwaDVOApp.database.listingDao().deleteStaleOwnerListings(ownerId, listings.map { it.id })
            
            if (cached.isNotEmpty()) {
                com.kapwadvo.app.KapwaDVOApp.database.listingDao().insertAll(cached)
            }
            listings.map { it.id }
        } catch (e: Exception) {
            null
        }
    }

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

    suspend fun getListingsByIds(ids: List<String>): List<Listing> = try {
        if (ids.isEmpty()) emptyList()
        else supabase.from("listings")
            .select { filter { isIn("id", ids) } }
            .decodeList<Listing>()
    } catch (e: Exception) { emptyList() }

    suspend fun createListing(insert: ListingInsert) {
        supabase.from("listings").insert(insert)
    }

    suspend fun updateListing(id: String, insert: ListingInsert) {
        supabase.from("listings").update(insert) { filter { eq("id", id) } }
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

    suspend fun cancelPendingUpdate(id: String) {
        supabase.from("listings").update(
            buildJsonObject { put("pending_updates", kotlinx.serialization.json.JsonNull) }
        ) { filter { eq("id", id) } }
    }
}
