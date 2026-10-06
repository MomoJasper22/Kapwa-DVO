package com.kapwadvo.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.kapwadvo.app.data.local.dao.ListingDao
import com.kapwadvo.app.data.local.entity.CachedListing

import com.kapwadvo.app.data.local.dao.ProfileDao
import com.kapwadvo.app.data.local.entity.CachedProfile

import com.kapwadvo.app.data.local.dao.ReviewDao
import com.kapwadvo.app.data.local.dao.SavedLocationDao
import com.kapwadvo.app.data.local.dao.BookingDao
import com.kapwadvo.app.data.local.entity.CachedReview
import com.kapwadvo.app.data.local.entity.CachedReviewComment
import com.kapwadvo.app.data.local.entity.CachedSavedLocation
import com.kapwadvo.app.data.local.entity.CachedBooking
import com.kapwadvo.app.data.local.dao.PendingSyncActionDao
import com.kapwadvo.app.data.local.entity.PendingSyncAction
@Database(
    entities = [
        CachedListing::class, 
        CachedProfile::class, 
        CachedReview::class, 
        CachedReviewComment::class,
        CachedSavedLocation::class,
        CachedBooking::class,
        PendingSyncAction::class
    ], 
    version = 3, 
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class KapwaDatabase : RoomDatabase() {
    abstract fun listingDao(): ListingDao
    abstract fun profileDao(): ProfileDao
    abstract fun reviewDao(): ReviewDao
    abstract fun savedLocationDao(): SavedLocationDao
    abstract fun bookingDao(): BookingDao
    abstract fun pendingSyncActionDao(): PendingSyncActionDao

    companion object {
        @Volatile
        private var INSTANCE: KapwaDatabase? = null

        val MIGRATION_2_3 = object : androidx.room.migration.Migration(2, 3) {
            override fun migrate(database: androidx.sqlite.db.SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE pending_sync_actions ADD COLUMN retryCount INTEGER NOT NULL DEFAULT 0")
            }
        }

        fun getDatabase(context: Context): KapwaDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    KapwaDatabase::class.java,
                    "kapwa_database"
                )
                .addMigrations(MIGRATION_2_3)
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
