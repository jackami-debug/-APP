package com.example.appenergytracker

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.activity.compose.setContent
import androidx.compose.material3.Surface
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import com.example.appenergytracker.ui.theme.AppEnergyTrackerTheme
import androidx.navigation.compose.rememberNavController
import com.example.appenergytracker.ui.navigation.AppNavGraph
import com.example.appenergytracker.service.AppLockService
import com.example.appenergytracker.service.EnergyStatusNotifier
import com.example.appenergytracker.viewmodel.EnergyViewModel
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val requestNotificationsPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { _ -> }
    private val energyViewModel: EnergyViewModel by viewModels()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 初始化 AppLockService 並確保監控開始
        val appLockService = AppLockService.getInstance(this)
        appLockService.getEnergyMonitorService().startMonitoring()
        
        android.util.Log.d("MainActivity", "AppLockService 已初始化，監控已開始")

        // 請求通知權限（Android 13+）
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestNotificationsPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        
        // 立即開始通知監聽
        startNotificationMonitoring()
        
        setContent {
            AppEnergyTrackerTheme {
                Surface(
                    modifier = Modifier,
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    AppNavGraph(navController = navController)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // 確保通知監聽正在運行
        if (!isNotificationMonitoringActive) {
            startNotificationMonitoring()
        }
    }
    
    override fun onPause() {
        super.onPause()
        // 應用進入背景時保持通知監聽，確保通知持續更新
        // 不停止監聽，讓通知在背景中也能即時更新
    }
    
    private var isNotificationMonitoringActive = false
    
    private fun startNotificationMonitoring() {
        if (isNotificationMonitoringActive) return
        
        isNotificationMonitoringActive = true
        android.util.Log.d("MainActivity", "開始通知監聽")
        
        // 在協程中監聽能量變化並即時更新通知
        lifecycleScope.launch {
            energyViewModel.currentEnergyMinutes.collectLatest { current ->
                val max = energyViewModel.maxEnergyMinutes.value
                android.util.Log.d("MainActivity", "能量更新: $current/$max，更新通知")
                EnergyStatusNotifier.show(this@MainActivity, current, max)
            }
        }
    }
}
