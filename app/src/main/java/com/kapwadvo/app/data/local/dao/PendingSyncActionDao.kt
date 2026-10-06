package com.kapwadvo.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.kapwadvo.app.data.local.entity.PendingSyncAction

@Dao
interface PendingSyncActionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(action: PendingSyncAction)

    @Query("SELECT * FROM pending_sync_actions WHERE userId = :userId ORDER BY createdAt ASC")
    suspend fun getPendingActionsForUser(userId: String): List<PendingSyncAction>

    @Query("DELETE FROM pending_sync_actions WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM pending_sync_actions WHERE userId = :userId")
    suspend fun clearForUser(userId: String)
}
