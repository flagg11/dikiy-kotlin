package com.rut.campusnavigation

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import org.osmdroid.config.Configuration

@HiltAndroidApp
class CampusNavigationApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Configuration.getInstance().apply {
            userAgentValue = packageName
            load(this@CampusNavigationApp, getSharedPreferences("osmdroid", MODE_PRIVATE))
        }
    }
}
