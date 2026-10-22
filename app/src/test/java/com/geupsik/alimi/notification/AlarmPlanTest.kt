package com.geupsik.alimi.notification

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.concurrent.TimeUnit

class AlarmPlanTest {

    private val seoul = ZoneId.of("Asia/Seoul")

    private fun millis(y: Int, mo: Int, d: Int, h: Int, mi: Int) =
        LocalDateTime.of(y, mo, d, h, mi).atZone(seoul).toInstant().toEpochMilli()

    @Test
    fun `아직 안 지났으면 오늘 그 시각`() {
        val now = millis(2026, 10, 22, 6, 0)
        assertEquals(millis(2026, 10, 22, 7, 30), AlarmPlan.nextTarget(now, 7 * 60 + 30, seoul))
    }

    @Test
    fun `이미 지났거나 딱 그 시각이면 내일`() {
        assertEquals(
            millis(2026, 10, 23, 7, 30),
            AlarmPlan.nextTarget(millis(2026, 10, 22, 9, 0), 7 * 60 + 30, seoul),
        )
        assertEquals(
            millis(2026, 10, 23, 7, 30),
            AlarmPlan.nextTarget(millis(2026, 10, 22, 7, 30), 7 * 60 + 30, seoul),
        )
    }

    @Test
    fun `5분 이내면 목표 시각에 바로 건다`() {
        val target = millis(2026, 10, 22, 7, 30)
        val now = target - TimeUnit.MINUTES.toMillis(4)
        assertEquals(target, AlarmPlan.nextTrigger(now, target))
    }

    @Test
    fun `멀면 중간 알람은 가장 늦게 울려도 목표 시각을 넘지 않는다`() {
        val target = millis(2026, 10, 23, 7, 30)
        var now = target - TimeUnit.HOURS.toMillis(24)
        var hops = 0
        while (true) {
            val trigger = AlarmPlan.nextTrigger(now, target)
            if (trigger == target) break
            val latest = trigger + ((trigger - now) * 0.75).toLong()
            assertTrue("hop $hops 이 목표를 넘음", latest <= target)
            // 최악의 경우(가장 일찍 울림)로 남은 시간을 줄여 본다
            now = trigger
            hops++
        }
        // 마지막 알람은 5분 이내에서 걸리므로 늦어도 3분 45초
        assertTrue(target - now <= AlarmPlan.FINAL_HOP_MILLIS)
        assertTrue("하루 중간 알람이 너무 많음: $hops", hops <= 10)
    }

    @Test
    fun `30초 일찍 깨어난 것은 제시간으로 본다`() {
        val target = millis(2026, 10, 22, 7, 30)
        assertTrue(AlarmPlan.isDue(target - 20_000, target))
        assertFalse(AlarmPlan.isDue(target - 60_000, target))
    }

    @Test
    fun `두 시간 넘게 늦으면 건너뛴다`() {
        val target = millis(2026, 10, 22, 7, 30)
        assertFalse(AlarmPlan.isStale(target + TimeUnit.MINUTES.toMillis(30), target))
        assertTrue(AlarmPlan.isStale(target + TimeUnit.HOURS.toMillis(3), target))
    }
}
