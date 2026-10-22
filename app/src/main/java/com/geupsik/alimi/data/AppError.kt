package com.geupsik.alimi.data

import com.geupsik.alimi.data.remote.NeisApiException
import kotlinx.serialization.SerializationException
import retrofit2.HttpException
import java.io.IOException
import java.net.SocketTimeoutException

/** 화면에 보여줄 수 있는 오류. 모든 네트워크/API 예외는 여기로 바꿔서 앱이 꺼지지 않게 한다. */
sealed class AppError(val userMessage: String) {
    data object Network : AppError("인터넷에 연결되어 있지 않아요. 연결을 확인하고 다시 시도해 주세요.")
    data object Timeout : AppError("나이스 서버 응답이 늦어요. 잠시 후 다시 시도해 주세요.")
    data object InvalidKey : AppError("나이스 인증키에 문제가 있어요. 앱 만든 사람에게 알려 주세요.")
    data object RateLimited : AppError("오늘 나이스 조회 한도를 넘었어요. 내일 다시 시도해 주세요.")
    data class Server(val code: String) :
        AppError("나이스 서버에 문제가 있어요. 잠시 후 다시 시도해 주세요. ($code)")
    data object Unknown : AppError("알 수 없는 오류가 났어요. 잠시 후 다시 시도해 주세요.")
}

fun Throwable.toAppError(): AppError = when (this) {
    is NeisApiException -> when (code) {
        // ERROR-290: 인증키 오류, INFO-300: 관리자가 인증키 사용을 제한함
        "ERROR-290", "INFO-300" -> AppError.InvalidKey
        // ERROR-337: 일별 트래픽 제한 초과
        "ERROR-337" -> AppError.RateLimited
        else -> AppError.Server(code)
    }
    is SocketTimeoutException -> AppError.Timeout
    is IOException -> AppError.Network
    is HttpException -> AppError.Server("HTTP ${code()}")
    is SerializationException, is IllegalArgumentException -> AppError.Server("응답 형식")
    else -> AppError.Unknown
}
