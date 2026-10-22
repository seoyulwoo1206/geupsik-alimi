package com.geupsik.alimi.data.prefs

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.geupsik.alimi.data.model.School
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json

private val Context.dataStore by preferencesDataStore(name = "user_prefs")

/** 학교·알레르기·알림 설정처럼 작은 사용자 설정을 저장한다. */
class UserPrefs(context: Context, private val json: Json) {

    private val store = context.applicationContext.dataStore

    val school: Flow<School?> = store.data.map { prefs ->
        prefs[KEY_SCHOOL]?.let { runCatching { json.decodeFromString<School>(it) }.getOrNull() }
    }

    suspend fun setSchool(school: School) {
        store.edit { it[KEY_SCHOOL] = json.encodeToString(school) }
    }

    /** 사용자가 고른 나이스 알레르기 번호(1~19) */
    val allergies: Flow<Set<Int>> = store.data.map { prefs ->
        prefs[KEY_ALLERGIES].orEmpty().mapNotNull { it.toIntOrNull() }.toSet()
    }

    /** 알레르기 안내 팝업에서 "확인했어요"를 누른 적이 있는지 */
    val allergyNoticeAccepted: Flow<Boolean> = store.data.map { it[KEY_ALLERGY_NOTICE] ?: false }

    /** 안내를 확인했다는 기록과 알레르기 목록을 한 번에 저장한다. */
    suspend fun setAllergies(numbers: Set<Int>, noticeAccepted: Boolean) {
        store.edit {
            it[KEY_ALLERGIES] = numbers.map(Int::toString).toSet()
            if (noticeAccepted) it[KEY_ALLERGY_NOTICE] = true
        }
    }

    val notification: Flow<NotificationSettings> = store.data.map {
        NotificationSettings(
            enabled = it[KEY_NOTIFY_ENABLED] ?: false,
            minuteOfDay = it[KEY_NOTIFY_MINUTE] ?: NotificationSettings.DEFAULT_MINUTE_OF_DAY,
        )
    }

    suspend fun setNotificationEnabled(enabled: Boolean) {
        store.edit { it[KEY_NOTIFY_ENABLED] = enabled }
    }

    suspend fun setNotificationTime(minuteOfDay: Int) {
        store.edit { it[KEY_NOTIFY_MINUTE] = minuteOfDay }
    }

    /** 마지막으로 알림을 보낸 목표 시각. 같은 시각 알림이 두 번 가지 않게 한다. */
    val lastNotifiedTarget: Flow<Long> = store.data.map { it[KEY_LAST_NOTIFIED] ?: 0L }

    suspend fun setLastNotifiedTarget(targetMillis: Long) {
        store.edit { it[KEY_LAST_NOTIFIED] = targetMillis }
    }

    /** 업데이트 확인 기록. 깃허브 호출 횟수를 아끼고, "나중에"를 누른 버전을 한동안 다시 묻지 않는 데 쓴다. */
    val updateState: Flow<UpdateCheckState> = store.data.map {
        UpdateCheckState(
            lastCheckMillis = it[KEY_UPDATE_LAST_CHECK] ?: 0L,
            cachedReleaseJson = it[KEY_UPDATE_CACHED],
            dismissedTag = it[KEY_UPDATE_DISMISSED_TAG],
            dismissedAtMillis = it[KEY_UPDATE_DISMISSED_AT] ?: 0L,
        )
    }

    suspend fun saveUpdateCheck(checkedAtMillis: Long, releaseJson: String?) {
        store.edit {
            it[KEY_UPDATE_LAST_CHECK] = checkedAtMillis
            if (releaseJson == null) it.remove(KEY_UPDATE_CACHED) else it[KEY_UPDATE_CACHED] = releaseJson
        }
    }

    suspend fun dismissUpdate(tag: String, atMillis: Long) {
        store.edit {
            it[KEY_UPDATE_DISMISSED_TAG] = tag
            it[KEY_UPDATE_DISMISSED_AT] = atMillis
        }
    }

    private companion object {
        val KEY_UPDATE_LAST_CHECK = longPreferencesKey("update_last_check")
        val KEY_UPDATE_CACHED = stringPreferencesKey("update_cached_release")
        val KEY_UPDATE_DISMISSED_TAG = stringPreferencesKey("update_dismissed_tag")
        val KEY_UPDATE_DISMISSED_AT = longPreferencesKey("update_dismissed_at")
        val KEY_SCHOOL = stringPreferencesKey("school")
        val KEY_ALLERGIES = stringSetPreferencesKey("allergies")
        val KEY_ALLERGY_NOTICE = booleanPreferencesKey("allergy_notice_accepted")
        val KEY_NOTIFY_ENABLED = booleanPreferencesKey("notify_enabled")
        val KEY_NOTIFY_MINUTE = intPreferencesKey("notify_minute_of_day")
        val KEY_LAST_NOTIFIED = longPreferencesKey("last_notified_target")
    }
}

data class UpdateCheckState(
    val lastCheckMillis: Long,
    val cachedReleaseJson: String?,
    val dismissedTag: String?,
    val dismissedAtMillis: Long,
)

/** 매일 급식 알림 설정. minuteOfDay는 0시부터 센 분 (7:30 → 450) */
data class NotificationSettings(val enabled: Boolean, val minuteOfDay: Int) {
    val hour: Int get() = minuteOfDay / 60
    val minute: Int get() = minuteOfDay % 60

    companion object {
        const val DEFAULT_MINUTE_OF_DAY = 7 * 60 + 30
    }
}
