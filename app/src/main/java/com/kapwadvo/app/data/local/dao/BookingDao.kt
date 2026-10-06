package com.kapwadvo.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.kapwadvo.app.data.local.entity.CachedBooking
import kotlinx.coroutines.flow.Flow

@Dao
interface BookingDao {
    @Query("SELECT * FROM cached_bookings WHERE userId = :userId ORDER BY date DESC")
    fun observeUserBookings(userId: String): Flow<List<CachedBooking>>

    @Query("SELECT * FROM cached_bookings WHERE userId = :userId ORDER BY date DESC")
    suspend fun getUserBookings(userId: String): List<CachedBooking>

    @Query("SELECT * FROM cached_bookings WHERE listingId = :listingId ORDER BY date DESC")
    suspend fun getListingBookings(listingId: String): List<CachedBooking>

    @Query("SELECT * FROM cached_bookings WHERE listingId IN (:listingIds) ORDER BY date DESC")
    fun observeOwnerBookings(listingIds: List<String>): Flow<List<CachedBooking>>

    @Query("SELECT * FROM cached_bookings WHERE listingId IN (:listingIds) ORDER BY date DESC")
    suspend fun getBookingsForListings(listingIds: List<String>): List<CachedBooking>
    
    @Query("DELETE FROM cached_bookings WHERE listingId IN (:listingIds) AND id NOT IN (:validIds)")
    suspend fun deleteStaleListingsBookings(listingIds: List<String>, validIds: List<String>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(bookings: List<CachedBooking>)

    @Query("DELETE FROM cached_bookings WHERE userId = :userId")
    suspend fun clearForUser(userId: String)
}
