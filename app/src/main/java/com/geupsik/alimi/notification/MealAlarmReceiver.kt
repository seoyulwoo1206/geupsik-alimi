package com.geupsik.alimi.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.geupsik.alimi.GeupsikApp
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * 알람이 울릴 때마다 불린다.
 * 아직 목표 시각 전이면 더 가까운 알람을 다시 걸고, 목표 시각이 되면 알림을 보낸 뒤 내일 알람을 건다.
 */
class MealAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_MEAL_ALARM) return
        val container = (context.applicationContext as GeupsikApp).container
        val target = intent.getLongExtra(EXTRA_TARGET_MILLIS, 0L)
        val pending = goAsync()

        scope.launch {
            try {
                val settings = container.userPrefs.notification.first()
                if (!settings.enabled) return@launch

                val now = System.currentTimeMillis()
                if (target > 0 && !AlarmPlan.isDue(now, target)) {
                    container.mealAlarmScheduler.scheduleToward(target, now)
                    return@launch
                }

                // 먼저 내일 알람을 걸어 둔다. 아래에서 문제가 생겨도 알림이 끊기지 않게.
                container.mealAlarmScheduler.scheduleAfterDelivery(settings, target, now)
                val alreadySent = container.userPrefs.lastNotifiedTarget.first() == target
                if (target > 0 && !alreadySent && !AlarmPlan.isStale(now, target)) {
                    container.userPrefs.setLastNotifiedTarget(target)
                    container.dailyMealNotification.send()
                }
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        const val ACTION_MEAL_ALARM = "com.geupsik.alimi.action.MEAL_ALARM"
        const val EXTRA_TARGET_MILLIS = "target_millis"

        // 백그라운드에서 예상 못 한 오류가 나도 앱이 죽지 않게 기록만 하고 넘어간다
        private val scope = CoroutineScope(
            SupervisorJob() + Dispatchers.IO +
                CoroutineExceptionHandler { _, e -> Log.w("MealAlarm", "알림 처리 실패", e) },
        )
    }
}
