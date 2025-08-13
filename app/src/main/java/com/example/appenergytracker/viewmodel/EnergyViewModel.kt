package com.example.appenergytracker.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.appenergytracker.data.database.AppDatabase
import com.example.appenergytracker.data.entity.ChargeLog
import com.example.appenergytracker.service.EnergyMonitorService
import com.example.appenergytracker.service.AppLockService
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
    private val appLockService = AppLockService.getInstance(application)
    // 使用 AppLockService 的 EnergyMonitorService 實例
    private val energyMonitorService = appLockService.getEnergyMonitorService()

    private val _currentEnergyMinutes = MutableStateFlow(0)
    val currentEnergyMinutes: StateFlow<Int> = _currentEnergyMinutes.asStateFlow()

    private val _maxEnergyMinutes = MutableStateFlow(180)
    val maxEnergyMinutes: StateFlow<Int> = _maxEnergyMinutes.asStateFlow()

    // 常數定義
    companion object {
        private const val DEFAULT_MAX_ENERGY = 180
        private const val MIN_ENERGY = 0
    }

    init {
        initializeEnergyMonitoring()
    }
    
    private fun initializeEnergyMonitoring() {
        // 監聽自動監控服務的能量變化（主要資料來源）
        energyMonitorService.currentEnergy.onEach { energy ->
            _currentEnergyMinutes.value = energy
        }.launchIn(viewModelScope)
        
        // 備用資料庫監聽（當自動監控服務未運行時使用）
        setupDatabaseBackupMonitoring()
    }
    
    private fun setupDatabaseBackupMonitoring() {
        val today = DateUtils.today()
        val goodHabitDao = AppDatabase.getDatabase(getApplication()).goodHabitDao()
        
        combine(
            appDao.getUsageLogsByDate(today),
            appDao.getChargeLogsByDate(today),
            goodHabitDao.getAllGoodHabitApps()
        ) { usageLogs, chargeLogs, habitApps ->
            calculateEnergyFromDatabase(usageLogs, chargeLogs, habitApps)
        }.onEach { calculatedEnergy ->
            // 當自動監控服務未運行時，或當有新的充電活動時，使用資料庫計算
            if (!energyMonitorService.isMonitoring) {
                _currentEnergyMinutes.value = calculatedEnergy
                android.util.Log.d("EnergyViewModel", "使用資料庫備用計算，能量: $calculatedEnergy")
            }
        }.launchIn(viewModelScope)
    }
    
    private fun calculateEnergyFromDatabase(
        usageLogs: List<com.example.appenergytracker.data.entity.AppUsageLog>,
        chargeLogs: List<ChargeLog>,
        habitApps: List<com.example.appenergytracker.model.GoodHabitApp>
    ): Int {
        try {
            val packageToHabit = habitApps.associateBy { habitApp -> habitApp.packageName }

            val usageDelta = usageLogs.sumOf { log ->
                val cfg = packageToHabit[log.packageName]
                when {
                    cfg == null -> 0 // 未設定者不影響能量
                    cfg.isGoodHabit -> (log.usageMinutes * cfg.ratio).toInt() // 好習慣增加能量
                    cfg.isBadHabit -> -log.usageMinutes // 壞習慣消耗能量，每分鐘扣除1點
                    else -> 0
                }
            }

            val chargeDelta = chargeLogs.sumOf { log ->
                (log.durationMinutes * log.ratio).toInt()
            }

            val rawEnergy = usageDelta + chargeDelta
            return min(_maxEnergyMinutes.value, max(MIN_ENERGY, rawEnergy))
        } catch (e: Exception) {
            android.util.Log.e("EnergyViewModel", "從資料庫計算能量時出錯", e)
            return 0
        }
    }

    fun setMaxEnergyMinutes(maxMinutes: Int) {
        val validMaxEnergy = max(MIN_ENERGY, maxMinutes)
        _maxEnergyMinutes.value = validMaxEnergy
        android.util.Log.d("EnergyViewModel", "最大能量已設定為: $validMaxEnergy")
    }

    fun insertCharge(activityType: String, durationMinutes: Int, ratio: Float) {
        try {
            val log = ChargeLog(
                activityType = activityType,
                chargeDate = DateUtils.today(),
                durationMinutes = durationMinutes,
                ratio = ratio
            )
            viewModelScope.launch {
                appDao.insertChargeLog(log)
                android.util.Log.d("EnergyViewModel", "已記錄充電活動: $activityType, 時長: ${durationMinutes}分鐘, 比例: $ratio")
                
                // 直接更新 EnergyMonitorService 的能量狀態
                val currentEnergy = energyMonitorService.currentEnergy.value
                val energyToAdd = (durationMinutes * ratio).toInt()
                val newEnergy = min(_maxEnergyMinutes.value, currentEnergy + energyToAdd)
                
                // 直接設置 EnergyMonitorService 的能量值
                energyMonitorService.setCurrentEnergy(newEnergy)
                
                android.util.Log.d("EnergyViewModel", "直接更新 EnergyMonitorService 能量: $currentEnergy + $energyToAdd = $newEnergy")
            }
        } catch (e: Exception) {
            android.util.Log.e("EnergyViewModel", "記錄充電活動時出錯", e)
        }
    }
    
    // 清除所有歷史記錄
    fun clearAllHistory() {
        viewModelScope.launch {
            try {
                appDao.clearAllUsageLogs()
                appDao.clearAllChargeLogs()
                android.util.Log.d("EnergyViewModel", "已清除所有歷史記錄")
            } catch (e: Exception) {
                android.util.Log.e("EnergyViewModel", "清除歷史記錄時出錯", e)
            }
        }
    }
    
    // 重新載入習慣 App 配置
    fun reloadHabitAppsConfig() {
        viewModelScope.launch {
            try {
                energyMonitorService.reloadHabitAppsConfig()
                appLockService.reloadHabitAppsConfig()
                android.util.Log.d("EnergyViewModel", "已重新載入習慣 App 配置")
            } catch (e: Exception) {
                android.util.Log.e("EnergyViewModel", "重新載入習慣 App 配置時出錯", e)
            }
        }
    }
    
    // 獲取當前服務狀態
    fun getServiceStatus(): String {
        val currentEnergy = _currentEnergyMinutes.value
        val maxEnergy = _maxEnergyMinutes.value
        val isMonitoring = energyMonitorService.isMonitoring
        val serviceStatus = appLockService.getServiceStatus()
        return "能量: $currentEnergy/$maxEnergy, 監控: $isMonitoring, 服務: $serviceStatus"
    }
    
    // 手動刷新能量狀態
    fun refreshEnergyStatus() {
        viewModelScope.launch {
            try {
                // 重新載入習慣 App 配置
                energyMonitorService.reloadHabitAppsConfig()
                // 重新載入當前能量
                // 注意：這裡我們依賴 EnergyMonitorService 的自動更新機制
                android.util.Log.d("EnergyViewModel", "已手動刷新能量狀態")
            } catch (e: Exception) {
                android.util.Log.e("EnergyViewModel", "手動刷新能量狀態時出錯", e)
            }
        }
    }
    
    override fun onCleared() {
        super.onCleared()
        android.util.Log.d("EnergyViewModel", "ViewModel 已清理")
    }
}


