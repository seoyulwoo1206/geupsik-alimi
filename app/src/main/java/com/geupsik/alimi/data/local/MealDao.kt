package com.geupsik.alimi.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface MealDao {

    @Query("SELECT * FROM meal WHERE schoolKey = :schoolKey AND date = :date ORDER BY mealCode")
    fun observeDay(schoolKey: String, date: String): Flow<List<MealEntity>>

    @Query("SELECT * FROM meal WHERE schoolKey = :schoolKey AND date = :date ORDER BY mealCode")
    suspend fun getDay(schoolKey: String, date: String): List<MealEntity>

    @Query("SELECT * FROM week_fetch WHERE schoolKey = :schoolKey AND weekStart = :weekStart")
    fun observeWeekFetch(schoolKey: String, weekStart: String): Flow<WeekFetchEntity?>

    @Query("SELECT * FROM week_fetch WHERE schoolKey = :schoolKey AND weekStart = :weekStart")
    suspend fun getWeekFetch(schoolKey: String, weekStart: String): WeekFetchEntity?

    @Query("DELETE FROM meal WHERE schoolKey = :schoolKey AND date BETWEEN :from AND :to")
    suspend fun deleteRange(schoolKey: String, from: String, to: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMeals(meals: List<MealEntity>)

    @Upsert
    suspend fun upsertWeekFetch(fetch: WeekFetchEntity)

    /** 한 주치를 통째로 바꾼다. 중간에 실패하면 이전 캐시가 그대로 남는다. */
    @Transaction
    suspend fun replaceWeek(
        schoolKey: String,
        from: String,
        to: String,
        meals: List<MealEntity>,
        fetch: WeekFetchEntity,
    ) {
        deleteRange(schoolKey, from, to)
        insertMeals(meals)
        upsertWeekFetch(fetch)
    }

    /** 오래된 캐시를 정리한다 (yyyyMMdd 문자열은 사전순 = 날짜순). */
    @Query("DELETE FROM meal WHERE date < :before")
    suspend fun deleteMealsBefore(before: String)

    @Query("DELETE FROM week_fetch WHERE weekStart < :before")
    suspend fun deleteWeekFetchesBefore(before: String)
}
