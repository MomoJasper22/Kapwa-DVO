package com.kapwadvo.app.data.repository

import com.kapwadvo.app.UserSession
import com.kapwadvo.app.data.models.Profile
import com.kapwadvo.app.supabase
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.OtpType
import io.github.jan.supabase.postgrest.from
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

object AuthRepository {

    suspend fun login(email: String, password: String) {
        supabase.auth.signInWith(Email) {
            this.email = email
            this.password = password
        }
        loadSession()
    }

    suspend fun signup(email: String, password: String, firstName: String, lastName: String) {
        supabase.auth.signUpWith(Email) {
            this.email = email
            this.password = password
            data = buildJsonObject {
                put("first_name", firstName)
                put("last_name", lastName)
            }
        }
        // We no longer try to insert into `profiles` here.
        // A Supabase trigger will handle it using the metadata above.
    }

    suspend fun logout() {
        supabase.auth.signOut()
        UserSession.clear()
    }

    /**
     * Returns true if the currently signed-up user has confirmed their email.
     * Supabase sets emailConfirmedAt once the link is clicked.
     */
    suspend fun isEmailConfirmed(): Boolean {
        return try {
            // Refresh the session so we get the latest user data from Supabase.
            supabase.auth.refreshCurrentSession()
            val user = supabase.auth.currentUserOrNull()
            user?.emailConfirmedAt != null
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Resends the confirmation email to the given address using Supabase OTP.
     */
    suspend fun resendConfirmationEmail(email: String) {
        supabase.auth.resendEmail(
            type  = OtpType.Email.SIGNUP,
            email = email
        )
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
        UserSession.firstName = profile?.firstName ?: ""
        UserSession.lastName = profile?.lastName ?: ""
        UserSession.email = profile?.email ?: user.email ?: ""
        UserSession.dob = profile?.dob ?: ""
        UserSession.phoneNumber = profile?.phoneNumber ?: ""
        UserSession.address = profile?.address ?: ""
    }

    /**
     * Updates the profile fields (name, DOB, phone, address) in the profiles table.
     */
    suspend fun updateProfile(
        firstName: String,
        lastName: String,
        dob: String,
        phoneNumber: String,
        address: String
    ) {
        val userId = currentUserId() ?: return
        supabase.from("profiles").update(
            buildJsonObject {
                put("first_name", firstName)
                put("last_name", lastName)
                if (dob.isNotBlank()) put("dob", dob)
                if (phoneNumber.isNotBlank()) put("phone_number", phoneNumber)
                if (address.isNotBlank()) put("address", address)
            }
        ) { filter { eq("id", userId) } }

        // Update in-memory session
        UserSession.firstName = firstName
        UserSession.lastName = lastName
        UserSession.dob = dob
        UserSession.phoneNumber = phoneNumber
        UserSession.address = address
    }

    /**
     * Updates email and/or password via Supabase Auth.
     * Returns true if a confirmation email was sent (i.e. email/password was changed).
     */
    suspend fun updateAuthCredentials(newEmail: String, newPassword: String): Boolean {
        val currentEmail = UserSession.email
        val emailChanged = newEmail.isNotBlank() && newEmail != currentEmail
        val passwordChanged = newPassword.isNotBlank()

        if (!emailChanged && !passwordChanged) return false

        supabase.auth.updateUser {
            if (emailChanged) email = newEmail
            if (passwordChanged) password = newPassword
        }
        return true
    }

    suspend fun switchRole(newRole: String) {
        val userId = currentUserId() ?: return
        supabase.from("profiles").update(
            buildJsonObject { put("role", newRole) }
        ) { filter { eq("id", userId) } }
        UserSession.role = newRole
    }

    fun hasSession(): Boolean = supabase.auth.currentSessionOrNull() != null

    fun currentUserId(): String? = supabase.auth.currentUserOrNull()?.id
}
