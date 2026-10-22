package com.geupsik.alimi.domain

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters

/** 나이스가 쓰는 yyyyMMdd 날짜와 주 단위 계산 */
object MealDates {

    private val YMD: DateTimeFormatter = DateTimeFormatter.BASIC_ISO_DATE

    fun toYmd(date: LocalDate): String = date.format(YMD)

    fun weekStart(date: LocalDate): LocalDate =
        date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

    fun weekEnd(date: LocalDate): LocalDate = weekStart(date).plusDays(6)
}
