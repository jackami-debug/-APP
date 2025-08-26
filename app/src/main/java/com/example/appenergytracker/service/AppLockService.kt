package com.example.appenergytracker.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.appenergytracker.MainActivity
import com.example.appenergytracker.R
import com.example.appenergytracker.data.database.AppDatabase
import com.example.appenergytracker.util.DateUtils
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.collectLatest

class AppLockService private constructor(private val context: Context) {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val goodHabitDao = AppDatabase.getDatabase(context).goodHabitDao()
    private val energyMonitorService = EnergyMonitorService(context)
    
    private var isLockingEnabled = false
    private var lastEnergyValue = -1
    private var isNotificationShown = false
    
    companion object {
        private const val CHANNEL_ID = "energy_alert"
        private const val NOTIFICATION_ID = 1001
        private const val INIT_RETRY_COUNT = 10
        private const val INIT_RETRY_DELAY = 500L
        private var instance: AppLockService? = null
        
        fun getInstance(context: Context): AppLockService {
            if (instance == null) {
                instance = AppLockService(context)
            }
            return instance!!
        }
    }
    
    init {
        android.util.Log.d("AppLockService", "初始化 AppLockService")
        createNotificationChannel()
        
        // 立即開始監控
        startMonitoring()
        
        // 初始化時檢查當前能量狀態
        initializeLockingState()
    }
    
