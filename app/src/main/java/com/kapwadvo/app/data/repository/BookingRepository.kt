package com.kapwadvo.app.data.repository

import com.kapwadvo.app.data.models.Booking
import com.kapwadvo.app.data.models.BookingInsert
import com.kapwadvo.app.supabase
import io.github.jan.supabase.postgrest.from
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

import kotlinx.coroutines.flow.map

object BookingRepository {

    suspend fun createBooking(insert: BookingInsert) {
        supabase.from("bookings").insert(insert)
        syncUserBookings(insert.userId)
    }

    suspend fun getBookingsForUser(userId: String): List<Booking> {
        return com.kapwadvo.app.KapwaDVOApp.database.bookingDao().getUserBookings(userId).map { it.toBooking() }
    }

    suspend fun syncUserBookings(userId: String) {
        try {
            val bookings = supabase.from("bookings")
                .select { filter { eq("user_id", userId) } }
                .decodeList<Booking>()
            val cached = bookings.map { com.kapwadvo.app.data.local.entity.CachedBooking.from(it) }
            com.kapwadvo.app.KapwaDVOApp.database.bookingDao().clearForUser(userId)
            com.kapwadvo.app.KapwaDVOApp.database.bookingDao().insertAll(cached)
        } catch(e: Exception) {}
    }
    
    suspend fun syncListingBookings(listingId: String) {
        try {
            val bookings = supabase.from("bookings")
                .select { filter { eq("listing_id", listingId) } }
                .decodeList<Booking>()
            val cached = bookings.map { com.kapwadvo.app.data.local.entity.CachedBooking.from(it) }
            com.kapwadvo.app.KapwaDVOApp.database.bookingDao().insertAll(cached)
        } catch(e: Exception) {}
    }

    suspend fun getBookingsForListings(listingIds: List<String>): List<Booking> {
        if (listingIds.isEmpty()) return emptyList()
        return com.kapwadvo.app.KapwaDVOApp.database.bookingDao().getBookingsForListings(listingIds).map { it.toBooking() }
    }
    
    fun observeBookingsForListings(listingIds: List<String>): kotlinx.coroutines.flow.Flow<List<Booking>> {
        if (listingIds.isEmpty()) return kotlinx.coroutines.flow.flowOf(emptyList())
        return com.kapwadvo.app.KapwaDVOApp.database.bookingDao().observeOwnerBookings(listingIds)
            .map { list -> list.map { it.toBooking() } }
    }
    
    suspend fun syncListingsBookings(listingIds: List<String>) {
        if (listingIds.isEmpty()) return
        try {
            val bookings = supabase.from("bookings")
                .select { filter { isIn("listing_id", listingIds) } }
                .decodeList<Booking>()
            val cached = bookings.map { com.kapwadvo.app.data.local.entity.CachedBooking.from(it) }
            // Delete stale owner bookings (even if empty to wipe all)
            com.kapwadvo.app.KapwaDVOApp.database.bookingDao().deleteStaleListingsBookings(listingIds, bookings.map { it.id })
            
            if (cached.isNotEmpty()) {
                com.kapwadvo.app.KapwaDVOApp.database.bookingDao().insertAll(cached)
            }
        } catch (e: Exception) {}
    }

    suspend fun updateBookingStatus(id: String, listingId: String, status: String) {
        supabase.from("bookings").update(
            buildJsonObject { put("status", status) }
        ) { filter { eq("id", id) } }
        syncListingBookings(listingId)
    }

    suspend fun updateBookingStatus(id: String, listingId: String, status: String, declineReason: String?) {
        supabase.from("bookings").update(
            buildJsonObject {
                put("status", status)
                if (declineReason != null) {
                    put("decline_reason", declineReason)
                }
            }
        ) { filter { eq("id", id) } }
        syncListingBookings(listingId)
    }

    suspend fun updateBookingDetails(id: String, listingId: String, date: String, guests: Int, notes: String?) {
        supabase.from("bookings").update(
            buildJsonObject {
                put("date", date)
                put("guests", guests)
                if (notes != null) put("notes", notes) else put("notes", kotlinx.serialization.json.JsonNull)
            }
        ) { filter { eq("id", id) } }
        syncListingBookings(listingId)
    }
}
