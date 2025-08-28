package com.example.appenergytracker.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.util.Log

object PermissionManager {
    
    /**
     * 檢查是否已忽略電池優化
     */
    fun isIgnoringBatteryOptimizations(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
            powerManager.isIgnoringBatteryOptimizations(context.packageName)
        } else {
            true // Android 6.0 以下不需要此權限
        }
    }
    
    /**
     * 請求忽略電池優化權限
     */
    fun requestIgnoreBatteryOptimizations(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                    data = Uri.parse("package:${context.packageName}")
                }
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
            } catch (e: Exception) {
                Log.e("PermissionManager", "請求忽略電池優化權限失敗", e)
            }
        }
    }
    
    /**
     * 檢查是否有懸浮窗權限
     */
    fun hasOverlayPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(context)
        } else {
            true // Android 6.0 以下不需要此權限
        }
    }
    
    /**
     * 請求懸浮窗權限
     */
    fun requestOverlayPermission(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION).apply {
                    data = Uri.parse("package:${context.packageName}")
                }
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
            } catch (e: Exception) {
                Log.e("PermissionManager", "請求懸浮窗權限失敗", e)
            }
        }
    }
    
    /**
     * 打開自啟動管理頁面（針對不同廠商）
     */
    fun openAutoStartSettings(context: Context) {
        try {
            val intent = Intent()
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            
            // 嘗試打開通用的自啟動管理頁面
            when {
                // 小米
                isMIUI() -> {
                    intent.action = "miui.intent.action.APP_PERM_EDITOR"
                    intent.putExtra("extra_pkgname", context.packageName)
                }
                // 華為
                isEMUI() -> {
                    intent.action = "huawei.intent.action.HSM_BOOTAPP_MANAGER"
                }
                // OPPO
                isColorOS() -> {
                    intent.action = "oppo.intent.action.OPPO_BOOT_COMPLETE"
                }
                // vivo
                isFuntouchOS() -> {
                    intent.action = "vivo.intent.action.VIVO_BOOT_COMPLETE"
                }
                // 三星
                isOneUI() -> {
                    intent.action = "samsung.intent.action.SAMSUNG_BOOT_COMPLETE"
                }
                // 通用
                else -> {
                    intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                    intent.data = Uri.parse("package:${context.packageName}")
                }
            }
            
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e("PermissionManager", "打開自啟動設定失敗", e)
            // 如果特定廠商設定失敗，使用通用設定
            try {
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.parse("package:${context.packageName}")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            } catch (e2: Exception) {
                Log.e("PermissionManager", "打開通用設定也失敗", e2)
            }
        }
    }
    
    /**
     * 檢查是否為 MIUI 系統
     */
    private fun isMIUI(): Boolean {
        return try {
            val prop = System.getProperty("ro.miui.ui.version.name")
            !prop.isNullOrEmpty()
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * 檢查是否為 EMUI 系統
     */
    private fun isEMUI(): Boolean {
        return try {
            val prop = System.getProperty("ro.build.version.emui")
            !prop.isNullOrEmpty()
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * 檢查是否為 ColorOS 系統
     */
    private fun isColorOS(): Boolean {
        return try {
            val prop = System.getProperty("ro.build.version.opporom")
            !prop.isNullOrEmpty()
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * 檢查是否為 FuntouchOS 系統
     */
    private fun isFuntouchOS(): Boolean {
        return try {
            val prop = System.getProperty("ro.vivo.os.version")
            !prop.isNullOrEmpty()
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * 檢查是否為 OneUI 系統
     */
    private fun isOneUI(): Boolean {
        return try {
            val prop = System.getProperty("ro.build.version.oneui")
            !prop.isNullOrEmpty()
        } catch (e: Exception) {
            false
        }
    }
}
