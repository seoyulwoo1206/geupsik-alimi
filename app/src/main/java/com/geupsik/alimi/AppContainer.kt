package com.geupsik.alimi

import android.content.Context
import com.geupsik.alimi.data.local.AppDatabase
import com.geupsik.alimi.data.prefs.UserPrefs
import com.geupsik.alimi.data.remote.NeisApi
import com.geupsik.alimi.data.repository.MealRepository
import com.geupsik.alimi.data.repository.SchoolRepository
import com.geupsik.alimi.notification.DailyMealNotification
import com.geupsik.alimi.notification.MealAlarmScheduler
import com.geupsik.alimi.notification.MealNotifier
import com.geupsik.alimi.update.GitHubApi
import com.geupsik.alimi.update.UpdateChecker
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit

/** 앱 전체에서 하나씩만 쓰는 객체들을 만들어 둔다 (간단한 수동 DI). */
class AppContainer(context: Context) {

    val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    private val okHttp = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private val neisApi: NeisApi = Retrofit.Builder()
        .baseUrl(NeisApi.BASE_URL)
        .client(okHttp)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()
        .create(NeisApi::class.java)

    val userPrefs = UserPrefs(context, json)

    val schoolRepository = SchoolRepository(neisApi, userPrefs, json, BuildConfig.NEIS_API_KEY)

    private val database = AppDatabase.create(context)

    val mealRepository = MealRepository(neisApi, database.mealDao(), json, BuildConfig.NEIS_API_KEY)

    val mealNotifier = MealNotifier(context)

    val mealAlarmScheduler = MealAlarmScheduler(context)

    val dailyMealNotification = DailyMealNotification(userPrefs, mealRepository, mealNotifier)

    private val gitHubApi: GitHubApi = Retrofit.Builder()
        .baseUrl(GitHubApi.BASE_URL)
        .client(okHttp)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()
        .create(GitHubApi::class.java)

    val updateChecker = UpdateChecker(
        api = gitHubApi,
        prefs = userPrefs,
        json = json,
        repo = BuildConfig.GITHUB_REPO,
        enabled = BuildConfig.UPDATE_CHECK_ENABLED,
        currentVersion = BuildConfig.VERSION_NAME,
    )
}
