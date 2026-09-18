package com.kapwadvo.app.data.repository

import com.kapwadvo.app.data.models.Booking
import com.kapwadvo.app.data.models.BookingInsert
import com.kapwadvo.app.supabase
import io.github.jan.supabase.postgrest.from
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

object BookingRepository {

    suspend fun createBooking(insert: BookingInsert) {
        supabase.from("bookings").insert(insert)
    }

    suspend fun getBookingsForUser(userId: String): List<Booking> = try {
        supabase.from("bookings")
            .select { filter { eq("user_id", userId) } }
            .decodeList<Booking>()
    } catch (e: Exception) { emptyList() }

    suspend fun getBookingsForListing(listingId: String): List<Booking> = try {
        supabase.from("bookings")
            .select { filter { eq("listing_id", listingId) } }
            .decodeList<Booking>()
    } catch (e: Exception) { emptyList() }

    suspend fun updateBookingStatus(id: String, status: String) {
        supabase.from("bookings").update(
            buildJsonObject { put("status", status) }
        ) { filter { eq("id", id) } }
    }
}
