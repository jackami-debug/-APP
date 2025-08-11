package com.example.appenergytracker.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import androidx.navigation.NavController
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.appenergytracker.viewmodel.EnergyViewModel
import androidx.compose.ui.platform.LocalContext
import com.example.appenergytracker.data.UsageStatsRepository
import com.example.appenergytracker.util.UsageAccess
import kotlinx.coroutines.launch

@Composable
fun MainScreen(navController: NavController) {
    val energyViewModel: EnergyViewModel = viewModel()
    val maxEnergy by energyViewModel.maxEnergyMinutes.collectAsState()
    val currentEnergy by energyViewModel.currentEnergyMinutes.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(32.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "目前能量：$currentEnergy / $maxEnergy 分鐘",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )

        EnergyBar(current = currentEnergy, max = maxEnergy)

        val context = LocalContext.current
        val hasAccess = remember { mutableStateOf(UsageAccess.hasUsageAccess(context)) }

        if (!hasAccess.value) {
            Text("需要開啟使用情況存取權限以自動記錄 App 使用時間")
            Button(onClick = { UsageAccess.openUsageAccessSettings(context) }) {
                Text("前往開啟權限")
            }
        } else {
            val scope = rememberCoroutineScope()
            Button(onClick = {
                val repo = UsageStatsRepository(context)
                scope.launch {
                    repo.collectTodayUsageAndStore()
                }
            }) { Text("刷新今日使用紀錄") }
        }

        Button(
            onClick = {
                navController.navigate("history")
            }
            ,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("📜 歷史紀錄")
        }

        Button(
            onClick = {
                navController.navigate("charge")
            }
            ,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("⚡ 充能活動")
        }

        Button(
            onClick = {
                navController.navigate("settings")
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("⚙️ 設定")
        }

    }
}

@Composable
fun EnergyBar(current: Int, max: Int) {
    val progress = current.toFloat() / max.toFloat()

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(24.dp)
            .background(color = Color.LightGray, shape = RoundedCornerShape(12.dp))
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(progress)
                .background(color = Color(0xFF4CAF50), shape = RoundedCornerShape(12.dp))
        )
    }
}
