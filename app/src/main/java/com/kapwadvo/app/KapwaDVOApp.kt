package com.kapwadvo.app

import android.app.Application
import org.osmdroid.config.Configuration

class KapwaDVOApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Configuration.getInstance().apply {
            load(applicationContext, getSharedPreferences("osmdroid", MODE_PRIVATE))
            userAgentValue = packageName
        }
    }
}
