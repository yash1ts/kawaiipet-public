package com.kawaiipet.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import com.kawaiipet.app.audio.BundledVoiceLoader
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class KawaiiPetApplication : Application() {

    @Inject lateinit var bundledVoiceLoader: BundledVoiceLoader

    override fun onCreate() {
        super.onCreate()
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
