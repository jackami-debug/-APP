package com.example.appenergytracker.data.dao

import android.util.Log
import androidx.room.*
import com.example.appenergytracker.model.GoodHabitApp
import kotlinx.coroutines.flow.Flow

@Dao
interface GoodHabitDao {

    @Query("SELECT * FROM GoodHabitApp ORDER BY name")
    fun getAllGoodHabitApps(): Flow<List<GoodHabitApp>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(apps: List<GoodHabitApp>)

    @Update
    suspend fun updateApp(app: GoodHabitApp)

    @Query("DELETE FROM GoodHabitApp")
    suspend fun clearAllInternal()

    // ✅ 包一層呼叫 + Log 用的函式
    suspend fun clearAll() {
        Log.d("GoodHabitDao", "清除資料中...")
        clearAllInternal()
        Log.d("GoodHabitDao", "GoodHabitDao: 所有資料已清除")
    }
}
