package com.geupsik.alimi.notification

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.geupsik.alimi.R
import com.geupsik.alimi.ui.MainActivity

/** 급식 알림 채널을 만들고 알림을 띄운다. */
class MealNotifier(private val context: Context) {

    private val manager = NotificationManagerCompat.from(context)

    fun createChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "오늘의 급식",
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply { description = "매일 정한 시간에 오늘 급식 메뉴를 알려 줘요." }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    /** 앱 알림이 시스템에서 꺼져 있거나(13 이상은 권한 거부 포함) 하면 false */
    fun canNotify(): Boolean = manager.areNotificationsEnabled()

    // 권한은 canNotify()로 먼저 확인하고, 그 사이에 바뀌는 경우는 SecurityException으로 막는다
    @SuppressLint("MissingPermission")
    fun show(text: NotificationText) {
        if (!canNotify()) return

        val openApp = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(text.title)
            .setContentText(text.summary)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text.body))
            .setContentIntent(openApp)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .build()
        try {
            manager.notify(NOTIFICATION_ID, notification)
        } catch (_: SecurityException) {
            // 알림 권한이 막 꺼진 경우. 조용히 넘어간다.
        }
    }

    companion object {
        const val CHANNEL_ID = "meal_daily"
        private const val NOTIFICATION_ID = 1
    }
}
