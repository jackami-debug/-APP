package com.example.appenergytracker.service

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import com.example.appenergytracker.data.database.AppDatabase
import com.example.appenergytracker.data.entity.AppUsageLog
import com.example.appenergytracker.model.GoodHabitApp
import com.example.appenergytracker.util.DateUtils
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

class EnergyMonitorService(private val context: Context) {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val appDao = AppDatabase.getDatabase(context).appDao()
    private val goodHabitDao = AppDatabase.getDatabase(context).goodHabitDao()
    
    // 追蹤每個 App 的使用時間（毫秒）
    private val appUsageTime = ConcurrentHashMap<String, AtomicLong>()
    
    // 追蹤每個 App 的累積使用時間（分鐘）
    private val appAccumulatedMinutes = ConcurrentHashMap<String, AtomicLong>()
    
    // 追蹤每個 App 的累積使用時間（秒）
    private val appAccumulatedSeconds = ConcurrentHashMap<String, AtomicLong>()
    
    // 快取習慣 App 配置，避免重複查詢資料庫
    private val habitAppsCache = MutableStateFlow<List<GoodHabitApp>>(emptyList())
    private val packageToHabitCache = MutableStateFlow<Map<String, GoodHabitApp>>(emptyMap())
    
    // 當前能量狀態
    private val _currentEnergy = MutableStateFlow(0)
    val currentEnergy: StateFlow<Int> = _currentEnergy.asStateFlow()
    
    // 監控狀態
    var isMonitoring = false
        private set
    private var monitoringJob: Job? = null
    
    // 當前正在使用的 App
    private var currentForegroundApp: String? = null
    private var lastEnergyValue = -1
    
    // 常數定義
    companion object {
        private const val MONITORING_INTERVAL = 1000L // 改為每1秒檢查一次，提高即時性
        private const val MAX_ENERGY = 180
        private const val ENERGY_DEDUCTION_PER_MINUTE = 1 // 每分鐘扣除1點能量
        private const val LOG_INTERVAL_MINUTES = 10 // 每10分鐘記錄一次
        private const val MIN_LOG_MINUTES = 5 // 最少5分鐘才記錄
        private const val SECONDS_PER_MINUTE = 60 // 每60秒為1分鐘
    }
    
    init {
        // 初始化時載入當前能量和習慣 App 配置
        scope.launch {
            loadHabitAppsConfig()
            loadCurrentEnergy()
        }
    }
    
    // 載入習慣 App 配置並建立快取
    private suspend fun loadHabitAppsConfig() {
        try {
            val habitApps = goodHabitDao.getAllGoodHabitApps().first()
            habitAppsCache.value = habitApps
            packageToHabitCache.value = habitApps.associateBy { it.packageName }
            android.util.Log.d("EnergyMonitor", "已載入 ${habitApps.size} 個習慣 App 配置")
        } catch (e: Exception) {
            android.util.Log.e("EnergyMonitor", "載入習慣 App 配置時出錯", e)
        }
    }
    
    fun startMonitoring() {
        if (isMonitoring) return
        isMonitoring = true
        
        monitoringJob = scope.launch {
            while (isMonitoring) {
                try {
                    checkCurrentAppUsage()
                    delay(MONITORING_INTERVAL)
                } catch (e: Exception) {
                    android.util.Log.e("EnergyMonitor", "監控過程中出錯", e)
                }
            }
        }
    }
    
    fun stopMonitoring() {
        isMonitoring = false
        monitoringJob?.cancel()
        monitoringJob = null
        
        // 記錄剩餘的使用時間
        scope.launch {
            recordRemainingUsage()
        }
    }
    
    private suspend fun checkCurrentAppUsage() {
        val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        
        // 獲取最近1秒的使用事件
        val endTime = System.currentTimeMillis()
        val startTime = endTime - MONITORING_INTERVAL
        
        val usageEvents = if (Build.VERSION.SDK_INT >= 28) {
            usageStatsManager.queryEvents(startTime, endTime)
        } else {
            @Suppress("DEPRECATION")
            usageStatsManager.queryEvents(startTime, endTime)
        }
        
        val detectedApp = getCurrentForegroundApp(usageEvents)
        
        // 更新當前正在使用的 App
        if (detectedApp != null && detectedApp != currentForegroundApp) {
            // App 切換了，記錄之前的 App 使用時間
            currentForegroundApp?.let { previousApp ->
                recordAppSwitch(previousApp)
            }
            currentForegroundApp = detectedApp
            android.util.Log.d("EnergyMonitor", "App 切換: $detectedApp")
        }
        
        // 處理當前正在使用的 App
        currentForegroundApp?.let { currentApp ->
            // 更新該 App 的使用時間
            appUsageTime.getOrPut(currentApp) { AtomicLong(0) }.addAndGet(MONITORING_INTERVAL)
            
            // 累積使用時間（秒）
            val accumulatedSeconds = appAccumulatedSeconds.getOrPut(currentApp) { AtomicLong(0) }
            val newSeconds = accumulatedSeconds.addAndGet(1)
            
            // 每60秒增加1分鐘
            if (newSeconds % SECONDS_PER_MINUTE == 0L) {
                val accumulatedMinutes = appAccumulatedMinutes.getOrPut(currentApp) { AtomicLong(0) }
                accumulatedMinutes.addAndGet(1)
                
                // 每10分鐘記錄一次使用日誌
                if (accumulatedMinutes.get() % LOG_INTERVAL_MINUTES == 0L) {
                    recordUsageLog(currentApp, LOG_INTERVAL_MINUTES.toInt())
                }
                
                // 每分鐘扣除一次能量（確保精確的1:1比例）
                checkAndDeductEnergy(currentApp)
            }
        }
    }
    
