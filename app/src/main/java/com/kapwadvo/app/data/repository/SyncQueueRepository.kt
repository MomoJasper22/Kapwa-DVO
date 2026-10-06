package com.kapwadvo.app.data.repository

import com.kapwadvo.app.KapwaDVOApp
import com.kapwadvo.app.data.local.entity.PendingSyncAction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object SyncQueueRepository {

    suspend fun queueAction(userId: String, actionType: String, payload: String) {
        withContext(Dispatchers.IO) {
            val action = PendingSyncAction(
                userId = userId,
                actionType = actionType,
                payload = payload
            )
            KapwaDVOApp.database.pendingSyncActionDao().insert(action)
        }
    }

    suspend fun getPendingActions(userId: String): List<PendingSyncAction> {
        return withContext(Dispatchers.IO) {
            KapwaDVOApp.database.pendingSyncActionDao().getPendingActionsForUser(userId)
        }
    }

    suspend fun deleteAction(id: Long) {
        withContext(Dispatchers.IO) {
            KapwaDVOApp.database.pendingSyncActionDao().delete(id)
        }
    }

    suspend fun updateAction(action: PendingSyncAction) {
        withContext(Dispatchers.IO) {
            KapwaDVOApp.database.pendingSyncActionDao().insert(action)
        }
    }
}
