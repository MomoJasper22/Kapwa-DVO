package com.kapwadvo.app.data

import android.content.Context
import android.content.SharedPreferences
import com.kapwadvo.app.supabase
import io.github.jan.supabase.postgrest.from

/**
 * Manages the list of listing/application categories.
 * Categories are persisted to SharedPreferences so admins can add/edit/delete them.
 * The "Others" category is always present and allows the user to specify a custom value.
 */
object CategoryManager {

    private const val PREF_NAME = "kapwa_categories"
    private const val KEY_CATEGORIES = "category_list"
    const val OTHERS = "Others"

    // No hardcoded default categories as per requirements
    private val DEFAULT_CATEGORIES = emptyList<String>()

    private var prefs: SharedPreferences? = null
    private var cachedCategories: List<String>? = null

    fun init(context: Context) {
        prefs = context.applicationContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    fun getCategories(): List<String> {
        cachedCategories?.let { return it }
        val saved = prefs?.getString(KEY_CATEGORIES, null)
        val result = if (saved.isNullOrEmpty()) {
            DEFAULT_CATEGORIES
        } else {
            saved.split("|").filter { it.isNotBlank() }
        }
        cachedCategories = result
        return result
    }

    suspend fun syncCategoriesFromSupabase() {
        try {
            val supabaseCategories = supabase.from("categories")
                .select()
                .decodeList<com.kapwadvo.app.data.models.Category>()
            val names = supabaseCategories.map { it.name }.sorted()
            saveToLocalCache(names)
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
        }
    }

    private fun saveToLocalCache(categories: List<String>) {
        cachedCategories = categories
        prefs?.edit()?.putString(KEY_CATEGORIES, categories.joinToString("|"))?.apply()
    }

    suspend fun addCategory(name: String) {
        try {
            val insert = kotlinx.serialization.json.buildJsonObject {
                put("name", kotlinx.serialization.json.JsonPrimitive(name))
            }
            supabase.from("categories").insert(insert)
            syncCategoriesFromSupabase()
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            throw e
        }
    }

    suspend fun deleteCategory(name: String) {
        try {
            supabase.from("categories").delete {
                filter { eq("name", name) }
            }
            syncCategoriesFromSupabase()
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            throw e
        }
    }

    suspend fun updateCategory(oldName: String, newName: String) {
        try {
            val update = kotlinx.serialization.json.buildJsonObject {
                put("name", kotlinx.serialization.json.JsonPrimitive(newName))
            }
            supabase.from("categories").update(update) {
                filter { eq("name", oldName) }
            }
            syncCategoriesFromSupabase()
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            throw e
        }
    }

    // ── Encoding / Decoding multi-select + Others ─────────────────────────────

    /**
     * Encodes selected categories + optional custom "Others" text into the stored string format.
     * Format: "Category1,Category2,Others: <custom text>"
     */
    fun encode(selected: Set<String>, othersText: String = ""): String {
        return selected.map {
            if (it == OTHERS && othersText.isNotBlank()) "$OTHERS: $othersText" else it
        }.joinToString(",")
    }

    /**
     * Decodes a stored category string back to selected chips + others text.
     * Returns Pair(selectedChipSet, othersCustomText)
     */
    fun decode(stored: String): Pair<Set<String>, String> {
        if (stored.isBlank()) return Pair(emptySet(), "")
        val parts = stored.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        val selected = mutableSetOf<String>()
        var othersText = ""
        for (part in parts) {
            if (part.startsWith("$OTHERS: ")) {
                selected.add(OTHERS)
                othersText = part.removePrefix("$OTHERS: ").trim()
            } else {
                selected.add(part)
            }
        }
        return Pair(selected, othersText)
    }

    /**
     * Returns the display string for users (hides "Others: <text>", shows just "Others").
     */
    fun toDisplayString(stored: String): String {
        if (stored.isBlank()) return ""
        return stored.split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .joinToString(" • ") { part ->
                if (part.startsWith("$OTHERS: ")) OTHERS else part
            }
    }

    /**
     * Returns the admin/owner display string that shows the full "Others: <custom text>".
     */
    fun toAdminDisplayString(stored: String): String {
        if (stored.isBlank()) return ""
        return stored.split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .joinToString(" • ")
    }
}
