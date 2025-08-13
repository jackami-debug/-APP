package com.example.appenergytracker.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import android.widget.Toast
import android.os.Handler
import android.os.Looper
import com.example.appenergytracker.data.database.AppDatabase
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import java.util.concurrent.ConcurrentHashMap

class AppLockAccessibilityService : AccessibilityService() {
    
    companion object {
        private var instance: AppLockAccessibilityService? = null
        private const val LOW_ENERGY_THRESHOLD = 10
        private const val TOAST_DURATION_SHORT = Toast.LENGTH_SHORT
        private const val TOAST_DURATION_LONG = Toast.LENGTH_LONG
        private const val BLOCK_DELAY = 500L // 阻止延遲，確保 UI 更新完成
        
        fun getInstance(): AppLockAccessibilityService? = instance
    }
    
    // 快取壞習慣 App 列表，避免重複查詢資料庫
    private val badHabitAppsCache = ConcurrentHashMap<String, Boolean>()
    private var lastCacheUpdateTime = 0L
    private val cacheValidityDuration = 30000L // 30秒快取有效期
    
    // 當前正在使用的 App
    private var currentForegroundApp: String? = null
    
    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        
        try {
            android.util.Log.d("AppLockAccessibilityService", "服務已成功連接")
            
            // 顯示服務啟動成功的訊息
            Handler(Looper.getMainLooper()).post {
                Toast.makeText(
                    this,
                    "App 鎖定服務已啟動",
                    TOAST_DURATION_SHORT
                ).show()
            }
            
            // 初始化快取
            updateBadHabitAppsCache()
        } catch (e: Exception) {
            android.util.Log.e("AppLockAccessibilityService", "服務連接時出錯", e)
        }
    }
    
    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        try {
            if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
                val packageName = event.packageName?.toString()
                if (packageName != null && packageName != "com.example.appenergytracker") {
                    // 避免監控自己的應用程式
                    android.util.Log.d("AppLockAccessibilityService", "檢測到 App 切換: $packageName")
                    
                    // 更新當前正在使用的 App
                    currentForegroundApp = packageName
                    
                    checkAndBlockApp(packageName)
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("AppLockAccessibilityService", "處理無障礙事件時出錯", e)
        }
    }
    
    private fun checkAndBlockApp(packageName: String) {
        try {
            // 檢查是否是壞習慣 App 且能量已歸零
            val appLockService = AppLockService.getInstance(applicationContext)
            
            // 使用快取檢查是否是壞習慣 App
            val isBadHabitApp = isBadHabitApp(packageName)
            
            if (isBadHabitApp) {
                val currentEnergy = appLockService.getCurrentEnergy()
                val isLockingEnabled = appLockService.isLockingEnabled()
                android.util.Log.d("AppLock", "壞習慣 App: $packageName, 當前能量: $currentEnergy, 鎖定狀態: $isLockingEnabled")
                
                when {
                    currentEnergy <= 0 || isLockingEnabled -> {
                        // 能量歸零或鎖定已啟用，阻止 App
                        android.util.Log.d("AppLock", "能量歸零或鎖定已啟用，阻止 App: $packageName")
                        blockAppLaunch(packageName)
                    }
                    currentEnergy < LOW_ENERGY_THRESHOLD -> {
                        // 能量低於10分鐘但大於0，顯示警告
                        android.util.Log.d("AppLock", "能量偏低，顯示警告，能量: $currentEnergy")
                        showLowEnergyWarning(currentEnergy)
                        // 記錄 App 切換，觸發使用時間記錄
                        appLockService.recordAppSwitch(packageName)
                    }
                    else -> {
                        // 能量充足，只記錄使用時間
                        android.util.Log.d("AppLock", "能量充足，記錄使用時間，能量: $currentEnergy")
                        appLockService.recordAppSwitch(packageName)
                    }
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("AppLockAccessibilityService", "檢查和阻止 App 時出錯", e)
        }
    }
    
    private fun isBadHabitApp(packageName: String): Boolean {
        try {
            // 檢查快取是否有效
            val currentTime = System.currentTimeMillis()
            if (currentTime - lastCacheUpdateTime > cacheValidityDuration) {
                updateBadHabitAppsCache()
            }
            
            // 從快取中查詢
            return badHabitAppsCache[packageName] ?: false
        } catch (e: Exception) {
            android.util.Log.e("AppLockAccessibilityService", "檢查壞習慣 App 時出錯", e)
            return false
        }
    }
    
    private fun updateBadHabitAppsCache() {
        try {
            runBlocking {
                val goodHabitDao = AppDatabase.getDatabase(applicationContext).goodHabitDao()
                val habitApps = goodHabitDao.getAllGoodHabitApps().first()
                
                // 清空舊快取
                badHabitAppsCache.clear()
                
                // 更新快取
                habitApps.forEach { habitApp ->
                    if (habitApp.isBadHabit) {
                        badHabitAppsCache[habitApp.packageName] = true
                    }
                }
                
                lastCacheUpdateTime = System.currentTimeMillis()
                android.util.Log.d("AppLockAccessibilityService", "已更新壞習慣 App 快取，共 ${badHabitAppsCache.size} 個")
            }
        } catch (e: Exception) {
            android.util.Log.e("AppLockAccessibilityService", "更新壞習慣 App 快取時出錯", e)
        }
    }
    
    private fun blockAppLaunch(packageName: String) {
        try {
            // 顯示阻止訊息
            showBlockMessage()
            
            // 回到主畫面
            performGlobalAction(GLOBAL_ACTION_HOME)
        } catch (e: Exception) {
            android.util.Log.e("AppLockAccessibilityService", "阻止 App 啟動時出錯", e)
        }
    }
    
    // 立即阻止當前正在使用的壞習慣 App（關鍵新功能）
    fun immediatelyBlockCurrentApp(packageName: String) {
        try {
            android.util.Log.d("AppLockAccessibilityService", "立即阻止當前壞習慣 App: $packageName")
            
            // 檢查是否為壞習慣 App
            val isBadHabitApp = isBadHabitApp(packageName)
            
            if (isBadHabitApp) {
                // 立即顯示阻止訊息
                showImmediateBlockMessage()
                
                // 延遲一小段時間後回到主畫面，確保訊息顯示完成
                Handler(Looper.getMainLooper()).postDelayed({
                    try {
                        // 回到主畫面
                        performGlobalAction(GLOBAL_ACTION_HOME)
                        android.util.Log.d("AppLockAccessibilityService", "已立即阻止壞習慣 App: $packageName")
                    } catch (e: Exception) {
                        android.util.Log.e("AppLockAccessibilityService", "立即阻止 App 時執行回到主畫面出錯", e)
                    }
                }, BLOCK_DELAY)
            } else {
                android.util.Log.d("AppLockAccessibilityService", "App 不是壞習慣 App，不需要立即阻止: $packageName")
            }
        } catch (e: Exception) {
            android.util.Log.e("AppLockAccessibilityService", "立即阻止 App 時出錯", e)
        }
    }
    
    private fun showBlockMessage() {
        // 使用 Handler 在主線程顯示 Toast
        Handler(Looper.getMainLooper()).post {
            try {
                Toast.makeText(
                    this,
                    "⚡ 能量已歸零！\n請充電後再使用壞習慣 App",
                    TOAST_DURATION_LONG
                ).show()
            } catch (e: Exception) {
                android.util.Log.e("AppLockAccessibilityService", "顯示阻止訊息時出錯", e)
            }
        }
    }
    
    private fun showImmediateBlockMessage() {
        // 使用 Handler 在主線程顯示立即阻止訊息
        Handler(Looper.getMainLooper()).post {
            try {
                Toast.makeText(
                    this,
                    "⚡ 能量歸零！\n立即阻止壞習慣 App 使用",
                    TOAST_DURATION_LONG
                ).show()
            } catch (e: Exception) {
                android.util.Log.e("AppLockAccessibilityService", "顯示立即阻止訊息時出錯", e)
            }
        }
    }
    
    private fun showLowEnergyWarning(currentEnergy: Int) {
        // 使用 Handler 在主線程顯示 Toast
        Handler(Looper.getMainLooper()).post {
            try {
                Toast.makeText(
                    this,
                    "⚠️ 能量偏低（${currentEnergy}分鐘）！\n建議減少壞習慣 App 使用",
                    TOAST_DURATION_SHORT
                ).show()
            } catch (e: Exception) {
                android.util.Log.e("AppLockAccessibilityService", "顯示低能量警告時出錯", e)
            }
        }
    }
    
    // 強制阻止當前正在使用的 App（用於能量歸零時立即阻止）
    fun forceBlockCurrentApp(packageName: String) {
        try {
            android.util.Log.d("AppLockAccessibilityService", "強制阻止當前 App: $packageName")
            
            // 檢查是否為壞習慣 App
            val isBadHabitApp = isBadHabitApp(packageName)
            
            if (isBadHabitApp) {
                // 立即阻止並回到主畫面
                blockAppLaunch(packageName)
            } else {
                android.util.Log.d("AppLockAccessibilityService", "App 不是壞習慣 App，不需要阻止: $packageName")
            }
        } catch (e: Exception) {
            android.util.Log.e("AppLockAccessibilityService", "強制阻止 App 時出錯", e)
        }
    }
    
    // 重新載入壞習慣 App 快取
    fun reloadBadHabitAppsCache() {
        updateBadHabitAppsCache()
    }
    
    // 獲取當前正在使用的 App
    fun getCurrentForegroundApp(): String? = currentForegroundApp
    
    override fun onInterrupt() {
        android.util.Log.d("AppLockAccessibilityService", "服務被中斷")
    }
    
    // 檢查服務狀態
    fun isServiceRunning(): Boolean {
        return instance != null
    }
    
    // 獲取服務實例
    fun getServiceInstance(): AppLockAccessibilityService? {
        return instance
    }
    
    override fun onDestroy() {
        super.onDestroy()
        instance = null
        badHabitAppsCache.clear()
        android.util.Log.d("AppLockAccessibilityService", "服務已銷毀")
    }
}
