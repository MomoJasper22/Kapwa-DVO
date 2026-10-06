package com.kapwadvo.app

import android.app.Application
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.kapwadvo.app.notifications.NotificationHelper
import com.kapwadvo.app.workers.NotificationWorker
import java.util.concurrent.TimeUnit
import com.kapwadvo.app.data.CategoryManager
import org.osmdroid.config.Configuration

class KapwaDVOApp : Application() {
    companion object {
        lateinit var database: com.kapwadvo.app.data.local.KapwaDatabase
            private set
        lateinit var networkMonitor: com.kapwadvo.app.util.NetworkMonitor
            private set
    }

    override fun onCreate() {
        super.onCreate()
        database = com.kapwadvo.app.data.local.KapwaDatabase.getDatabase(this)
        networkMonitor = com.kapwadvo.app.util.NetworkMonitor(this)
        
        Configuration.getInstance().apply {
            load(applicationContext, getSharedPreferences("osmdroid", MODE_PRIVATE))
            userAgentValue = packageName
            val cacheDir = java.io.File(applicationContext.cacheDir, "osmdroid")
            if (!cacheDir.exists()) cacheDir.mkdirs()
            osmdroidBasePath = cacheDir
            osmdroidTileCache = cacheDir
        }
        initSupabase(applicationContext)
        CategoryManager.init(this)

        NotificationHelper.createNotificationChannel(this)

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val workRequest = PeriodicWorkRequestBuilder<NotificationWorker>(1, TimeUnit.HOURS)
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "KapwaNotificationWork",
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )

        val periodicConstraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.UNMETERED)
            .build()
            
        val syncWorkRequest = PeriodicWorkRequestBuilder<com.kapwadvo.app.workers.NetworkSyncWorker>(2, TimeUnit.HOURS)
            .setConstraints(periodicConstraints)
            .build()
            
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "KapwaNetworkSyncWork",
            ExistingPeriodicWorkPolicy.KEEP,
            syncWorkRequest
        )

        val oneTimeSync = androidx.work.OneTimeWorkRequestBuilder<com.kapwadvo.app.workers.NetworkSyncWorker>()
            .setConstraints(constraints)
            .build()
            
        WorkManager.getInstance(this).enqueueUniqueWork(
            "KapwaInitialSync",
            androidx.work.ExistingWorkPolicy.REPLACE,
            oneTimeSync
        )
    }
}
