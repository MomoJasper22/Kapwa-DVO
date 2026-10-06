package com.kapwadvo.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.kapwadvo.app.data.local.entity.CachedListing
import kotlinx.coroutines.flow.Flow

@Dao
interface ListingDao {
    @Query("SELECT * FROM cached_listings WHERE status = 'approved'")
    fun observeApprovedListings(): Flow<List<CachedListing>>

    @Query("SELECT * FROM cached_listings WHERE status = 'approved'")
    suspend fun getApprovedListings(): List<CachedListing>

    @Query("SELECT * FROM cached_listings WHERE id = :id")
    suspend fun getListingById(id: String): CachedListing?

    @Query("SELECT * FROM cached_listings WHERE id IN (:ids)")
    suspend fun getListingsByIds(ids: List<String>): List<CachedListing>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(listings: List<CachedListing>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(listing: CachedListing)

    @Query("DELETE FROM cached_listings WHERE status = 'approved' AND id NOT IN (:validIds)")
    suspend fun deleteStaleApprovedListings(validIds: List<String>)
    
    @Query("SELECT * FROM cached_listings WHERE ownerId = :ownerId")
    fun observeOwnerListings(ownerId: String): Flow<List<CachedListing>>
    
    @Query("SELECT * FROM cached_listings WHERE ownerId = :ownerId")
    suspend fun getOwnerListings(ownerId: String): List<CachedListing>
    
    @Query("DELETE FROM cached_listings WHERE ownerId = :ownerId AND id NOT IN (:validIds) AND status != 'approved'")
    suspend fun deleteStaleOwnerListings(ownerId: String, validIds: List<String>)
}
