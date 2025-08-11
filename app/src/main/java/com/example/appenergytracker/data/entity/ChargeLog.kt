package com.example.appenergytracker.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "charge_log")
data class ChargeLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val activityType: String,     // 例如：讀書、運動
    val chargeDate: String,       // 格式 yyyy-MM-dd
    val durationMinutes: Int,
    val ratio: Float              // 例如 1:2 → ratio = 2.0f
)
