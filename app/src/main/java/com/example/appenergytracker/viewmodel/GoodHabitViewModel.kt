package com.example.appenergytracker.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.appenergytracker.data.database.AppDatabase
import com.example.appenergytracker.model.GoodHabitApp
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.ui.graphics.ImageBitmap


class GoodHabitViewModel(application: Application) : AndroidViewModel(application) {
    private val goodHabitDao = AppDatabase.getDatabase(application).goodHabitDao()  // ✅ 使用正確名稱

    val goodHabitApps: StateFlow<List<GoodHabitApp>> =
        goodHabitDao.getAllGoodHabitApps().stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun insertAll(apps: List<GoodHabitApp>) {
        viewModelScope.launch {
            goodHabitDao.insertAll(apps)
        }
    }

    fun updateApp(app: GoodHabitApp) {
        viewModelScope.launch {
            goodHabitDao.updateApp(app)
        }
    }

    fun clearAllApps() {
        viewModelScope.launch {
            goodHabitDao.clearAll()  // ✅ 清空資料庫
        }
    }
    val appIcons = mutableStateMapOf<String, ImageBitmap?>()
}
