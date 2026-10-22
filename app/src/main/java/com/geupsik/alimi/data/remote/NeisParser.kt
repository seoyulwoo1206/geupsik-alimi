package com.geupsik.alimi.data.remote

import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/** 나이스 응답 코드. https://open.neis.go.kr 의 "메시지 설명" 참고 */
object NeisCode {
    const val OK = "INFO-000"
    const val NO_DATA = "INFO-200"
}

/** 나이스가 정상(INFO-000)이나 데이터 없음(INFO-200)이 아닌 코드를 돌려줬을 때 */
class NeisApiException(val code: String, override val message: String) : Exception(message)

data class NeisPage<T>(val rows: List<T>, val totalCount: Int)

/**
 * 나이스 응답을 풀어낸다.
 *
 * 정상:   {"schoolInfo":[{"head":[{"list_total_count":9},{"RESULT":{...}}]},{"row":[...]}]}
 * 그 외:  {"RESULT":{"CODE":"INFO-200","MESSAGE":"해당하는 데이터가 없습니다."}}
 */
object NeisParser {

    fun <T> parse(
        root: JsonObject,
        service: String,
        rowSerializer: KSerializer<T>,
        json: Json,
    ): NeisPage<T> {
        root["RESULT"]?.let { result ->
            val (code, message) = readResult(result.jsonObject)
            if (code == NeisCode.NO_DATA) return NeisPage(emptyList(), 0)
            throw NeisApiException(code, message)
        }

        val sections = root[service]?.jsonArray
            ?: throw NeisApiException("UNKNOWN", "응답에 $service 항목이 없습니다.")

        var totalCount = 0
        var rows: JsonArray? = null
        for (section in sections) {
            val obj = section.jsonObject
            obj["head"]?.jsonArray?.forEach { headItem ->
                val head = headItem.jsonObject
                head["list_total_count"]?.let { totalCount = it.jsonPrimitive.int }
                head["RESULT"]?.let {
                    val (code, message) = readResult(it.jsonObject)
                    if (code != NeisCode.OK) throw NeisApiException(code, message)
                }
            }
            obj["row"]?.let { rows = it.jsonArray }
        }

        val parsed = rows.orEmpty().map { json.decodeFromJsonElement(rowSerializer, it) }
        return NeisPage(parsed, maxOf(totalCount, parsed.size))
    }

    private fun readResult(result: JsonObject): Pair<String, String> {
        val code = result["CODE"]?.jsonPrimitive?.content ?: "UNKNOWN"
        val message = result["MESSAGE"]?.jsonPrimitive?.content.orEmpty()
        return code to message
    }
}
