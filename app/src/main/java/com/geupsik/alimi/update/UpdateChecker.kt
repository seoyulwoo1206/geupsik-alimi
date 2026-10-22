package com.geupsik.alimi.update

import com.geupsik.alimi.data.prefs.UserPrefs
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json
import retrofit2.HttpException
import java.util.concurrent.TimeUnit

/** 새 버전 안내에 필요한 정보 */
data class UpdateInfo(
    /** 릴리스 태그 그대로 (예: v0.2.0) */
    val tag: String,
    val currentVersion: String,
    val notes: String?,
    /** APK 파일이 올라가 있으면 그 주소, 없으면 릴리스 페이지 */
    val downloadUrl: String,
)

sealed interface ManualCheckResult {
    data class UpdateAvailable(val info: UpdateInfo) : ManualCheckResult
    data object UpToDate : ManualCheckResult
    /** 저장소가 없거나 아직 릴리스가 없음 */
    data object NoRelease : ManualCheckResult
    data object Failed : ManualCheckResult
}

/**
 * 깃허브 최신 릴리스 버전을 지금 앱 버전과 비교한다.
 * 오류가 나도 앱 사용에는 지장이 없게, 자동 확인은 실패하면 조용히 넘어간다.
 */
class UpdateChecker(
    private val api: GitHubApi,
    private val prefs: UserPrefs,
    private val json: Json,
    private val repo: String,
    private val enabled: Boolean,
    private val currentVersion: String,
    private val now: () -> Long = System::currentTimeMillis,
) {
    /** play 빌드이거나 저장소를 아직 안 정했으면 false */
    val isAvailable: Boolean get() = enabled && ownerAndRepo() != null

    /**
     * 앱을 켤 때 부른다. 같은 학교 친구들이 같은 와이파이(같은 IP)를 쓰면 깃허브 호출 한도(시간당 60번)에
     * 걸릴 수 있어서, 6시간 안에 이미 확인했으면 저장해 둔 결과를 쓴다.
     * "나중에"를 누른 버전은 3일 동안 다시 묻지 않는다.
     */
    suspend fun checkOnLaunch(): UpdateInfo? {
        if (!isAvailable) return null
        val state = prefs.updateState.first()

        val release = if (now() - state.lastCheckMillis < RECHECK_AFTER_MILLIS) {
            state.cachedReleaseJson?.let { runCatching { json.decodeFromString<GitHubRelease>(it) }.getOrNull() }
        } else {
            fetchAndCache() ?: return null
        }
        val info = release?.toUpdateInfoIfNewer() ?: return null

        val recentlyDismissed = state.dismissedTag == info.tag &&
            now() - state.dismissedAtMillis < SNOOZE_MILLIS
        return if (recentlyDismissed) null else info
    }

    /** 설정에서 직접 확인할 때. 기록과 상관없이 바로 물어본다. */
    suspend fun checkNow(): ManualCheckResult {
        if (!isAvailable) return ManualCheckResult.NoRelease
        val (owner, name) = ownerAndRepo()!!
        return try {
            val release = api.latestRelease(owner, name)
            prefs.saveUpdateCheck(now(), json.encodeToString(release))
            release.toUpdateInfoIfNewer()
                ?.let { ManualCheckResult.UpdateAvailable(it) }
                ?: ManualCheckResult.UpToDate
        } catch (e: CancellationException) {
            throw e
        } catch (e: HttpException) {
            if (e.code() == 404) ManualCheckResult.NoRelease else ManualCheckResult.Failed
        } catch (_: Exception) {
            ManualCheckResult.Failed
        }
    }

    suspend fun dismiss(info: UpdateInfo) = prefs.dismissUpdate(info.tag, now())

    /** 실패하면 null. 릴리스가 아직 없으면(404) 그 사실을 기록하고 null. */
    private suspend fun fetchAndCache(): GitHubRelease? {
        val (owner, name) = ownerAndRepo() ?: return null
        return try {
            api.latestRelease(owner, name).also {
                prefs.saveUpdateCheck(now(), json.encodeToString(it))
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: HttpException) {
            if (e.code() == 404) prefs.saveUpdateCheck(now(), null)
            null
        } catch (_: Exception) {
            null
        }
    }

    private fun GitHubRelease.toUpdateInfoIfNewer(): UpdateInfo? {
        val latest = AppVersion.parse(tagName) ?: return null
        val current = AppVersion.parse(currentVersion) ?: return null
        if (latest <= current) return null
        val apk = assets.firstOrNull { it.name.endsWith(".apk", ignoreCase = true) }
        return UpdateInfo(
            tag = tagName,
            currentVersion = currentVersion,
            notes = body?.trim()?.takeIf { it.isNotEmpty() }?.take(MAX_NOTES_LENGTH),
            downloadUrl = apk?.downloadUrl ?: htmlUrl,
        )
    }

    private fun ownerAndRepo(): Pair<String, String>? {
        val parts = repo.split('/').map { it.trim() }
        if (parts.size != 2 || parts.any { it.isEmpty() }) return null
        return parts[0] to parts[1]
    }

    private companion object {
        val RECHECK_AFTER_MILLIS = TimeUnit.HOURS.toMillis(6)
        val SNOOZE_MILLIS = TimeUnit.DAYS.toMillis(3)
        const val MAX_NOTES_LENGTH = 600
    }
}
