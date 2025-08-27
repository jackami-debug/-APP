package com.example.appenergytracker.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.appenergytracker.viewmodel.EnergyViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.ui.platform.LocalContext
import android.content.Context

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
    
    // 免費充能冷卻時間相關狀態
    var freeChargeCooldown by remember { mutableStateOf(0) } // 剩餘冷卻秒數
    var isFreeChargeLocked by remember { mutableStateOf(false) } // 是否鎖定
    var showFreeChargeSuccess by remember { mutableStateOf(false) } // 是否顯示成功訊息
    var freeChargeExpiry by remember { mutableStateOf(0L) } // 冷卻截止時間（毫秒）

    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("charge_prefs", Context.MODE_PRIVATE) }

    val coroutineScope = rememberCoroutineScope()

    // 初次進入頁面時，還原冷卻狀態
    LaunchedEffect(Unit) {
        val savedExpiry = prefs.getLong("free_charge_cooldown_until", 0L)
        freeChargeExpiry = savedExpiry
        val now = System.currentTimeMillis()
        val remaining = ((savedExpiry - now) / 1000).toInt()
        if (remaining > 0) {
            isFreeChargeLocked = true
            freeChargeCooldown = remaining
        } else {
            isFreeChargeLocked = false
            freeChargeCooldown = 0
        }
    }

    // 根據到期時間每秒刷新剩餘倒數，時間到自動解鎖
    LaunchedEffect(isFreeChargeLocked, freeChargeExpiry) {
        if (isFreeChargeLocked && freeChargeExpiry > 0L) {
            while (true) {
                val now = System.currentTimeMillis()
                val remaining = ((freeChargeExpiry - now) / 1000).toInt()
                if (remaining <= 0) {
                    isFreeChargeLocked = false
                    freeChargeCooldown = 0
                    freeChargeExpiry = 0L
                    prefs.edit().remove("free_charge_cooldown_until").apply()
                    break
                } else {
                    freeChargeCooldown = remaining
                }
                delay(1000)
            }
        }
    }

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

            // 免費充能按鈕
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "🎁 免費充能",
                        fontSize = 16.sp,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Text(
                        text = "特殊情況下可獲得 10 分鐘能量",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    
                    // 成功訊息
                    if (showFreeChargeSuccess) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E8)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = Color(0xFF4CAF50)
                                )
                                Text(
                                    text = "成功獲得 10 分鐘能量！",
                                    color = Color(0xFF4CAF50),
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                    
                    // 冷卻時間顯示
                    if (isFreeChargeLocked) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Timer,
                                    contentDescription = null,
                                    tint = Color(0xFFFF9800)
                                )
                                Text(
                                    text = "冷卻中：${freeChargeCooldown / 60}:${String.format("%02d", freeChargeCooldown % 60)}",
                                    color = Color(0xFFFF9800),
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                    
                    Button(
                        onClick = {
                            if (!isFreeChargeLocked) {
                                // 執行免費充能
                                energyViewModel.insertCharge(
                                    activityType = "免費充能",
                                    durationMinutes = 10,
                                    ratio = 1.0f
                                )
                                
                                // 顯示成功訊息
                                showFreeChargeSuccess = true
                                
                                // 啟動冷卻時間（以到期時間持久化）
                                isFreeChargeLocked = true
                                val expiry = System.currentTimeMillis() + 15 * 60 * 1000
                                freeChargeExpiry = expiry
                                prefs.edit().putLong("free_charge_cooldown_until", expiry).apply()
                                
                                // 3秒後隱藏成功訊息
                                coroutineScope.launch {
                                    kotlinx.coroutines.delay(3000)
                                    showFreeChargeSuccess = false
                                }
                                
                                // 倒數由上方 LaunchedEffect 根據到期時間自動刷新
                            }
                        },
                        enabled = !isFreeChargeLocked,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isFreeChargeLocked) 
                                MaterialTheme.colorScheme.surfaceVariant 
                            else 
                                MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(
                            imageVector = if (isFreeChargeLocked) Icons.Default.Lock else Icons.Default.CardGiftcard, 
                            contentDescription = null
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                                                        Text(if (isFreeChargeLocked) "冷卻中..." else "獲得 10 分鐘能量")
                    }
                }
            }

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
                label = { Text("每分鐘獲取多少能量") },
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