    private fun getCurrentForegroundApp(usageEvents: UsageEvents): String? {
        var currentApp: String? = null
        val event = UsageEvents.Event()
        
        while (usageEvents.hasNextEvent()) {
            usageEvents.getNextEvent(event)
            if (event.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND) {
                currentApp = event.packageName
            }
        }
        
        return currentApp
    }
    
    private suspend fun checkAndDeductEnergy(packageName: String) {
        // 使用快取的習慣 App 配置
        val habitApp = packageToHabitCache.value[packageName]
        
        if (habitApp?.isBadHabit == true) {
            // 壞習慣 App，扣除能量（每分鐘扣除1點）
            val energyToDeduct = ENERGY_DEDUCTION_PER_MINUTE
            val currentEnergyValue = _currentEnergy.value
            val newEnergy = maxOf(0, currentEnergyValue - energyToDeduct)
            _currentEnergy.value = newEnergy
            
            android.util.Log.d("EnergyMonitor", "壞習慣 App: $packageName, 扣除能量: $energyToDeduct, 當前能量: $newEnergy")
            
            // 能量歸零檢測
            if (newEnergy <= 0 && currentEnergyValue > 0) {
                android.util.Log.d("EnergyMonitor", "能量歸零，立即阻止當前壞習慣 App: $packageName")
                // 立即阻止當前正在使用的壞習慣 App
                immediatelyBlockCurrentApp(packageName)
            }
        } else if (habitApp?.isGoodHabit == true) {
            // 好習慣 App，增加能量
            val energyToAdd = habitApp.ratio.toInt()
            val currentEnergyValue = _currentEnergy.value
            val newEnergy = minOf(MAX_ENERGY, currentEnergyValue + energyToAdd)
            _currentEnergy.value = newEnergy
            
            android.util.Log.d("EnergyMonitor", "好習慣 App: $packageName, 增加能量: $energyToAdd, 當前能量: $newEnergy")
        }
    }
    
