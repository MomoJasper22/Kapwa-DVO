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
                    "CANCEL_RESERVATION" -> {
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
                }
                com.kapwadvo.app.data.repository.SyncQueueRepository.deleteAction(action.id)
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                // If it fails, leave it in the queue for next time
            }
        }
    }
}
