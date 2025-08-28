package com.example.appenergytracker.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.appenergytracker.service.AppLockService
import com.example.appenergytracker.service.BackgroundService
import com.example.appenergytracker.service.AppRestartService

class BootReceiver : BroadcastReceiver() {
    
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            "android.intent.action.QUICKBOOT_POWERON",
            "com.htc.intent.action.QUICKBOOT_POWERON" -> {
                Log.d("BootReceiver", "設備開機完成，啟動應用服務")
                
                // 延遲啟動，確保系統完全啟動
                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                    try {
                        // 啟動後台服務
                        BackgroundService.startService(context)
                        
                        // 啟動應用重啟服務
                        AppRestartService.startService(context)
                        
                        // 初始化 AppLockService
                        AppLockService.getInstance(context)
                        
                        Log.d("BootReceiver", "應用服務啟動成功")
                    } catch (e: Exception) {
                        Log.e("BootReceiver", "啟動應用服務失敗", e)
                    }
                }, 10000) // 延遲 10 秒啟動
            }
        }
    }
}
