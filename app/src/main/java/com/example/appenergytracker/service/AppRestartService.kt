package com.example.appenergytracker.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.util.Log
import kotlinx.coroutines.*

class AppRestartService : Service() {
    
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    companion object {
        fun startService(context: Context) {
            val intent = Intent(context, AppRestartService::class.java)
            context.startService(intent)
        }
        
        fun stopService(context: Context) {
            val intent = Intent(context, AppRestartService::class.java)
            context.stopService(intent)
        }
    }
    
    override fun onCreate() {
        super.onCreate()
        Log.d("AppRestartService", "應用重啟服務創建")
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("AppRestartService", "應用重啟服務開始")
        
        // 定期檢查並重啟必要的服務
        scope.launch {
            while (true) {
                delay(60000) // 每分鐘檢查一次
                checkAndRestartServices()
            }
        }
        
        return START_STICKY
    }
    
    override fun onDestroy() {
        super.onDestroy()
        Log.d("AppRestartService", "應用重啟服務銷毀")
        scope.cancel()
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    private suspend fun checkAndRestartServices() {
        try {
            // 檢查 BackgroundService 是否運行
            if (!isServiceRunning(BackgroundService::class.java)) {
                Log.w("AppRestartService", "檢測到 BackgroundService 未運行，嘗試重啟")
                BackgroundService.startService(this)
            }
            
            // 檢查 AppLockService 是否正常
            val appLockService = AppLockService.getInstance(this)
            val status = appLockService.getServiceStatus()
            if (status == "未運行") {
                Log.w("AppRestartService", "檢測到 AppLockService 異常，嘗試重新初始化")
                appLockService.resetLockingState()
            }
            
            // 檢查無障礙服務
            val accessibilityService = AppLockAccessibilityService.getInstance()
            if (accessibilityService?.isServiceRunning() != true) {
                Log.w("AppRestartService", "檢測到無障礙服務未運行，提示用戶")
                // 這裡可以發送通知提醒用戶重新開啟無障礙服務
            }
            
        } catch (e: Exception) {
            Log.e("AppRestartService", "檢查服務狀態失敗", e)
        }
    }
    
    private fun isServiceRunning(serviceClass: Class<*>): Boolean {
        val manager = getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
        for (service in manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.name == service.service.className) {
                return true
            }
        }
        return false
    }
}
