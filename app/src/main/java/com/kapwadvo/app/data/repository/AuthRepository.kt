package com.kapwadvo.app.data.repository

import com.kapwadvo.app.KapwaDVOApp
import com.kapwadvo.app.UserSession
import com.kapwadvo.app.data.models.Profile
import com.kapwadvo.app.supabase
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.OtpType
import io.github.jan.supabase.auth.user.UserInfo
import io.github.jan.supabase.exceptions.RestException
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.storage.storage
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

object AuthRepository {

    suspend fun getCurrentUser(): UserInfo? {
        val user = supabase.auth.currentUserOrNull()
        if (user != null) return user
        
        return supabase.auth.sessionManager.loadSessionOrNull()?.user
    }

    suspend fun login(email: String, password: String) {
        supabase.auth.signInWith(Email) {
            this.email = email
            this.password = password
        }
        loadSession()
    }

    suspend fun signup(email: String, password: String, firstName: String, lastName: String) {
        val user = supabase.auth.signUpWith(Email) {
            this.email = email
            this.password = password
            data = buildJsonObject {
                put("first_name", firstName)
                put("last_name", lastName)
            }
        }
        
        if (user != null && user.identities?.isEmpty() == true) {
            throw Exception("EMAIL_ALREADY_REGISTERED")
        }
        // We no longer try to insert into `profiles` here.
        // A Supabase trigger will handle it using the metadata above.
    }

    suspend fun logout() {
        try {
            supabase.auth.signOut()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            UserSession.clear()
            KapwaDVOApp.database.profileDao().clear()
        }
    }

