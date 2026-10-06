package com.kapwadvo.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.kapwadvo.app.data.models.Booking

@Entity(tableName = "cached_bookings")
data class CachedBooking(
    @PrimaryKey val id: String,
    val listingId: String,
    val userId: String,
    val date: String,
    val guests: Int,
    val status: String,
    val notes: String?,
    val declineReason: String?
) {
    fun toBooking() = Booking(id, listingId, userId, date, guests, status, notes, declineReason)

    companion object {
        fun from(booking: Booking) = CachedBooking(
            id = booking.id,
            listingId = booking.listingId,
            userId = booking.userId,
            date = booking.date,
            guests = booking.guests,
            status = booking.status,
            notes = booking.notes,
            declineReason = booking.declineReason
        )
    }
}
