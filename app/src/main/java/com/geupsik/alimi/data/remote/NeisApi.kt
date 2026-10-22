package com.geupsik.alimi.data.remote

import kotlinx.serialization.json.JsonObject
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * 나이스 교육정보 개방 포털 API.
 * 응답 구조가 서비스마다 조금씩 달라서 JsonObject로 받고 [NeisParser]에서 풀어낸다.
 * key가 null이면 KEY 파라미터가 빠져서 샘플 모드(최대 5건)로 동작한다.
 */
interface NeisApi {

    @GET("hub/schoolInfo")
    suspend fun schoolInfo(
        @Query("KEY") key: String?,
        @Query("SCHUL_NM") schoolName: String,
        @Query("pIndex") pageIndex: Int = 1,
        @Query("pSize") pageSize: Int = 100,
        @Query("Type") type: String = "json",
    ): JsonObject

    /** 날짜는 yyyyMMdd. 하루에 조식/중식/석식이 따로 한 줄씩 온다. */
    @GET("hub/mealServiceDietInfo")
    suspend fun mealServiceDietInfo(
        @Query("KEY") key: String?,
        @Query("ATPT_OFCDC_SC_CODE") officeCode: String,
        @Query("SD_SCHUL_CODE") schoolCode: String,
        @Query("MLSV_FROM_YMD") fromDate: String,
        @Query("MLSV_TO_YMD") toDate: String,
        @Query("pIndex") pageIndex: Int = 1,
        @Query("pSize") pageSize: Int = 100,
        @Query("Type") type: String = "json",
    ): JsonObject

    companion object {
        const val BASE_URL = "https://open.neis.go.kr/"
    }
}
