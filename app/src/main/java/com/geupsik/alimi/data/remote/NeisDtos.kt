package com.geupsik.alimi.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** schoolInfo(학교기본정보)의 row 한 줄. 쓰는 필드만 받는다. */
@Serializable
data class SchoolRow(
    @SerialName("ATPT_OFCDC_SC_CODE") val officeCode: String,
    @SerialName("ATPT_OFCDC_SC_NM") val officeName: String? = null,
    @SerialName("SD_SCHUL_CODE") val schoolCode: String,
    @SerialName("SCHUL_NM") val name: String,
    @SerialName("SCHUL_KND_SC_NM") val kind: String? = null,
    @SerialName("ORG_RDNMA") val roadAddress: String? = null,
)

/** mealServiceDietInfo(급식식단정보)의 row 한 줄 = 끼니 하나 */
@Serializable
data class MealRow(
    @SerialName("MLSV_YMD") val date: String,
    @SerialName("MMEAL_SC_CODE") val mealCode: String,
    @SerialName("MMEAL_SC_NM") val mealName: String? = null,
    @SerialName("DDISH_NM") val dishes: String? = null,
    @SerialName("CAL_INFO") val calories: String? = null,
)
