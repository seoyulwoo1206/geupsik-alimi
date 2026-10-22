package com.geupsik.alimi.update

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.Path

/** 깃허브 공개 API. 로그인 없이 IP당 시간당 60번까지 부를 수 있다. */
interface GitHubApi {

    /** 정식 릴리스 중 가장 최근 것 (draft·pre-release 제외) */
    @Headers("Accept: application/vnd.github+json")
    @GET("repos/{owner}/{repo}/releases/latest")
    suspend fun latestRelease(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
    ): GitHubRelease

    companion object {
        const val BASE_URL = "https://api.github.com/"
    }
}

@Serializable
data class GitHubRelease(
    @SerialName("tag_name") val tagName: String,
    @SerialName("name") val name: String? = null,
    @SerialName("body") val body: String? = null,
    @SerialName("html_url") val htmlUrl: String,
    @SerialName("assets") val assets: List<GitHubAsset> = emptyList(),
)

@Serializable
data class GitHubAsset(
    @SerialName("name") val name: String,
    @SerialName("browser_download_url") val downloadUrl: String,
)
