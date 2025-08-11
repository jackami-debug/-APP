package com.example.appenergytracker.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.appenergytracker.data.database.AppDatabase
import com.example.appenergytracker.data.entity.ChargeLog
import com.example.appenergytracker.util.DateUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.math.min

class EnergyViewModel(application: Application) : AndroidViewModel(application) {
    private val appDao = AppDatabase.getDatabase(application).appDao()

    private val _currentEnergyMinutes = MutableStateFlow(0)
    val currentEnergyMinutes: StateFlow<Int> = _currentEnergyMinutes.asStateFlow()

    private val _maxEnergyMinutes = MutableStateFlow(180)
    val maxEnergyMinutes: StateFlow<Int> = _maxEnergyMinutes.asStateFlow()

    init {
        // 監聽今日的使用與充能紀錄，動態計算能量
        val today = DateUtils.today()
        val goodHabitDao = AppDatabase.getDatabase(getApplication()).goodHabitDao()
        combine(
            appDao.getUsageLogsByDate(today),
            appDao.getChargeLogsByDate(today),
            goodHabitDao.getAllGoodHabitApps()
        ) { usageLogs, chargeLogs, habitApps ->
            val packageToHabit = habitApps.associateBy { it.packageName }

            val usageDelta = usageLogs.sumOf { log ->
                val cfg = packageToHabit[log.packageName]
                when {
                    cfg == null -> 0 // 未設定者不影響能量
                    cfg.isGoodHabit -> (log.usageMinutes * cfg.ratio).toInt()
                    cfg.isBadHabit -> -log.usageMinutes // 只有被標記成壞習慣才扣除
                    else -> 0
                }
            }

            val chargeDelta = chargeLogs.sumOf { log ->
                (log.durationMinutes * log.ratio).toInt()
            }

            val rawEnergy = usageDelta + chargeDelta
            min(_maxEnergyMinutes.value, max(0, rawEnergy))
        }.onEach { energy ->
            _currentEnergyMinutes.value = energy
        }.launchIn(viewModelScope)
    }

    fun setMaxEnergyMinutes(maxMinutes: Int) {
        _maxEnergyMinutes.value = max(0, maxMinutes)
    }

    fun insertCharge(activityType: String, durationMinutes: Int, ratio: Float) {
        val log = ChargeLog(
            activityType = activityType,
            chargeDate = DateUtils.today(),
            durationMinutes = durationMinutes,
            ratio = ratio
        )
        viewModelScope.launch {
            appDao.insertChargeLog(log)
        }
    }
}


