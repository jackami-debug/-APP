package com.example.appenergytracker.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.appenergytracker.MainActivity
import com.example.appenergytracker.R

object EnergyStatusNotifier {
    private const val CHANNEL_ID = "energy_status"
    private const val CHANNEL_NAME = "能量狀態"
    private const val CHANNEL_DESC = "顯示目前剩餘能量"
    private const val NOTIFICATION_ID = 2001

    private fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_LOW
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = CHANNEL_DESC
            }
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(channel)
        }
    }

    fun show(context: Context, currentEnergyMinutes: Int, maxEnergyMinutes: Int) {
        ensureChannel(context)

        val clampedMax = if (maxEnergyMinutes > 0) maxEnergyMinutes else 1
        val clampedCurrent = currentEnergyMinutes.coerceIn(0, clampedMax)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val title = "剩餘能量: ${clampedCurrent} 分鐘"

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setAutoCancel(false)
            .setOngoing(false)
            .setContentIntent(pendingIntent)
            .setProgress(clampedMax, clampedCurrent, false)
            .setColor(ContextCompat.getColor(context, R.color.purple_500))
            .setColorized(true)
            .setShowWhen(false)
            .setSilent(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setCategory(NotificationCompat.CATEGORY_STATUS)

        with(NotificationManagerCompat.from(context)) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                    notify(NOTIFICATION_ID, builder.build())
                }
            } else {
                notify(NOTIFICATION_ID, builder.build())
            }
        }
    }

    fun hide(context: Context) {
        NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID)
    }
}


