package com.kapwadvo.app.data.local

import androidx.room.TypeConverter
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString

class Converters {
    private val json = Json { ignoreUnknownKeys = true }

    @TypeConverter
    fun fromStringList(value: List<String>?): String {
        return value?.let { json.encodeToString(it) } ?: "[]"
    }

    @TypeConverter
    fun toStringList(value: String): List<String> {
        return try {
            json.decodeFromString<List<String>>(value)
        } catch (e: Exception) {
            emptyList()
        }
    }

    @TypeConverter
    fun fromJsonObject(value: JsonObject?): String? {
        return value?.let { json.encodeToString(it) }
    }

    @TypeConverter
    fun toJsonObject(value: String?): JsonObject? {
        if (value.isNullOrEmpty()) return null
        return try {
            json.decodeFromString<JsonObject>(value)
        } catch (e: Exception) {
            null
        }
    }
}
