package com.example.appenergytracker.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "app_usage_log")
data class AppUsageLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val packageName: String,
    val appName: String,
    val usageDate: String,   // 格式建議 yyyy-MM-dd
    val usageMinutes: Int
)
