package com.example.appenergytracker.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.navigation.NavController
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.appenergytracker.viewmodel.EnergyViewModel
import androidx.compose.ui.platform.LocalContext
import com.example.appenergytracker.data.UsageStatsRepository
import com.example.appenergytracker.util.UsageAccess
import com.example.appenergytracker.util.AccessibilityUtils
import com.example.appenergytracker.util.OverlayUtils
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import androidx.compose.runtime.saveable.rememberSaveable


@Composable
fun MainScreen(navController: NavController) {
    val energyViewModel: EnergyViewModel = viewModel()
    val maxEnergy by energyViewModel.maxEnergyMinutes.collectAsState()
    val currentEnergy by energyViewModel.currentEnergyMinutes.collectAsState()

    // 增加自動刷新機制
    LaunchedEffect(Unit) {
        while (true) {
            // 每5秒強制刷新一次能量狀態
            delay(5000)
        }
    }

    // 隱藏開發測試按鈕的解鎖狀態
    var showTestButton by rememberSaveable { mutableStateOf(false) }
    var secretTapCount by remember { mutableStateOf(0) }

    LaunchedEffect(secretTapCount) {
        if (secretTapCount >= 5) {
            showTestButton = true
            secretTapCount = 0
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
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

            // 能量狀態提示
            if (currentEnergy <= 0) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFD32F2F)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                        Column {
                            Text(
                                text = "能量已歸零！",
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "壞習慣 App 已被鎖定，請充電後再使用",
                                color = Color.White.copy(alpha = 0.9f),
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            } else if (currentEnergy < maxEnergy * 0.3) {
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
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = Color(0xFFFF9800)
                        )
                        Text(
                            text = "能量偏低，建議減少壞習慣 App 使用",
                            color = Color(0xFFFF9800),
                            fontSize = 14.sp
                        )
                    }
                }
            }

            val context = LocalContext.current

            // 使用 LaunchedEffect 來定期檢查權限狀態
            var hasUsageAccess by remember { mutableStateOf(false) }
            var hasAccessibilityAccess by remember { mutableStateOf(false) }
            var hasOverlayAccess by remember { mutableStateOf(false) }

            LaunchedEffect(Unit) {
                while (true) {
                    hasUsageAccess = UsageAccess.hasUsageAccess(context)
                    hasAccessibilityAccess =
                        AccessibilityUtils.isAccessibilityServiceEnabled(context)
                    hasOverlayAccess = OverlayUtils.isOverlayPermissionGranted(context)
                    delay(1000) // 每秒檢查一次權限狀態
                }
            }

            if (!hasUsageAccess) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = Color(0xFFFF9800)
                        )
                        Text(
                            text = "需要開啟使用情況存取權限",
                            color = Color(0xFFFF9800),
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "以自動監控 App 使用時間和能量消耗",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center
                        )
                        Button(
                            onClick = { UsageAccess.openUsageAccessSettings(context) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9800))
                        ) {
                            Text("前往開啟權限")
                        }
                    }
                }
            } else if (!hasAccessibilityAccess) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = Color(0xFFFF9800)
                        )
                        Text(
                            text = "需要開啟無障礙服務權限",
                            color = Color(0xFFFF9800),
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "以啟用 App 鎖定功能",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center
                        )
                        Button(
                            onClick = { AccessibilityUtils.openAccessibilitySettings(context) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9800))
                        ) {
                            Text("前往開啟權限")
                        }
                    }
                }
            } else if (!hasOverlayAccess) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = Color(0xFFFF9800)
                        )
                        Text(
                            text = "需要開啟懸浮視窗權限",
                            color = Color(0xFFFF9800),
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "以顯示能量倒數計時小圖示",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center
                        )
                        Button(
                            onClick = { OverlayUtils.openOverlayPermissionSettings(context) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9800))
                        ) {
                            Text("前往開啟權限")
                        }
                    }
                }
            } else {
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
                            text = "自動監控已啟用",
                            color = Color(0xFF4CAF50),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Button(
                onClick = {
                    navController.navigate("history")
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("📜 歷史紀錄")
            }

            Button(
                onClick = {
                    navController.navigate("charge")
                },
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

            // 測試按鈕（開發用）
            if (showTestButton) {
                Button(
                    onClick = {
                        navController.navigate("test")
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF9C27B0))
                ) {
                    Text("🧪 即時監控測試")
                }
            }

        }
        // 右上角隱形點擊區（連點 5 下解鎖）
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .size(48.dp)
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) {
                    secretTapCount++
                }
        )
    }
}

@Composable
fun EnergyBar(current: Int, max: Int) {
        val progress = (current.toFloat() / max.toFloat()).coerceIn(0f, 1f)

        // 根據能量水平決定顏色
        val barColor = when {
            current <= 0 -> Color(0xFFD32F2F) // 紅色：能量耗盡
            current < max * 0.3 -> Color(0xFFFF9800) // 橙色：能量偏低
            current < max * 0.7 -> Color(0xFFFFEB3B) // 黃色：能量中等
            else -> Color(0xFF4CAF50) // 綠色：能量充足
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(24.dp)
                .background(color = Color.LightGray, shape = RoundedCornerShape(12.dp))
                .padding(2.dp) // 外邊距確保邊界清晰
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(progress)
                    .background(color = barColor, shape = RoundedCornerShape(10.dp))
            )
        }
    }
