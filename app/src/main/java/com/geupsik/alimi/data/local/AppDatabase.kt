package com.geupsik.alimi.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [MealEntity::class, WeekFetchEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun mealDao(): MealDao

    companion object {
        fun create(context: Context): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, "geupsik.db")
                // 캐시일 뿐이라 구조가 바뀌어 마이그레이션이 없으면 지우고 다시 받는다
                .fallbackToDestructiveMigration(dropAllTables = true)
                .build()
    }
}
