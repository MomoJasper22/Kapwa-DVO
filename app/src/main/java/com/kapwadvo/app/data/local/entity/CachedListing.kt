package com.kapwadvo.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.kapwadvo.app.data.models.Listing
import kotlinx.serialization.json.JsonObject

@Entity(tableName = "cached_listings")
data class CachedListing(
    @PrimaryKey
    val id: String,
    val ownerId: String?,
    val ownerName: String?,
    val name: String,
    val description: String,
    val category: String,
    val lat: Double?,
    val lng: Double?,
    val status: String,
    val address: String?,
    val hours: String?,
    val contact: String?,
    val bookingsEnabled: Boolean,
    val photoUrls: List<String>,
    val pendingUpdates: JsonObject?,
    val createdAt: String?,
    val cachedAt: Long
) {
    fun toListing(): Listing = Listing(
        id = id,
        ownerId = ownerId,
        ownerName = ownerName,
        name = name,
        description = description,
        category = category,
        lat = lat,
        lng = lng,
        status = status,
        address = address,
        hours = hours,
        contact = contact,
        pendingUpdates = pendingUpdates,
        bookingsEnabled = bookingsEnabled,
        photoUrls = photoUrls,
        createdAt = createdAt
    )

    companion object {
        fun fromListing(listing: Listing, cachedAt: Long = System.currentTimeMillis()): CachedListing {
            return CachedListing(
                id = listing.id,
                ownerId = listing.ownerId,
                ownerName = listing.ownerName,
                name = listing.name,
                description = listing.description,
                category = listing.category,
                lat = listing.lat,
                lng = listing.lng,
                status = listing.status,
                address = listing.address,
                hours = listing.hours,
                contact = listing.contact,
                bookingsEnabled = listing.bookingsEnabled,
                photoUrls = listing.photoUrls,
                pendingUpdates = listing.pendingUpdates,
                createdAt = listing.createdAt,
                cachedAt = cachedAt
            )
        }
    }
}
