package com.example.appenergytracker.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "GoodHabitApp")
data class GoodHabitApp(
    @PrimaryKey val packageName: String, // 主鍵為 packageName，保證唯一性
    val name: String,
    val ratio: Float,
    val isGoodHabit: Boolean,
    val isBadHabit: Boolean
)