    /**
     * Returns true if the currently signed-up user has confirmed their email.
     * Supabase sets emailConfirmedAt once the link is clicked.
     */
    suspend fun isEmailConfirmed(): Boolean {
        return try {
            // Refresh the session so we get the latest user data from Supabase.
            supabase.auth.refreshCurrentSession()
            val user = getCurrentUser()
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

    suspend fun getProfiles(userIds: List<String>): List<Profile> = try {
        if (userIds.isEmpty()) emptyList()
        else supabase.from("profiles")
            .select { filter { isIn("id", userIds) } }
            .decodeList<Profile>()
    } catch (e: Exception) { emptyList() }

    suspend fun loadSessionFromCache(): Boolean {
        val user = getCurrentUser() ?: return false
        val cached = com.kapwadvo.app.KapwaDVOApp.database.profileDao().getProfile()
        if (cached != null && cached.id == user.id) {
            UserSession.isGuest = false
            UserSession.userId = user.id
            UserSession.role = cached.role
            UserSession.firstName = cached.firstName ?: ""
            UserSession.lastName = cached.lastName ?: ""
            UserSession.email = cached.email ?: user.email ?: ""
            UserSession.dob = cached.dob ?: ""
            UserSession.phoneNumber = cached.phoneNumber ?: ""
            UserSession.address = cached.address ?: ""
            UserSession.avatarUrl = cached.avatarUrl
            return true
        }
        return false
    }

    suspend fun loadSession() {
        // Sync local offline changes to the server first
        syncProfileToServer()
        
        val user = getCurrentUser() ?: return
        val profile = getProfile(user.id)
        if (profile != null) {
            com.kapwadvo.app.KapwaDVOApp.database.profileDao().insert(com.kapwadvo.app.data.local.entity.CachedProfile.fromProfile(profile))
            UserSession.isGuest = false
            UserSession.userId = user.id
            UserSession.role = profile.role
            UserSession.firstName = profile.firstName ?: ""
            UserSession.lastName = profile.lastName ?: ""
            UserSession.email = profile.email ?: user.email ?: ""
            UserSession.dob = profile.dob ?: ""
            UserSession.phoneNumber = profile.phoneNumber ?: ""
            UserSession.address = profile.address ?: ""
            UserSession.avatarUrl = profile.avatarUrl
        } else {
            // Only set minimal info if we couldn't fetch the profile (e.g. offline)
            // but we don't want to overwrite a valid loaded cache with defaults.
            if (UserSession.userId != user.id) {
                UserSession.isGuest = false
                UserSession.userId = user.id
                UserSession.role = "user"
                UserSession.email = user.email ?: ""
            }
        }
    }

    suspend fun syncProfileToServer() {
        val user = getCurrentUser() ?: return
        val cached = KapwaDVOApp.database.profileDao().getProfile()
        if (cached != null && cached.id == user.id) {
            try {
                supabase.from("profiles").update(
                    buildJsonObject {
                        put("role", cached.role)
                        if (cached.firstName != null) put("first_name", cached.firstName)
                        if (cached.lastName != null) put("last_name", cached.lastName)
                        if (cached.dob != null) put("dob", cached.dob)
                        if (cached.phoneNumber != null) put("phone_number", cached.phoneNumber)
                        if (cached.address != null) put("address", cached.address)
                        // avatarUrl shouldn't be synced this way as it involves a bucket, but we can update the URL
                        if (cached.avatarUrl != null) put("avatar_url", cached.avatarUrl)
                    }
                ) { filter { eq("id", user.id) } }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
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
        
        // Update in-memory session
        UserSession.firstName = firstName
        UserSession.lastName = lastName
        UserSession.dob = dob
        UserSession.phoneNumber = phoneNumber
        UserSession.address = address
        
        // Update room cache
        val currentProfile = KapwaDVOApp.database.profileDao().getProfile()
        if (currentProfile != null) {
            val updatedProfile = currentProfile.copy(
                firstName = firstName,
                lastName = lastName,
                dob = dob,
                phoneNumber = phoneNumber,
                address = address
            )
            KapwaDVOApp.database.profileDao().insert(updatedProfile)
        }

        try {
            supabase.from("profiles").update(
                buildJsonObject {
                    put("first_name", firstName)
                    put("last_name", lastName)
                    if (dob.isNotBlank()) put("dob", dob)
                    if (phoneNumber.isNotBlank()) put("phone_number", phoneNumber)
                    if (address.isNotBlank()) put("address", address)
                }
            ) { filter { eq("id", userId) } }
        } catch (e: Exception) {
            e.printStackTrace()
            // The local DB and session are updated, NetworkSyncWorker will sync this later
        }
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
        
        UserSession.role = newRole
        
        // Update room cache
        val currentProfile = KapwaDVOApp.database.profileDao().getProfile()
        if (currentProfile != null) {
            val updatedProfile = currentProfile.copy(role = newRole)
            KapwaDVOApp.database.profileDao().insert(updatedProfile)
        }

        try {
            supabase.from("profiles").update(
                buildJsonObject { put("role", newRole) }
            ) { filter { eq("id", userId) } }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun hasSession(): Boolean {
        if (getCurrentUser() == null) return false
        
        return try {
            supabase.auth.refreshCurrentSession()
            true
        } catch (e: RestException) {
            false
        } catch (e: Exception) {
            // For network errors (like UnknownHostException or ConnectException), 
            // we still have a session locally.
            true
        }
    }

    suspend fun currentUserId(): String? = getCurrentUser()?.id

    suspend fun uploadAvatar(bytes: ByteArray): String {
        val userId = currentUserId() ?: throw Exception("User not logged in")
        val fileName = "${userId}/profile_${System.currentTimeMillis()}.jpg"
        
        val bucket = supabase.storage.from("avatars")
        bucket.upload(fileName, bytes) { upsert = true }
        
        val publicUrl = bucket.publicUrl(fileName)
        
        // Update database
        supabase.from("profiles").update(
            buildJsonObject {
                put("avatar_url", publicUrl)
            }
        ) { filter { eq("id", userId) } }
        
        UserSession.avatarUrl = publicUrl
        
        // Update room cache
        val profile = getProfile(userId)
        if (profile != null) {
            com.kapwadvo.app.KapwaDVOApp.database.profileDao().insert(com.kapwadvo.app.data.local.entity.CachedProfile.fromProfile(profile))
        }
        
        return publicUrl
    }
}
