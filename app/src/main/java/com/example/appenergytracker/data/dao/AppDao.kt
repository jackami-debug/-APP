package com.example.appenergytracker.data.dao

import androidx.room.*
import com.example.appenergytracker.data.entity.AppCategory
import com.example.appenergytracker.data.entity.AppUsageLog
import com.example.appenergytracker.data.entity.ChargeLog
import kotlinx.coroutines.flow.Flow

@Dao
interface AppDao {

    // ----------------------------
    // AppCategory（分類表）
    // ----------------------------

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertApp(app: AppCategory)

    @Delete
    suspend fun deleteApp(app: AppCategory)

    @Query("SELECT * FROM app_category ORDER BY appName ASC")
    fun getAllAppCategories(): Flow<List<AppCategory>>

    @Query("SELECT * FROM app_category WHERE type = :type")
    fun getAppsByType(type: String): Flow<List<AppCategory>>

    @Query("SELECT * FROM app_category WHERE packageName = :packageName")
    suspend fun getCategoryByPackage(packageName: String): AppCategory?

    // ----------------------------
    // AppUsageLog（使用紀錄）
    // ----------------------------

    @Insert
    suspend fun insertUsageLog(log: AppUsageLog)

    @Insert
    suspend fun insertUsageLogs(logs: List<AppUsageLog>)

    @Query("SELECT * FROM app_usage_log WHERE usageDate = :date ORDER BY usageMinutes DESC")
    fun getUsageLogsByDate(date: String): Flow<List<AppUsageLog>>

    @Query("DELETE FROM app_usage_log")
    suspend fun clearAllUsageLogs()

    @Query("DELETE FROM app_usage_log WHERE usageDate = :date")
    suspend fun clearUsageLogsByDate(date: String)

    // ----------------------------
    // ChargeLog（充能紀錄）
    // ----------------------------

    @Insert
    suspend fun insertChargeLog(log: ChargeLog)

    @Query("SELECT * FROM charge_log WHERE chargeDate = :date")
    fun getChargeLogsByDate(date: String): Flow<List<ChargeLog>>

    @Query("DELETE FROM charge_log")
    suspend fun clearAllChargeLogs()
}
