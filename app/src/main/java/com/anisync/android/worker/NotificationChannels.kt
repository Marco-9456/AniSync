package com.anisync.android.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build

object NotificationChannels {
    const val AIRING_CHANNEL_ID = "airing_notifications"
    const val PLANNING_CHANNEL_ID = "planning_notifications"
    const val UPCOMING_CHANNEL_ID = "upcoming_notifications"

    fun createChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val airingChannel = NotificationChannel(
                AIRING_CHANNEL_ID,
                "Airing Episodes",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifications for new airing episodes"
            }

            val planningChannel = NotificationChannel(
                PLANNING_CHANNEL_ID,
                "Planning List Updates",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifications when shows in your Planning list start airing"
            }

            val upcomingChannel = NotificationChannel(
                UPCOMING_CHANNEL_ID,
                "Upcoming Episodes",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifications for upcoming episode premieres"
            }

            notificationManager.createNotificationChannels(listOf(airingChannel, planningChannel, upcomingChannel))
        }
    }
}
