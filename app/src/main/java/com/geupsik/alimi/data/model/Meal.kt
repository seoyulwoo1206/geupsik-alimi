package com.geupsik.alimi.data.model

import com.geupsik.alimi.domain.Dish

/** 화면과 알림에서 쓰는 끼니 하나 */
data class Meal(
    /** 1 조식, 2 중식, 3 석식 */
    val code: Int,
    val name: String,
    val dishes: List<Dish>,
    /** 예: "831.5 Kcal" */
    val calories: String?,
)

/** 하루치 급식 캐시 상태 */
data class DayMeals(
    val meals: List<Meal>,
    /** 그 주를 한 번이라도 받아 왔는지. false면 "아직 모름", true인데 meals가 비면 "급식 없음" */
    val weekFetched: Boolean,
)
