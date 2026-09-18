package com.kapwadvo.app

import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.storage.Storage

val supabase = createSupabaseClient(
    supabaseUrl = "https://qcawpmgjyumsfoppwwhz.supabase.co",
    supabaseKey = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InFjYXdwbWdqeXVtc2ZvcHB3d2h6Iiwicm9sZSI6ImFub24iLCJpYXQiOjE3ODk2NDI2MjIsImV4cCI6MjEwNTIxODYyMn0.KXTM1Ay3nCSej_-wSAHCmPklvxm-aG2_kv9WNO4tMBs"
) {
    install(Auth)
    install(Postgrest)
    install(Storage)
}
