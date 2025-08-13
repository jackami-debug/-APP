package com.example.appenergytracker

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.Surface
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import com.example.appenergytracker.ui.theme.AppEnergyTrackerTheme
import androidx.navigation.compose.rememberNavController
import com.example.appenergytracker.ui.navigation.AppNavGraph
import com.example.appenergytracker.service.AppLockService

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 初始化 AppLockService 並確保監控開始
        val appLockService = AppLockService.getInstance(this)
        appLockService.getEnergyMonitorService().startMonitoring()
        
        android.util.Log.d("MainActivity", "AppLockService 已初始化，監控已開始")
        
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
}
