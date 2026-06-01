package com.example.nestblr

import android.app.Application
import android.preference.PreferenceManager
import dagger.hilt.android.HiltAndroidApp
import org.osmdroid.config.Configuration

@HiltAndroidApp
class NestBlrApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        // OSM's public tile servers reject requests without a real User-Agent.
        // Must run before any MapView is inflated.
        @Suppress("DEPRECATION")
        Configuration.getInstance().load(
            this,
            PreferenceManager.getDefaultSharedPreferences(this)
        )
        Configuration.getInstance().userAgentValue = packageName
    }
}
