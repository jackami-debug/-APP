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
        // 監聽能量狀態並顯示通知；滑除後再次開啟 App 會重新觸發這裡
        lifecycleScope.launchWhenResumed {
            energyViewModel.currentEnergyMinutes.collectLatest { current ->
                val max = energyViewModel.maxEnergyMinutes.value
                EnergyStatusNotifier.show(this@MainActivity, current, max)
            }
        }
    }
}
