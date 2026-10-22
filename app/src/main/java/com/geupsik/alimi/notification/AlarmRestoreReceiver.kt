package com.geupsik.alimi.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.geupsik.alimi.GeupsikApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * 재부팅, 앱 업데이트, 시간·시간대 변경 뒤에 알람을 다시 건다.
 * (재부팅하면 걸어 둔 알람이 모두 사라지고, 시간이 바뀌면 목표 시각 계산이 어긋난다)
 */
class AlarmRestoreReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action !in HANDLED_ACTIONS) return
        val container = (context.applicationContext as GeupsikApp).container
        val pending = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                container.mealAlarmScheduler.apply(container.userPrefs.notification.first())
            } finally {
                pending.finish()
            }
        }
    }

    private companion object {
        val HANDLED_ACTIONS = setOf(
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
        )
    }
}
