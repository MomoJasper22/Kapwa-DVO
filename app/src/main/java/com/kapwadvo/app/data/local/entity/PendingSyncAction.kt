package com.kapwadvo.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pending_sync_actions")
data class PendingSyncAction(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: String,
    val actionType: String,
    val payload: String, // JSON representation of the action data
    val createdAt: Long = System.currentTimeMillis()
)
