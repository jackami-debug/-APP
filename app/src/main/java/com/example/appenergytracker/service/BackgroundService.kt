package com.example.appenergytracker.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import kotlinx.coroutines.*

class BackgroundService : Service() {
    
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var wakeLock: PowerManager.WakeLock? = null
    
    companion object {
        private const val WAKE_LOCK_TAG = "AppEnergyTracker:BackgroundService"
        
        fun startService(context: Context) {
            val intent = Intent(context, BackgroundService::class.java)
            context.startService(intent)
        }
        
        fun stopService(context: Context) {
            val intent = Intent(context, BackgroundService::class.java)
            context.stopService(intent)
        }
    }
    
    override fun onCreate() {
        super.onCreate()
        android.util.Log.d("BackgroundService", "後台服務創建")
        acquireWakeLock()
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        android.util.Log.d("BackgroundService", "後台服務開始")
        
        // 定期檢查服務狀態
        scope.launch {
            while (true) {
                delay(30000) // 每30秒檢查一次
                checkServiceStatus()
            }
        }
        
        // 定期重新獲取 WakeLock
        scope.launch {
            while (true) {
                delay(5 * 60 * 1000) // 每5分鐘重新獲取 WakeLock
                refreshWakeLock()
            }
        }
        
        return START_STICKY // 服務被殺死後自動重啟
    }
    
    override fun onDestroy() {
        super.onDestroy()
        android.util.Log.d("BackgroundService", "後台服務銷毀")
        releaseWakeLock()
        scope.cancel()
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
    

    
    private fun acquireWakeLock() {
        try {
            val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
            wakeLock = powerManager.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                WAKE_LOCK_TAG
            )
            wakeLock?.acquire(10*60*1000L) // 10分鐘
            android.util.Log.d("BackgroundService", "WakeLock 已獲取")
        } catch (e: Exception) {
            android.util.Log.e("BackgroundService", "獲取 WakeLock 失敗", e)
        }
    }
    
    private fun releaseWakeLock() {
        try {
            wakeLock?.let {
                if (it.isHeld) {
                    it.release()
                    android.util.Log.d("BackgroundService", "WakeLock 已釋放")
                }
            }
            wakeLock = null
        } catch (e: Exception) {
            android.util.Log.e("BackgroundService", "釋放 WakeLock 失敗", e)
        }
    }
    
    private fun refreshWakeLock() {
        try {
            releaseWakeLock()
            acquireWakeLock()
            android.util.Log.d("BackgroundService", "WakeLock 已刷新")
        } catch (e: Exception) {
            android.util.Log.e("BackgroundService", "刷新 WakeLock 失敗", e)
        }
    }
    
    private suspend fun checkServiceStatus() {
        try {
            // 檢查 AppLockService 是否正常運行
            val appLockService = AppLockService.getInstance(this)
            val status = appLockService.getServiceStatus()
            android.util.Log.d("BackgroundService", "服務狀態檢查: $status")
            
            // 如果服務異常，嘗試重新初始化
            if (status == "未運行") {
                android.util.Log.w("BackgroundService", "檢測到服務異常，嘗試重新初始化")
                appLockService.resetLockingState()
            }
        } catch (e: Exception) {
            android.util.Log.e("BackgroundService", "檢查服務狀態失敗", e)
        }
    }
}
