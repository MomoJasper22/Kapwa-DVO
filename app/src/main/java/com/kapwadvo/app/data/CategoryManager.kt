package com.kapwadvo.app.data

import android.content.Context
import android.content.SharedPreferences

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

    fun saveCategories(categories: List<String>) {
        cachedCategories = categories
        prefs?.edit()?.putString(KEY_CATEGORIES, categories.joinToString("|"))?.apply()
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
