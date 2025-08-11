package com.example.appenergytracker.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.appenergytracker.viewmodel.EnergyViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChargeScreen(navController: NavController) {
    val energyViewModel: EnergyViewModel = viewModel()
    val activityTypes = listOf("讀書", "運動", "做家事", "冥想", "其他")
    var selectedType by remember { mutableStateOf(activityTypes.first()) }
    var ratioText by remember { mutableStateOf("1.0") }
    var isTiming by remember { mutableStateOf(false) }
    var elapsedSeconds by remember { mutableStateOf(0) }
    var showResult by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("⚡ 充能活動") }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(24.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("選擇活動類型", fontSize = 18.sp)

            // 活動下拉選單
            var expanded by remember { mutableStateOf(false) }
            Box {
                OutlinedButton(onClick = { expanded = true }) {
                    Text(selectedType)
                }
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    activityTypes.forEach { type ->
                        DropdownMenuItem(
                            text = { Text(type) },
                            onClick = {
                                selectedType = type
                                expanded = false
                            }
                        )
                    }
                }
            }

            OutlinedTextField(
                value = ratioText,
                onValueChange = { ratioText = it },
                label = { Text("每分鐘換多少能量") },
                singleLine = true
            )

            if (!isTiming) {
                Button(
                    onClick = {
                        isTiming = true
                        showResult = false
                        elapsedSeconds = 0
                        coroutineScope.launch {
                            while (isTiming) {
                                delay(1000)
                                elapsedSeconds++
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("開始計時")
                }
            } else {
                Button(
                    onClick = {
                        isTiming = false
                        showResult = true
                        val ratio = ratioText.toFloatOrNull() ?: 1f
                        val minutes = (elapsedSeconds / 60f).toInt()
                        if (minutes > 0) {
                            energyViewModel.insertCharge(
                                activityType = selectedType,
                                durationMinutes = minutes,
                                ratio = ratio
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("結束計時")
                }
            }

            Text("已進行時間：${elapsedSeconds / 60} 分 ${elapsedSeconds % 60} 秒")

            if (showResult) {
                val ratio = ratioText.toFloatOrNull() ?: 1f
                val earned = (elapsedSeconds / 60f) * ratio
                Text("🌟 獲得能量：${"%.1f".format(earned)} 分鐘")
            }
        }
    }
}
