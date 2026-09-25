package com.kapwadvo.app

/**
 * Lightweight in-memory session state. Populated after login / session restore.
 * Not persisted across process death — Supabase Auth handles token persistence.
 */
object UserSession {
    var isGuest: Boolean = false
    var userId: String? = null
    var role: String = "guest"
    var firstName: String = ""
    var lastName: String = ""
    var email: String = ""
    var dob: String = ""
    var phoneNumber: String = ""
    var address: String = ""

    val fullName: String
        get() = listOf(firstName, lastName).filter { it.isNotEmpty() }.joinToString(" ")

    fun isAdmin() = role == "admin"
    fun isOwner() = role == "owner"
    fun isLoggedIn() = !isGuest && userId != null
    fun isUser() = role == "user"
    fun isOwnerOrAdmin() = role == "owner" || role == "admin"

    fun clear() {
        isGuest = false
        userId = null
        role = "guest"
        firstName = ""
        lastName = ""
        email = ""
        dob = ""
        phoneNumber = ""
        address = ""
    }
}
