package com.kapwadvo.app.workers

import android.content.Context
import android.content.SharedPreferences
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.kapwadvo.app.UserSession
import com.kapwadvo.app.data.repository.BookingRepository
import com.kapwadvo.app.data.repository.ListingRepository
import com.kapwadvo.app.notifications.NotificationHelper
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.TimeUnit

class NotificationWorker(appContext: Context, workerParams: WorkerParameters) :
    CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        if (UserSession.userId == null) {
            com.kapwadvo.app.data.repository.AuthRepository.loadSessionFromCache()
        }
        val userId = UserSession.userId ?: return Result.success()

        val prefs = applicationContext.getSharedPreferences("KapwaDVONotifications", Context.MODE_PRIVATE)

        checkUserNotifications(userId, prefs)
        checkOwnerNotifications(userId, prefs)

        return Result.success()
    }

    private suspend fun checkUserNotifications(userId: String, prefs: SharedPreferences) {
        val userBookings = BookingRepository.getBookingsForUser(userId)
        val notifiedStatuses = prefs.getStringSet("notified_status_bookings", mutableSetOf())?.toMutableSet() ?: mutableSetOf()
        val remindedUser = prefs.getStringSet("reminded_user_bookings", mutableSetOf())?.toMutableSet() ?: mutableSetOf()

        val sdf = SimpleDateFormat("yyyy-MM-dd hh:mm a", Locale.getDefault())
        val now = System.currentTimeMillis()
        var prefsChanged = false

        for (booking in userBookings) {
            // Check accepted/declined status change
            if (booking.status == "accepted" || booking.status == "declined") {
                val statusKey = "${booking.id}:${booking.status}"
                if (!notifiedStatuses.contains(statusKey)) {
                    NotificationHelper.showNotification(
                        applicationContext,
                        "Booking ${booking.status.replaceFirstChar { it.uppercase() }}",
                        "Your booking request has been ${booking.status}."
                    )
                    notifiedStatuses.add(statusKey)
                    prefsChanged = true
                }
            }

            // Check 3-6 hour reminder (if accepted)
            if (booking.status == "accepted" && !remindedUser.contains(booking.id)) {
                try {
                    val date = sdf.parse(booking.date)
                    if (date != null) {
                        val diffHours = TimeUnit.MILLISECONDS.toHours(date.time - now)
                        if (diffHours in 3..6) {
                            NotificationHelper.showNotification(
                                applicationContext,
                                "Upcoming Reservation Reminder",
                                "Your reservation is coming up in about $diffHours hours!"
                            )
                            remindedUser.add(booking.id)
                            prefsChanged = true
                        }
                    }
                } catch (e: Exception) { }
            }
        }

        if (prefsChanged) {
            prefs.edit()
                .putStringSet("notified_status_bookings", notifiedStatuses)
                .putStringSet("reminded_user_bookings", remindedUser)
                .apply()
        }
    }

    private suspend fun checkOwnerNotifications(ownerId: String, prefs: SharedPreferences) {
        val listings = ListingRepository.getListingsByOwner(ownerId)
        if (listings.isEmpty()) return

        val ownerBookings = BookingRepository.getBookingsForListings(listings.map { it.id })
        val notifiedNew = prefs.getStringSet("notified_new_bookings", mutableSetOf())?.toMutableSet() ?: mutableSetOf()
        val remindedOwner = prefs.getStringSet("reminded_owner_bookings", mutableSetOf())?.toMutableSet() ?: mutableSetOf()

        val sdf = SimpleDateFormat("yyyy-MM-dd hh:mm a", Locale.getDefault())
        val now = System.currentTimeMillis()
        var prefsChanged = false
        var newPendingCount = 0

        for (booking in ownerBookings) {
            // Check for new pending
            if (booking.status == "pending" && !notifiedNew.contains(booking.id)) {
                newPendingCount++
                notifiedNew.add(booking.id)
                prefsChanged = true
            }

            // Check 24-48 hour reminder
            if (booking.status == "accepted" && !remindedOwner.contains(booking.id)) {
                try {
                    val date = sdf.parse(booking.date)
                    if (date != null) {
                        val diffHours = TimeUnit.MILLISECONDS.toHours(date.time - now)
                        if (diffHours in 24..48) {
                            NotificationHelper.showNotification(
                                applicationContext,
                                "Upcoming Booking",
                                "You have an accepted booking in about $diffHours hours."
                            )
                            remindedOwner.add(booking.id)
                            prefsChanged = true
                        }
                    }
                } catch (e: Exception) { }
            }
        }

        if (newPendingCount > 0) {
            NotificationHelper.showNotification(
                applicationContext,
                "New Booking Requests",
                "You have $newPendingCount new booking request(s) to review."
            )
        }

        if (prefsChanged) {
            prefs.edit()
                .putStringSet("notified_new_bookings", notifiedNew)
                .putStringSet("reminded_owner_bookings", remindedOwner)
                .apply()
        }
    }
}
