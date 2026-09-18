package com.kapwadvo.app

/**
 * Lightweight in-memory session state. Populated after login / session restore.
 * Not persisted across process death — Supabase Auth handles token persistence.
 */
object UserSession {
    var isGuest: Boolean = false
    var userId: String? = null
    var role: String = "guest"
    var name: String = ""
    var email: String = ""

    fun isAdmin() = role == "admin"
    fun isOwner() = role == "owner"
    fun isLoggedIn() = !isGuest && userId != null
    fun isUser() = role == "user"
    fun isOwnerOrAdmin() = role == "owner" || role == "admin"

    fun clear() {
        isGuest = false
        userId = null
        role = "guest"
        name = ""
        email = ""
    }
}
