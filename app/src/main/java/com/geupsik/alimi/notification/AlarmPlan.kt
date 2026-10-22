package com.geupsik.alimi.notification

import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit

/**
 * 정확한 알람 권한(SCHEDULE_EXACT_ALARM) 없이 정해진 시간 근처에 알림을 띄우기 위한 계산.
 *
 * 권한 없이 쓰는 알람(setAndAllowWhileIdle)은 "남은 시간의 최대 75%"까지 늦게 울릴 수 있다
 * (안드로이드 12 이상은 최대 1시간). 24시간 뒤로 한 번에 맞추면 크게 늦을 수 있어서,
 * 목표 시각까지 남은 시간을 줄여 가며 여러 번 나눠 깨운다.
 *
 *   남은 시간 r이 5분보다 길면 → now + r/1.75 에 중간 알람 (늦어도 목표 시각 전에는 울린다)
 *   5분 이하로 남으면         → 목표 시각에 마지막 알람 (늦어도 4분 남짓)
 *
 * 하루 8번쯤 아주 짧게 깨어나는 정도라 배터리 영향은 거의 없다.
 */
object AlarmPlan {

    val FINAL_HOP_MILLIS = TimeUnit.MINUTES.toMillis(5)

    /** 이보다 많이 늦었으면(예: 폰이 꺼져 있었음) 그날 알림은 건너뛴다. */
    val STALE_AFTER_MILLIS = TimeUnit.HOURS.toMillis(2)

    /** 목표 시각보다 이만큼 이르게 깨어나도 그냥 알림을 보낸다. */
    val EARLY_TOLERANCE_MILLIS = TimeUnit.SECONDS.toMillis(30)

    private const val HOP_DIVISOR = 1.75

    /** now 이후에 처음 오는 hh:mm 시각 */
    fun nextTarget(nowMillis: Long, minuteOfDay: Int, zone: ZoneId = ZoneId.systemDefault()): Long {
        val now = ZonedDateTime.ofInstant(Instant.ofEpochMilli(nowMillis), zone)
        val time = LocalTime.of(minuteOfDay / 60, minuteOfDay % 60)
        var target = now.with(time).withSecond(0).withNano(0)
        if (!target.toInstant().isAfter(now.toInstant())) target = target.plusDays(1)
        return target.toInstant().toEpochMilli()
    }

    /** 다음으로 알람을 걸 시각 */
    fun nextTrigger(nowMillis: Long, targetMillis: Long): Long {
        val remaining = targetMillis - nowMillis
        if (remaining <= FINAL_HOP_MILLIS) return targetMillis
        return nowMillis + (remaining / HOP_DIVISOR).toLong()
    }

    fun isDue(nowMillis: Long, targetMillis: Long): Boolean =
        nowMillis >= targetMillis - EARLY_TOLERANCE_MILLIS

    fun isStale(nowMillis: Long, targetMillis: Long): Boolean =
        nowMillis - targetMillis > STALE_AFTER_MILLIS
}
