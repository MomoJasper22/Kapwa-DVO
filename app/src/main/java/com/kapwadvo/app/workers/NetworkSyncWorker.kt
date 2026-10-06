package com.kapwadvo.app.workers

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.kapwadvo.app.UserSession
import com.kapwadvo.app.data.repository.AuthRepository
import com.kapwadvo.app.data.repository.BookingRepository
import com.kapwadvo.app.data.repository.ListingRepository
import com.kapwadvo.app.data.repository.SavedRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import io.github.jan.supabase.postgrest.from

class NetworkSyncWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            if (UserSession.userId == null) {
                AuthRepository.loadSessionFromCache()
            }
            
            val uid = UserSession.userId
            if (uid != null) {
                processPendingActions(uid)
            }
            
            // Push locally modified profile to server before downloading
            AuthRepository.syncProfileToServer()
            
            // Sync globally cached data
            ListingRepository.syncApprovedListings()
            com.kapwadvo.app.data.CategoryManager.syncCategoriesFromSupabase()
            
            // Sync user-specific cached data
            if (UserSession.isLoggedIn() && uid != null) {
                SavedRepository.syncSavedForUser(uid)
                BookingRepository.syncUserBookings(uid)
                
                if (UserSession.isOwner()) {
                    val ownerListingIds = ListingRepository.syncOwnerListings(uid)
                    if (ownerListingIds != null && ownerListingIds.isNotEmpty()) {
                        BookingRepository.syncListingsBookings(ownerListingIds)
                    }
                }
            }
            
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    private suspend fun processPendingActions(userId: String) {
        val actions = com.kapwadvo.app.data.repository.SyncQueueRepository.getPendingActions(userId)
        for (action in actions) {
            try {
                val json = org.json.JSONObject(action.payload)
                when (action.actionType) {
                    "SUBMIT_PUBLIC_SPOT" -> {
                        val request = kotlinx.serialization.json.Json.decodeFromString<com.kapwadvo.app.data.models.PublicSpotRequestInsert>(action.payload)
                        com.kapwadvo.app.data.repository.PublicSpotRepository.createRequest(request)
                    }
                    "SUBMIT_OWNER_APPLICATION" -> {
                        val application = kotlinx.serialization.json.Json.decodeFromString<com.kapwadvo.app.data.models.OwnerApplicationInsert>(action.payload)
                        com.kapwadvo.app.data.repository.ApplicationRepository.submitApplication(application)
                    }
                    "CANCEL_RESERVATION", "UPDATE_RESERVATION_STATUS" -> {
                        val bookingId = json.getString("id")
                        val listingId = json.getString("listingId")
                        val status = json.getString("status")
                        BookingRepository.updateBookingStatus(bookingId, listingId, status)
                    }
                    "DISABLE_BOOKING" -> {
                        val listingId = json.getString("listingId")
                        val enable = json.getBoolean("enable")
                        ListingRepository.toggleBookingEnabled(listingId, enable)
                    }
                    "TOGGLE_SAVE" -> {
                        val listingId = json.getString("listingId")
                        val isSaved = json.getBoolean("saved")
                        if (isSaved) {
                            val insert = com.kapwadvo.app.data.models.SavedLocationInsert(userId = userId, listingId = listingId)
                            com.kapwadvo.app.supabase.from("saved_locations").insert(insert)
                        } else {
                            com.kapwadvo.app.supabase.from("saved_locations").delete {
                                filter {
                                    eq("user_id", userId)
                                    eq("listing_id", listingId)
                                }
                            }
                        }
                    }
                }
                com.kapwadvo.app.data.repository.SyncQueueRepository.deleteAction(action.id)
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                android.util.Log.e("NetworkSyncWorker", "Failed to process action ${action.id}", e)
                if (action.retryCount >= 3) {
                    android.util.Log.e("NetworkSyncWorker", "Action ${action.id} exceeded retry limit. Dropping.")
                    com.kapwadvo.app.data.repository.SyncQueueRepository.deleteAction(action.id)
                } else {
                    val updatedAction = action.copy(retryCount = action.retryCount + 1)
                    com.kapwadvo.app.data.repository.SyncQueueRepository.updateAction(updatedAction)
                }
            }
        }
    }
}