    // 立即阻止當前正在使用的壞習慣 App
    private fun immediatelyBlockCurrentApp(packageName: String) {
        try {
            android.util.Log.d("EnergyMonitor", "立即阻止當前壞習慣 App: $packageName")
            
            // 使用 Handler 在主線程執行，避免阻塞
            Handler(Looper.getMainLooper()).post {
                try {
                    val accessibilityService = AppLockAccessibilityService.getInstance()
                    android.util.Log.d("EnergyMonitor", "無障礙服務實例: $accessibilityService")
                    
                    if (accessibilityService != null) {
                        android.util.Log.d("EnergyMonitor", "立即阻止壞習慣 App: $packageName")
                        // 直接阻止當前 App，不需要等待 App 切換
                        accessibilityService.immediatelyBlockCurrentApp(packageName)
                    } else {
                        android.util.Log.w("EnergyMonitor", "無障礙服務未運行，無法立即阻止 App")
                        // 嘗試延遲重試
                        Handler(Looper.getMainLooper()).postDelayed({
                            val retryService = AppLockAccessibilityService.getInstance()
                            if (retryService != null) {
                                android.util.Log.d("EnergyMonitor", "重試：立即阻止壞習慣 App: $packageName")
                                retryService.immediatelyBlockCurrentApp(packageName)
                            } else {
                                android.util.Log.e("EnergyMonitor", "重試失敗：無障礙服務仍未運行")
                            }
                        }, 1000) // 1秒後重試
                    }
                } catch (e: Exception) {
                    android.util.Log.e("EnergyMonitor", "立即阻止 App 時出錯", e)
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("EnergyMonitor", "立即阻止 App 時出錯", e)
        }
    }
    
    private suspend fun recordUsageLog(packageName: String, minutes: Int) {
        try {
            val pm: PackageManager = context.packageManager
            val appName = runCatching { 
                pm.getApplicationLabel(pm.getApplicationInfo(packageName, 0)).toString() 
            }.getOrDefault(packageName)
            
            val usageLog = AppUsageLog(
                packageName = packageName,
                appName = appName,
                usageDate = DateUtils.today(),
                usageMinutes = minutes
            )
            
            appDao.insertUsageLog(usageLog)
        } catch (e: Exception) {
            android.util.Log.e("EnergyMonitor", "記錄使用日誌時出錯", e)
        }
    }
    
    suspend fun loadCurrentEnergy() {
        try {
            val today = DateUtils.today()
            val usageLogs = appDao.getUsageLogsByDate(today).first()
            val chargeLogs = appDao.getChargeLogsByDate(today).first()
            
            // 使用快取的習慣 App 配置
            val packageToHabit = packageToHabitCache.value
            
            val usageDelta = usageLogs.sumOf { log ->
                val cfg = packageToHabit[log.packageName]
                when {
                    cfg == null -> 0
                    cfg.isGoodHabit -> (log.usageMinutes * cfg.ratio).toInt()
                    cfg.isBadHabit -> -log.usageMinutes
                    else -> 0
                }
            }
            
            val chargeDelta = chargeLogs.sumOf { log ->
                (log.durationMinutes * log.ratio).toInt()
            }
            
            val totalEnergy = usageDelta + chargeDelta
            val finalEnergy = maxOf(0, minOf(MAX_ENERGY, totalEnergy))
            _currentEnergy.value = finalEnergy
            lastEnergyValue = finalEnergy
            
            android.util.Log.d("EnergyMonitor", "載入當前能量 - 使用記錄: $usageDelta, 充電記錄: $chargeDelta, 總能量: $finalEnergy")
        } catch (e: Exception) {
            android.util.Log.e("EnergyMonitor", "載入當前能量時出錯", e)
        }
    }
    
    fun getCurrentAppUsageTime(packageName: String): Long {
        return appUsageTime[packageName]?.get() ?: 0
    }
    
    fun clearUsageTime() {
        appUsageTime.clear()
        appAccumulatedMinutes.clear()
        appAccumulatedSeconds.clear()
    }
    
    private suspend fun recordRemainingUsage() {
        try {
            // 記錄所有累積的使用時間
            appAccumulatedMinutes.forEach { (packageName, minutes) ->
                if (minutes.get() > 0) {
                    recordUsageLog(packageName, minutes.get().toInt())
                }
            }
            
            // 清空累積記錄
            appAccumulatedMinutes.clear()
            appAccumulatedSeconds.clear()
        } catch (e: Exception) {
            android.util.Log.e("EnergyMonitor", "記錄剩餘使用時間時出錯", e)
        }
    }
    
    suspend fun recordAppSwitch(packageName: String) {
        try {
            val accumulatedMinutes = appAccumulatedMinutes[packageName]
            val accumulatedSeconds = appAccumulatedSeconds[packageName]
            
            if (accumulatedMinutes != null && accumulatedMinutes.get() > 0) {
                // 記錄當前累積的使用時間（如果超過5分鐘才記錄）
                if (accumulatedMinutes.get() >= MIN_LOG_MINUTES) {
                    recordUsageLog(packageName, accumulatedMinutes.get().toInt())
                    // 重置累積時間
                    accumulatedMinutes.set(0)
                    accumulatedSeconds?.set(0)
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("EnergyMonitor", "記錄 App 切換時出錯", e)
        }
    }
    
    // 重新載入習慣 App 配置（當配置變更時調用）
    fun reloadHabitAppsConfig() {
        scope.launch {
            loadHabitAppsConfig()
        }
    }
    
    // 獲取當前正在使用的 App
    fun getCurrentForegroundApp(): String? = currentForegroundApp
    
    // 測試用方法：模擬能量消耗
    fun simulateEnergyConsumption(amount: Int) {
        val currentValue = _currentEnergy.value
        val newValue = maxOf(0, currentValue - amount)
        _currentEnergy.value = newValue
        android.util.Log.d("EnergyMonitor", "模擬消耗能量: $amount, 從 $currentValue 變為 $newValue")
    }
    
    // 測試用方法：重置能量
    fun resetEnergy(maxEnergy: Int = MAX_ENERGY) {
        _currentEnergy.value = maxEnergy
        android.util.Log.d("EnergyMonitor", "重置能量為: $maxEnergy")
    }
    
    // 設置當前能量值
    fun setCurrentEnergy(energy: Int) {
        val clampedEnergy = maxOf(0, minOf(MAX_ENERGY, energy))
        _currentEnergy.value = clampedEnergy
        android.util.Log.d("EnergyMonitor", "設置當前能量為: $clampedEnergy")
    }
    
    fun destroy() {
        stopMonitoring()
        scope.cancel()
    }
}
