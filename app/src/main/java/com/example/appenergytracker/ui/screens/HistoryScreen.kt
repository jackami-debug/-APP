package com.example.appenergytracker.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.compose.ui.platform.LocalContext
import com.example.appenergytracker.data.database.AppDatabase
import com.example.appenergytracker.util.DateUtils
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(navController: NavController) {
    val context = LocalContext.current
    val dao = remember { AppDatabase.getDatabase(context).appDao() }
    val scope = rememberCoroutineScope()
    var usage by remember { mutableStateOf(emptyList<com.example.appenergytracker.data.entity.AppUsageLog>()) }
    var charges by remember { mutableStateOf(emptyList<com.example.appenergytracker.data.entity.ChargeLog>()) }
    val today = remember { DateUtils.today() }

    LaunchedEffect(today) {
        combine(
            dao.getUsageLogsByDate(today),
            dao.getChargeLogsByDate(today)
        ) { u, c -> u to c }
            .collectLatest { (u, c) ->
                usage = u
                charges = c
            }
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("📜 歷史紀錄") }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "🧾 今日使用紀錄",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )

            if (usage.isEmpty()) {
                Text("（今日尚無使用紀錄）")
            } else {
                usage.forEach { log ->
                    Text("${log.appName} - ${log.usageMinutes} 分鐘")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "⚡ 今日充能紀錄",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )

            if (charges.isEmpty()) {
                Text("（今日尚無充能紀錄）")
            } else {
                charges.forEach { c ->
                    val earned = (c.durationMinutes * c.ratio)
                    Text("${c.activityType} - +${"%.1f".format(earned)} 分鐘")
                }
            }
        }
    }
}
