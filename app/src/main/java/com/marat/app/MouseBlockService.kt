package com.marat.app

import android.app.*
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.marat.app.ui.HomeFragment

class MouseBlockService : Service() {

    companion object {
        const val CHANNEL_ID = "MouseBlockChannel"
        const val NOTIFICATION_ID = 1
        const val ACTION_START_BLOCK = "com.marat.app.ACTION_START_BLOCK"
        const val ACTION_STOP_BLOCK = "com.marat.app.ACTION_STOP_BLOCK"
        const val ACTION_UNBLOCK_FROM_NOTIFICATION = "com.marat.app.ACTION_UNBLOCK_FROM_NOTIFICATION"
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        Log.d("MouseBlockService", "Service Created")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("MouseBlockService", "onStartCommand received: ${intent?.action}")
        when (intent?.action) {
            ACTION_START_BLOCK -> {
                startForeground(NOTIFICATION_ID, createNotification())
                Log.d("MouseBlockService", "Foreground service started")
                return START_STICKY
            }
            ACTION_STOP_BLOCK -> {
                Log.d("MouseBlockService", "Stopping foreground service")
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return START_NOT_STICKY
            }
        }
        return START_NOT_STICKY
    }

    private fun createNotification(): Notification {
        val unblockIntent = Intent(this, HomeFragment.UnblockReceiver::class.java).apply {
            action = HomeFragment.RECEIVER_ACTION_UNBLOCK
        }

        val pendingIntentFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }

        val unblockPendingIntent = PendingIntent.getBroadcast(
            this,
            0,
            unblockIntent,
            pendingIntentFlag
        )

        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingActivityIntent = PendingIntent.getActivity(
            this,
            0,
            notificationIntent,
            pendingIntentFlag
        )


        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Mouse Block Active")
            .setContentText("Нажмите 'Разблокировать' для снятия блокировки.")
            .setSmallIcon(R.drawable.baseline_block_24)
            .setOngoing(true)
            .addAction(R.drawable.baseline_mouse_24, "Разблокировать", unblockPendingIntent)
            .setContentIntent(pendingActivityIntent)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "Mouse Block Status",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Notification channel for mouse block status"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(serviceChannel)
            Log.d("MouseBlockService", "Notification channel created")
        }
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onDestroy() {
        Log.d("MouseBlockService", "Service Destroyed")
        super.onDestroy()
    }
}