package com.kapwadvo.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.kapwadvo.app.data.local.entity.CachedProfile
import kotlinx.coroutines.flow.Flow

@Dao
interface ProfileDao {
    @Query("SELECT * FROM cached_profile LIMIT 1")
    suspend fun getProfile(): CachedProfile?

    @Query("SELECT * FROM cached_profile LIMIT 1")
    fun observeProfile(): Flow<CachedProfile?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(profile: CachedProfile)

    @Query("DELETE FROM cached_profile")
    suspend fun clear()
}
