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
            // Push locally modified profile to server before downloading
            AuthRepository.syncProfileToServer()
            
            // Sync globally cached data
            ListingRepository.syncApprovedListings()
            
            // Sync user-specific cached data
            val uid = UserSession.userId
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
}
