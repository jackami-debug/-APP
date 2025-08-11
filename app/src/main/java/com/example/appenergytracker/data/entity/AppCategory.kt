package com.example.appenergytracker.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "app_category")
data class AppCategory(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val appName: String,
    val packageName: String,
    val type: String, // "good" or "bad"
    val convertRatio: Float = 1f // 僅 good 類型會用到
)

