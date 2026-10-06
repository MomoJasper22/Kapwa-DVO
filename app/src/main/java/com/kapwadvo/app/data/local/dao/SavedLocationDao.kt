package com.kapwadvo.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.kapwadvo.app.data.local.entity.CachedSavedLocation
import kotlinx.coroutines.flow.Flow

@Dao
interface SavedLocationDao {
    @Query("SELECT * FROM cached_saved_locations WHERE userId = :userId")
    fun observeSavedLocations(userId: String): Flow<List<CachedSavedLocation>>

    @Query("SELECT * FROM cached_saved_locations WHERE userId = :userId")
    suspend fun getSavedLocations(userId: String): List<CachedSavedLocation>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(locations: List<CachedSavedLocation>)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(location: CachedSavedLocation)

    @Query("DELETE FROM cached_saved_locations WHERE userId = :userId AND listingId = :listingId")
    suspend fun delete(userId: String, listingId: String)
    
    @Query("DELETE FROM cached_saved_locations WHERE userId = :userId")
    suspend fun clearForUser(userId: String)
}
