package com.kawaiipet.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import com.kawaiipet.app.BuildConfig
import com.kawaiipet.app.audio.BundledVoiceLoader
import com.posthog.android.PostHogAndroid
import com.posthog.android.PostHogAndroidConfig
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class KawaiiPetApplication : Application() {

    @Inject lateinit var bundledVoiceLoader: BundledVoiceLoader

    override fun onCreate() {
        super.onCreate()
        val apiKey = BuildConfig.POSTHOG_API_KEY.trim()
        if (apiKey.isNotEmpty()) {
            val host = BuildConfig.POSTHOG_HOST.trim().ifEmpty { "https://us.i.posthog.com" }
            PostHogAndroid.setup(this, PostHogAndroidConfig(apiKey = apiKey, host = host))
        }
        bundledVoiceLoader.startWarmup()
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            NOTIFICATION_CHANNEL_ID,
            getString(R.string.notification_channel_name),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = getString(R.string.notification_channel_description)
        }
        getSystemService(NotificationManager::class.java)
            .createNotificationChannel(channel)
    }

    companion object {
        const val NOTIFICATION_CHANNEL_ID = "kawaiipet_overlay"
    }
}
