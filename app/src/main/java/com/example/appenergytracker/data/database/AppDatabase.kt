package com.example.appenergytracker.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.appenergytracker.data.dao.AppDao
import com.example.appenergytracker.data.dao.GoodHabitDao
import com.example.appenergytracker.data.entity.AppCategory
import com.example.appenergytracker.data.entity.AppUsageLog
import com.example.appenergytracker.data.entity.ChargeLog
import com.example.appenergytracker.model.GoodHabitApp

@Database(
    entities = [
        AppCategory::class,
        AppUsageLog::class,
        ChargeLog::class,
        GoodHabitApp::class  // ✅ 加入 GoodHabitApp
    ],
    version = 4,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun appDao(): AppDao
    abstract fun goodHabitDao(): GoodHabitDao  // ✅ 加入 GoodHabitDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "app_database"
                ).fallbackToDestructiveMigration() // ← 若資料結構改變不報錯直接重建資料表
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
