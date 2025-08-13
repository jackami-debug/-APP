package com.example.appenergytracker.ui.screens

import android.content.pm.PackageManager
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import androidx.navigation.NavController
import com.example.appenergytracker.data.database.AppDatabase
import com.example.appenergytracker.util.DateUtils
import com.example.appenergytracker.viewmodel.GoodHabitViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlin.math.max

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(navController: NavController) {
    val context = LocalContext.current
    val dao = remember { AppDatabase.getDatabase(context).appDao() }
    val viewModel: GoodHabitViewModel = remember { GoodHabitViewModel(context.applicationContext as android.app.Application) }
    val scope = rememberCoroutineScope()
    val pm = remember { context.packageManager }
    
    var usage by remember { mutableStateOf(emptyList<com.example.appenergytracker.data.entity.AppUsageLog>()) }
    var charges by remember { mutableStateOf(emptyList<com.example.appenergytracker.data.entity.ChargeLog>()) }
    val goodHabitApps by viewModel.goodHabitApps.collectAsState()
    val today = remember { DateUtils.today() }
    
    // App 圖示快取
    val appIcons = remember { mutableStateMapOf<String, ImageBitmap?>() }

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

    // 載入 App 圖示
    LaunchedEffect(usage) {
        usage.forEach { log ->
            if (!appIcons.containsKey(log.packageName)) {
                val icon = runCatching { 
                    pm.getApplicationIcon(log.packageName).toBitmap().asImageBitmap() 
                }.getOrNull()
                appIcons[log.packageName] = icon
            }
        }
    }

    // 計算統計數據
    val totalUsageMinutes = usage.sumOf { log -> log.usageMinutes }
    val totalChargeMinutes = charges.sumOf { charge -> (charge.durationMinutes * charge.ratio).toInt() }
    val netEnergy = totalChargeMinutes - totalUsageMinutes
    
    // 分類統計
    val goodHabitUsage = usage.filter { log ->
        goodHabitApps.any { habitApp -> habitApp.packageName == log.packageName && habitApp.isGoodHabit }
    }
    val badHabitUsage = usage.filter { log ->
        goodHabitApps.any { habitApp -> habitApp.packageName == log.packageName && habitApp.isBadHabit }
    }
    val neutralUsage = usage.filter { log ->
        goodHabitApps.none { habitApp -> habitApp.packageName == log.packageName }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("📊 歷史紀錄") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(16.dp)
        ) {
            // 今日概覽卡片
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "📈 今日概覽",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            StatItem(
                                icon = Icons.Default.Timer,
                                label = "總使用時間",
                                value = "${totalUsageMinutes}分鐘",
                                color = MaterialTheme.colorScheme.error
                            )
                            StatItem(
                                icon = Icons.Default.BatteryChargingFull,
                                label = "總充能時間",
                                value = "${totalChargeMinutes}分鐘",
                                color = MaterialTheme.colorScheme.primary
                            )
                            StatItem(
                                icon = Icons.Default.TrendingUp,
                                label = "淨能量",
                                value = "${netEnergy}分鐘",
                                color = if (netEnergy >= 0) Color(0xFF4CAF50) else Color(0xFFF44336)
                            )
                        }
                    }
                }
            }

            // 使用時間長條圖
            if (usage.isNotEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "📊 使用時間統計",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            
                            val maxUsage = max(1, usage.maxOfOrNull { log -> log.usageMinutes } ?: 1)
                            
                            usage.sortedByDescending { log -> log.usageMinutes }.forEach { log ->
                                UsageBarItem(
                                    appName = log.appName,
                                    packageName = log.packageName,
                                    usageMinutes = log.usageMinutes,
                                    maxUsage = maxUsage,
                                    icon = appIcons[log.packageName],
                                    isGoodHabit = goodHabitApps.any { it.packageName == log.packageName && it.isGoodHabit },
                                    isBadHabit = goodHabitApps.any { it.packageName == log.packageName && it.isBadHabit }
                                )
                            }
                        }
                    }
                }
            }

            // 分類統計
            item {
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "🏷️ 分類統計",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        
                        CategoryStatItem(
                            title = "好習慣 App",
                            minutes = goodHabitUsage.sumOf { log -> log.usageMinutes },
                            count = goodHabitUsage.size,
                            color = Color(0xFF4CAF50),
                            icon = Icons.Default.ThumbUp
                        )
                        
                        CategoryStatItem(
                            title = "壞習慣 App",
                            minutes = badHabitUsage.sumOf { log -> log.usageMinutes },
                            count = badHabitUsage.size,
                            color = Color(0xFFF44336),
                            icon = Icons.Default.ThumbDown
                        )
                        
                        CategoryStatItem(
                            title = "一般 App",
                            minutes = neutralUsage.sumOf { log -> log.usageMinutes },
                            count = neutralUsage.size,
                            color = Color(0xFF9E9E9E),
                            icon = Icons.Default.Apps
                        )
                    }
                }
            }

            // 充能紀錄
            if (charges.isNotEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "⚡ 充能紀錄",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            
                            charges.forEach { charge ->
                                val earned = (charge.durationMinutes * charge.ratio)
                                ChargeItem(
                                    activityType = charge.activityType,
                                    durationMinutes = charge.durationMinutes,
                                    ratio = charge.ratio,
                                    earnedMinutes = earned
                                )
                            }
                        }
                    }
                }
            }

            // 空狀態
            if (usage.isEmpty() && charges.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.History,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "今日尚無使用紀錄",
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = "開始使用 App 或進行充能活動來查看統計",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(24.dp)
        )
        Text(
            text = value,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = label,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun UsageBarItem(
    appName: String,
    packageName: String,
    usageMinutes: Int,
    maxUsage: Int,
    icon: ImageBitmap?,
    isGoodHabit: Boolean,
    isBadHabit: Boolean
) {
    val backgroundColor = when {
        isGoodHabit -> Color(0xFFE8F5E8)
        isBadHabit -> Color(0xFFFFF0F0)
        else -> MaterialTheme.colorScheme.surfaceVariant
    }
    
    val barColor = when {
        isGoodHabit -> Color(0xFF4CAF50)
        isBadHabit -> Color(0xFFF44336)
        else -> MaterialTheme.colorScheme.primary
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(backgroundColor)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // App 圖示
        if (icon != null) {
            androidx.compose.foundation.Image(
                bitmap = icon,
                contentDescription = null,
                modifier = Modifier.size(32.dp)
            )
        } else {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = appName.firstOrNull()?.uppercase() ?: "?",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // App 名稱和時間
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = appName,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = "${usageMinutes} 分鐘",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // 長條圖
        Box(
            modifier = Modifier
                .width(80.dp)
                .height(24.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            val progress = usageMinutes.toFloat() / maxUsage
            Canvas(
                modifier = Modifier.fillMaxSize()
            ) {
                drawRoundRect(
                    color = barColor,
                    topLeft = Offset(0f, 0f),
                    size = androidx.compose.ui.geometry.Size(
                        width = size.width * progress,
                        height = size.height
                    ),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(12.dp.toPx())
                )
            }
        }
    }
}

@Composable
private fun CategoryStatItem(
    title: String,
    minutes: Int,
    count: Int,
    color: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(24.dp)
        )
        
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = "$count 個 App",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        Text(
            text = "${minutes} 分鐘",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

@Composable
private fun ChargeItem(
    activityType: String,
    durationMinutes: Int,
    ratio: Float,
    earnedMinutes: Float
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFFE3F2FD))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = Icons.Default.BatteryChargingFull,
            contentDescription = null,
            tint = Color(0xFF2196F3),
            modifier = Modifier.size(24.dp)
        )
        
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = activityType,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = "實際時間: ${durationMinutes} 分鐘 (比例: ${ratio}x)",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        Text(
            text = "+${"%.1f".format(earnedMinutes)} 分鐘",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF2196F3)
        )
    }
}
