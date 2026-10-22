package com.geupsik.alimi.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.geupsik.alimi.data.prefs.NotificationSettings

/** 매일 급식 알림용 알람을 걸고 끈다. 알람은 하나만 쓰고, 새로 걸면 이전 것이 바뀐다. */
class MealAlarmScheduler(private val context: Context) {

    private val alarmManager = context.getSystemService(AlarmManager::class.java)

    /** 설정에 맞게 다시 건다. 꺼져 있으면 취소한다. */
    fun apply(settings: NotificationSettings, nowMillis: Long = System.currentTimeMillis()) {
        if (!settings.enabled) {
            cancel()
            return
        }
        scheduleToward(AlarmPlan.nextTarget(nowMillis, settings.minuteOfDay), nowMillis)
    }

    /**
     * 방금 알림을 보낸 뒤 다음 날 알람을 건다.
     * 알람은 30초까지 일찍 깨어날 수 있어서, now가 아니라 목표 시각 이후로 계산해야 같은 날 두 번 울리지 않는다.
     */
    fun scheduleAfterDelivery(settings: NotificationSettings, deliveredTarget: Long, nowMillis: Long) {
        val after = maxOf(nowMillis, deliveredTarget) + 1_000
        scheduleToward(AlarmPlan.nextTarget(after, settings.minuteOfDay), nowMillis)
    }

    /** 목표 시각을 향해 다음 단계 알람을 건다. */
    fun scheduleToward(targetMillis: Long, nowMillis: Long = System.currentTimeMillis()) {
        val trigger = AlarmPlan.nextTrigger(nowMillis, targetMillis)
        // 권한이 필요 없는 부정확 알람. 절전(Doze) 중에도 울린다.
        alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, trigger, pendingIntent(targetMillis))
    }

    fun cancel() {
        alarmManager.cancel(pendingIntent(0L))
    }

    private fun pendingIntent(targetMillis: Long): PendingIntent {
        // extra는 PendingIntent를 구분하는 데 쓰이지 않아서, 같은 requestCode면 늘 같은 알람이 덮어써진다
        val intent = Intent(context, MealAlarmReceiver::class.java)
            .setAction(MealAlarmReceiver.ACTION_MEAL_ALARM)
            .putExtra(MealAlarmReceiver.EXTRA_TARGET_MILLIS, targetMillis)
        return PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private companion object {
        const val REQUEST_CODE = 1001
    }
}
