package com.geupsik.alimi.data.model

import kotlinx.serialization.Serializable

/** 사용자가 고른 학교. 급식 조회에는 교육청 코드 + 학교 코드가 필요하다. */
@Serializable
data class School(
    val officeCode: String,
    val schoolCode: String,
    val name: String,
    val kind: String,
    val address: String,
    val officeName: String,
)