    private fun initializeLockingState() {
        scope.launch {
            try {
                // 等待能量監控服務初始化完成並載入能量
                repeat(INIT_RETRY_COUNT) {
                    val currentEnergy = energyMonitorService.currentEnergy.value
                    if (currentEnergy > 0 || energyMonitorService.isMonitoring) {
                        android.util.Log.d("AppLockService", "初始化時檢查能量: $currentEnergy")
                        
                        // 根據能量狀態設置鎖定狀態
                        updateLockingState(currentEnergy)
                        return@repeat
                    }
                    delay(INIT_RETRY_DELAY)
                }
                
                // 如果重試後仍未初始化完成，設置預設狀態
                if (lastEnergyValue == -1) {
                    android.util.Log.w("AppLockService", "初始化超時，設置預設未鎖定狀態")
                    updateLockingState(0)
                }
            } catch (e: Exception) {
                android.util.Log.e("AppLockService", "初始化鎖定狀態時出錯", e)
                updateLockingState(0)
            }
        }
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "能量提醒"
            val descriptionText = "當能量歸零時的提醒通知"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    private fun startMonitoring() {
        android.util.Log.d("AppLockService", "開始監控能量狀態")
        
        // 啟動 EnergyMonitorService 的監控
        energyMonitorService.startMonitoring()
        
        scope.launch {
            energyMonitorService.currentEnergy.collectLatest { currentEnergy ->
                android.util.Log.d("AppLockService", "收到能量更新: $currentEnergy (之前: $lastEnergyValue)")
                if (lastEnergyValue != currentEnergy) {
                    lastEnergyValue = currentEnergy
                    checkAndHandleEnergyStatus(currentEnergy)
                }
            }
        }
    }
    
    private suspend fun checkAndHandleEnergyStatus(currentEnergy: Int) {
        android.util.Log.d("AppLockService", "檢查能量狀態: $currentEnergy, 當前鎖定狀態: $isLockingEnabled")
        
        when {
            currentEnergy <= 0 && !isLockingEnabled -> {
                // 能量歸零，啟用鎖定
                android.util.Log.d("AppLockService", "能量歸零，啟用鎖定")
                updateLockingState(currentEnergy)
                showEnergyDepletedNotification()
            }
            currentEnergy > 0 && isLockingEnabled -> {
                // 能量恢復，禁用鎖定
                android.util.Log.d("AppLockService", "能量恢復，禁用鎖定")
                updateLockingState(currentEnergy)
                hideEnergyDepletedNotification()
            }
            currentEnergy > 0 && !isLockingEnabled -> {
                // 能量充足且未鎖定，確保狀態正確
                android.util.Log.d("AppLockService", "能量充足，確保未鎖定狀態")
                updateLockingState(currentEnergy)
            }
            currentEnergy <= 0 && isLockingEnabled -> {
                // 能量歸零且已鎖定，確保通知顯示
                android.util.Log.d("AppLockService", "能量歸零且已鎖定，確保通知顯示")
                if (!isNotificationShown) {
                    showEnergyDepletedNotification()
                }
            }
        }
    }
    
    private fun updateLockingState(currentEnergy: Int) {
        val shouldLock = currentEnergy <= 0
        if (isLockingEnabled != shouldLock) {
            isLockingEnabled = shouldLock
            android.util.Log.d("AppLockService", "鎖定狀態已更新: $isLockingEnabled (能量: $currentEnergy)")
        }
    }
    
    private fun showEnergyDepletedNotification() {
        if (isNotificationShown) return
        
        try {
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            val pendingIntent: PendingIntent = PendingIntent.getActivity(
                context, 0, intent,
                PendingIntent.FLAG_IMMUTABLE
            )
            
            val builder = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle("能量已歸零！")
                .setContentText("請充電後再使用壞習慣 App")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setOngoing(true)
                .setAutoCancel(false)
                .setContentIntent(pendingIntent)
                .addAction(
                    R.drawable.ic_launcher_foreground,
                    "立即充電",
                    pendingIntent
                )
            
            with(NotificationManagerCompat.from(context)) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    if (context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                        notify(NOTIFICATION_ID, builder.build())
                        isNotificationShown = true
                    }
                } else {
                    notify(NOTIFICATION_ID, builder.build())
                    isNotificationShown = true
                }
            }
            
            android.util.Log.d("AppLockService", "能量歸零通知已顯示")
        } catch (e: Exception) {
            android.util.Log.e("AppLockService", "顯示能量歸零通知時出錯", e)
        }
    }
    
    private fun hideEnergyDepletedNotification() {
        try {
            NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID)
            isNotificationShown = false
            android.util.Log.d("AppLockService", "能量歸零通知已隱藏")
        } catch (e: Exception) {
            android.util.Log.e("AppLockService", "隱藏能量歸零通知時出錯", e)
        }
    }
    
    fun isAppLocked(packageName: String): Boolean {
        if (!isLockingEnabled) return false
        
        return runBlocking {
            try {
                val habitApps = goodHabitDao.getAllGoodHabitApps().first()
                val app = habitApps.find { it.packageName == packageName }
                app?.isBadHabit == true
            } catch (e: Exception) {
                android.util.Log.e("AppLockService", "檢查 App 鎖定狀態時出錯", e)
                false
            }
        }
    }
    
    fun getCurrentEnergy(): Int = energyMonitorService.currentEnergy.value
    
    fun isLockingEnabled(): Boolean = isLockingEnabled
    
    // 獲取 EnergyMonitorService 實例
    fun getEnergyMonitorService(): EnergyMonitorService = energyMonitorService
    
    // 手動重置鎖定狀態（用於調試）
    fun resetLockingState() {
        scope.launch {
            val currentEnergy = energyMonitorService.currentEnergy.value
            updateLockingState(currentEnergy)
            android.util.Log.d("AppLockService", "手動重置：能量 $currentEnergy，鎖定狀態: $isLockingEnabled")
        }
    }
    
    // 檢查服務狀態
    fun getServiceStatus(): String {
        val currentEnergy = energyMonitorService.currentEnergy.value
        val isLocking = isLockingEnabled
        val isMonitoring = energyMonitorService.isMonitoring
        val notificationShown = isNotificationShown
        return "能量: $currentEnergy, 鎖定: $isLocking, 監控: $isMonitoring, 通知: $notificationShown"
    }
    
    fun recordAppSwitch(packageName: String) {
        // 當 App 切換時，記錄當前累積的使用時間
        scope.launch {
            energyMonitorService.recordAppSwitch(packageName)
        }
    }
    
    // 重新載入習慣 App 配置
    fun reloadHabitAppsConfig() {
        scope.launch {
            energyMonitorService.reloadHabitAppsConfig()
        }
    }
    
    fun destroy() {
        scope.cancel()
        hideEnergyDepletedNotification()
    }
}
