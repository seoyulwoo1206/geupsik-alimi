package com.geupsik.alimi.data.repository

import com.geupsik.alimi.data.model.School
import com.geupsik.alimi.data.prefs.UserPrefs
import com.geupsik.alimi.data.remote.NeisApi
import com.geupsik.alimi.data.remote.NeisParser
import com.geupsik.alimi.data.remote.SchoolRow
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.json.Json

data class SchoolSearchResult(val schools: List<School>, val totalCount: Int)

class SchoolRepository(
    private val api: NeisApi,
    private val prefs: UserPrefs,
    private val json: Json,
    private val apiKey: String,
) {
    /** 인증키가 없으면 나이스가 결과를 최대 5개까지만 준다. */
    val isSampleMode: Boolean get() = apiKey.isBlank()

    val selectedSchool: Flow<School?> = prefs.school

    /** 실패하면 예외를 던진다. 호출하는 쪽에서 toAppError()로 바꿔서 보여준다. */
    suspend fun search(name: String): SchoolSearchResult {
        val response = api.schoolInfo(key = apiKey.ifBlank { null }, schoolName = name)
        val page = NeisParser.parse(response, "schoolInfo", SchoolRow.serializer(), json)
        // 나이스에는 학교 코드가 빈칸인 시험용 항목(예: AIDT고등학교)이 섞여 있다.
        // 이런 항목은 급식을 조회할 수 없으니 빼고, 혹시 겹치는 학교도 한 번만 보여준다.
        val schools = page.rows
            .map { it.toSchool() }
            .filter { it.officeCode.isNotEmpty() && it.schoolCode.isNotEmpty() }
            .distinctBy { it.officeCode to it.schoolCode }
        val removed = page.rows.size - schools.size
        return SchoolSearchResult(schools, page.totalCount - removed)
    }

    suspend fun select(school: School) = prefs.setSchool(school)

    private fun SchoolRow.toSchool() = School(
        officeCode = officeCode.trim(),
        schoolCode = schoolCode.trim(),
        name = name.trim(),
        kind = kind.orEmpty(),
        address = roadAddress?.trim().orEmpty(),
        officeName = officeName.orEmpty(),
    )
}
