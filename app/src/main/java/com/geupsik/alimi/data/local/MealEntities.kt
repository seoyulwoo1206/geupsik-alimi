package com.geupsik.alimi.data.local

import androidx.room.Entity

/**
 * 끼니 하나(예: 10월 22일 중식). 메뉴는 나이스 원문 그대로 저장하고,
 * 화면에 보여줄 때 MenuParser로 알레르기 번호를 떼어 낸다.
 * 그래서 파서를 고치면 이미 저장된 메뉴에도 바로 반영된다.
 */
@Entity(tableName = "meal", primaryKeys = ["schoolKey", "date", "mealCode"])
data class MealEntity(
    /** 교육청코드-학교코드. 학교를 바꿔도 다른 학교 캐시와 섞이지 않게 한다. */
    val schoolKey: String,
    /** yyyyMMdd */
    val date: String,
    /** 1 조식, 2 중식, 3 석식 */
    val mealCode: Int,
    val mealName: String,
    val dishesRaw: String,
    val calories: String?,
)

/**
 * 어느 주를 언제 받아 왔는지 기록한다.
 * 기록이 있는데 그날 끼니가 없으면 "급식 없는 날", 기록이 없으면 "아직 못 받아 온 날"로 구분한다.
 */
@Entity(tableName = "week_fetch", primaryKeys = ["schoolKey", "weekStart"])
data class WeekFetchEntity(
    val schoolKey: String,
    /** 그 주 월요일, yyyyMMdd */
    val weekStart: String,
    val fetchedAtMillis: Long,
)
