package com.kapwadvo.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.kapwadvo.app.data.models.SavedLocation

@Entity(tableName = "cached_saved_locations")
data class CachedSavedLocation(
    @PrimaryKey
    val id: String,
    val userId: String,
    val listingId: String
) {
    fun toSavedLocation() = SavedLocation(id, userId, listingId)
    
    companion object {
        fun from(savedLocation: SavedLocation) = CachedSavedLocation(
            id = savedLocation.id,
            userId = savedLocation.userId,
            listingId = savedLocation.listingId
        )
    }
}
