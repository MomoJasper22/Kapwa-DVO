package com.kapwadvo.app

import android.content.Context
import com.russhwolf.settings.SharedPreferencesSettings
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.SettingsSessionManager
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.storage.Storage
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.SupabaseClient

lateinit var supabase: SupabaseClient
    private set

fun initSupabase(context: Context) {
    supabase = createSupabaseClient(
        supabaseUrl = BuildConfig.SUPABASE_URL,
        supabaseKey = BuildConfig.SUPABASE_KEY
    ) {
        install(Auth) {
            val prefs = context.getSharedPreferences("supabase_session", Context.MODE_PRIVATE)
            sessionManager = SettingsSessionManager(SharedPreferencesSettings(prefs))
        }
        install(Postgrest)
        install(Storage)
        install(Realtime)
    }
}
