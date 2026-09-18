package com.kapwadvo.app.data.repository

import com.kapwadvo.app.UserSession
import com.kapwadvo.app.data.models.Profile
import com.kapwadvo.app.supabase
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.postgrest.from

object AuthRepository {

    suspend fun login(email: String, password: String) {
        supabase.auth.signInWith(Email) {
            this.email = email
            this.password = password
        }
        loadSession()
    }

    suspend fun signup(email: String, password: String, name: String) {
        supabase.auth.signUpWith(Email) {
            this.email = email
            this.password = password
        }
        val userId = supabase.auth.currentUserOrNull()?.id ?: return
        supabase.from("profiles").insert(
            Profile(id = userId, role = "user", name = name, email = email, status = "active")
        )
        loadSession()
    }

    suspend fun logout() {
        supabase.auth.signOut()
        UserSession.clear()
    }

    suspend fun getProfile(userId: String): Profile? = try {
        supabase.from("profiles")
            .select { filter { eq("id", userId) } }
            .decodeSingleOrNull<Profile>()
    } catch (e: Exception) { null }

    suspend fun loadSession() {
        val user = supabase.auth.currentUserOrNull() ?: return
        val profile = getProfile(user.id)
        UserSession.isGuest = false
        UserSession.userId = user.id
        UserSession.role = profile?.role ?: "user"
        UserSession.name = profile?.name ?: ""
        UserSession.email = profile?.email ?: user.email ?: ""
    }

    fun hasSession(): Boolean = supabase.auth.currentSessionOrNull() != null

    fun currentUserId(): String? = supabase.auth.currentUserOrNull()?.id
}
