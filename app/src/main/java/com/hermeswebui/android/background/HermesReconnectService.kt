package com.hermeswebui.android.background

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.hermeswebui.android.MainActivity
import com.hermeswebui.android.R
import com.hermeswebui.android.core.security.UrlOrigins

class HermesReconnectService : Service() {
    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val serverLabel = intent?.getStringExtra(EXTRA_SERVER_LABEL)
            ?.takeIf { it.isNotBlank() }
            ?: getString(R.string.app_name)
        val notification = buildNotification(serverLabel)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                RECONNECT_NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            startForeground(RECONNECT_NOTIFICATION_ID, notification)
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        stopForeground(STOP_FOREGROUND_REMOVE)
        super.onDestroy()
    }

    private fun buildNotification(serverLabel: String): Notification {
        val launchIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            RECONNECT_NOTIFICATION_ID,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val contentText = if (serverLabel == getString(R.string.app_name)) {
            getString(R.string.reconnect_notification_body)
        } else {
            getString(R.string.reconnect_notification_body_with_server, serverLabel)
        }

        return NotificationCompat.Builder(this, HERMES_NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(getString(R.string.reconnect_notification_title))
            .setContentText(contentText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(contentText))
            .setContentIntent(pendingIntent)
            .setOnlyAlertOnce(true)
            .setOngoing(true)
            .setAutoCancel(false)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    companion object {
        private const val EXTRA_SERVER_LABEL = "extra.SERVER_LABEL"
        private const val HERMES_NOTIFICATION_CHANNEL_ID = "hermes_webui_notifications"
        private const val RECONNECT_NOTIFICATION_ID = 20_001

        fun start(context: Context, serverUrl: String) {
            val serverLabel = UrlOrigins.hostFrom(serverUrl) ?: context.getString(R.string.app_name)
            val intent = Intent(context, HermesReconnectService::class.java).apply {
                putExtra(EXTRA_SERVER_LABEL, serverLabel)
            }
            ContextCompat.startForegroundService(context, intent)
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, HermesReconnectService::class.java))
        }
    }
}


