package com.example.appenergytracker.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.appenergytracker.service.AppLockService
import com.example.appenergytracker.service.EnergyMonitorService
import com.example.appenergytracker.service.AppLockAccessibilityService
import com.example.appenergytracker.viewmodel.EnergyViewModel
import kotlinx.coroutines.delay

@Composable
fun TestScreen() {
    var currentEnergy by remember { mutableStateOf(0) }
    var currentApp by remember { mutableStateOf("未知") }
    var serviceStatus by remember { mutableStateOf("") }
    var testLog by remember { mutableStateOf("") }
    
    val context = LocalContext.current
    val appLockService = remember { AppLockService.getInstance(context) }
    val energyMonitorService = remember { appLockService.getEnergyMonitorService() }
    val energyViewModel: EnergyViewModel = viewModel()
    
    // 定期更新狀態
    LaunchedEffect(Unit) {
        while (true) {
            currentEnergy = energyMonitorService.currentEnergy.value
            currentApp = energyMonitorService.getCurrentForegroundApp() ?: "未知"
            serviceStatus = appLockService.getServiceStatus()
            delay(1000)
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "即時監控測試",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )
        
        // 當前狀態卡片
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "當前狀態",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text("當前能量: $currentEnergy 分鐘")
                Text("當前 App: $currentApp")
                Text("服務狀態: $serviceStatus")
            }
        }
        
        // 測試功能按鈕
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "測試功能",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(16.dp))
                
                // 模擬能量消耗
                Button(
                    onClick = {
                        // 使用 EnergyMonitorService 的測試方法
                        energyMonitorService.simulateEnergyConsumption(10)
                        testLog += "已模擬消耗 10 點能量\n"
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("模擬消耗能量 (快速測試)")
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // 重置能量
                Button(
                    onClick = {
                        // 使用 EnergyMonitorService 的測試方法
                        energyMonitorService.resetEnergy(180)
                        testLog += "已重置能量為 180\n"
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("重置能量為最大值")
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // 立即阻止當前 App
                Button(
                    onClick = {
                        val accessibilityService = AppLockAccessibilityService.getInstance()
                        if (accessibilityService != null) {
                            accessibilityService.immediatelyBlockCurrentApp(currentApp)
                            testLog += "已嘗試立即阻止當前 App: $currentApp\n"
                        } else {
                            testLog += "無障礙服務未運行，無法阻止 App\n"
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("立即阻止當前 App")
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // 重新載入配置
                Button(
                    onClick = {
                        energyMonitorService.reloadHabitAppsConfig()
                        val accessibilityService = AppLockAccessibilityService.getInstance()
                        accessibilityService?.reloadBadHabitAppsCache()
                        testLog += "已重新載入習慣 App 配置\n"
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("重新載入習慣 App 配置")
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // 測試充電功能
                Button(
                    onClick = {
                        energyViewModel.insertCharge("測試充電", 5, 2.0f)
                        testLog += "已測試充電功能：5分鐘活動，2.0倍率\n"
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                ) {
                    Text("測試充電功能 (+10能量)")
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // 清除測試日誌
                Button(
                    onClick = {
                        testLog = ""
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("清除測試日誌")
                }
            }
        }
        
        // 測試日誌
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "測試日誌",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = testLog.ifEmpty { "尚未執行測試..." },
                    fontSize = 12.sp,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
        
        // 使用說明
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "使用說明",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = """
                        1. 確保已啟用無障礙服務
                        2. 在設定中將某個 App 設為壞習慣
                        3. 使用「模擬消耗能量」快速測試
                        4. 當能量歸零時，系統會立即阻止壞習慣 App
                        5. 觀察「立即阻止當前 App」的效果
                    """.trimIndent(),
                    fontSize = 12.sp
                )
            }
        }
    }
}
