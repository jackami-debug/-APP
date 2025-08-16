package com.example.appenergytracker.ui.screens

import android.content.Intent
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

// 已知應排除之系統背景套件（不具統計意義）
private val DEFAULT_EXCLUDED_PACKAGES = setOf(
    "com.android.systemui",
    "com.google.android.gms",
    // 常見桌面（部分品牌可能會由動態偵測補齊）
    "com.miui.home",
    "com.mi.android.globallauncher",
    "com.google.android.apps.nexuslauncher",
    "com.sec.android.app.launcher",
    "net.oneplus.launcher",
    "com.huawei.android.launcher",
    "com.coloros.launcher",
    "com.oppo.launcher",
    "com.vivo.launcher",
    "com.android.launcher",
    "com.android.launcher3",
    "org.lineageos.trebuchet"
)

private fun queryLauncherPackages(pm: PackageManager): Set<String> {
    val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
    return pm.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)
        .mapNotNull { it.activityInfo?.packageName }
        .toSet()
}

private fun formatUsageMinutes(minutes: Int): String {
    if (minutes < 60) return "${minutes} 分鐘"
    val hours = minutes / 60
    val remain = minutes % 60
    return if (remain == 0) "${hours} 小時" else "${hours} 小時 ${remain} 分鐘"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(navController: NavController) {
    val context = LocalContext.current
    val dao = remember { AppDatabase.getDatabase(context).appDao() }
    val viewModel: GoodHabitViewModel = remember { GoodHabitViewModel(context.applicationContext as android.app.Application) }
    val scope = rememberCoroutineScope()
    val pm = remember { context.packageManager }
    val launcherPackages = remember { queryLauncherPackages(pm) }
    val excludedPackages = remember { DEFAULT_EXCLUDED_PACKAGES + launcherPackages }
    
    var usage by remember { mutableStateOf(emptyList<com.example.appenergytracker.data.entity.AppUsageLog>()) }
    var charges by remember { mutableStateOf(emptyList<com.example.appenergytracker.data.entity.ChargeLog>()) }
    val goodHabitApps by viewModel.goodHabitApps.collectAsState()
    val today = remember { DateUtils.today() }
    var selectedDate by remember { mutableStateOf(today) }
    var earliestDate by remember { mutableStateOf<String?>(null) }
    
    // App 圖示快取
    val appIcons = remember { mutableStateMapOf<String, ImageBitmap?>() }

    LaunchedEffect(selectedDate) {
        combine(
            dao.getUsageLogsByDate(selectedDate),
            dao.getChargeLogsByDate(selectedDate)
        ) { u, c -> u to c }
            .collectLatest { (u, c) ->
                usage = u
                charges = c
            }
    }

    // 取得最早紀錄日期（僅取一次，或當畫面建立時）
    LaunchedEffect(Unit) {
        val uMin = runCatching { dao.getEarliestUsageDate() }.getOrNull()
        val cMin = runCatching { dao.getEarliestChargeDate() }.getOrNull()
        earliestDate = listOfNotNull(uMin, cMin).minOrNull()
    }

    // 彙總使用紀錄（同一 App 僅一筆，分鐘數相加）
    data class UsageAgg(val packageName: String, val appName: String, val minutes: Int)
    val aggregatedUsage by remember(usage) {
        mutableStateOf(
            usage
                .filter { it.packageName !in excludedPackages }
                .groupBy { it.packageName }
                .map { (pkg, logs) ->
                    val minutes = logs.sumOf { it.usageMinutes }
                    val name = logs.first().appName
                    UsageAgg(pkg, name, minutes)
                }
        )
    }

    // 載入 App 圖示（依彙總後的套件清單）
    LaunchedEffect(aggregatedUsage) {
        aggregatedUsage.forEach { item ->
            if (!appIcons.containsKey(item.packageName)) {
                val icon = runCatching {
                    pm.getApplicationIcon(item.packageName).toBitmap().asImageBitmap()
                }.getOrNull()
                appIcons[item.packageName] = icon
            }
        }
    }

    // 計算統計數據
    val totalUsageMinutes = aggregatedUsage.sumOf { it.minutes }
    
    // 分類統計
    val goodHabitUsage = aggregatedUsage.filter { item ->
        goodHabitApps.any { habitApp -> habitApp.packageName == item.packageName && habitApp.isGoodHabit }
    }
    val badHabitUsage = aggregatedUsage.filter { item ->
        goodHabitApps.any { habitApp -> habitApp.packageName == item.packageName && habitApp.isBadHabit }
    }
    val neutralUsage = aggregatedUsage.filter { item ->
        goodHabitApps.none { habitApp -> habitApp.packageName == item.packageName }
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
            // 已移除「今日概況」卡片

            // 使用時間長條圖
            if (aggregatedUsage.isNotEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "📊 使用時間統計",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    IconButton(
                                        onClick = { selectedDate = DateUtils.addDays(selectedDate, -1) },
                                        enabled = earliestDate?.let { selectedDate > it } ?: true
                                    ) {
                                        Icon(Icons.Default.KeyboardArrowLeft, contentDescription = "前一天")
                                    }
                                    Text(
                                        text = DateUtils.formatWithWeekday(selectedDate),
                                        fontSize = 14.sp
                                    )
                                    IconButton(
                                        onClick = { selectedDate = DateUtils.addDays(selectedDate, +1) },
                                        enabled = !DateUtils.isToday(selectedDate)
                                    ) {
                                        Icon(Icons.Default.KeyboardArrowRight, contentDescription = "後一天")
                                    }
                                }
                            }
                            
                            val maxUsage = max(1, aggregatedUsage.maxOfOrNull { it.minutes } ?: 1)
                            
                            aggregatedUsage.sortedByDescending { it.minutes }.forEach { item ->
                                UsageBarItem(
                                    appName = item.appName,
                                    packageName = item.packageName,
                                    usageMinutes = item.minutes,
                                    maxUsage = maxUsage,
                                    icon = appIcons[item.packageName],
                                    isGoodHabit = goodHabitApps.any { it.packageName == item.packageName && it.isGoodHabit },
                                    isBadHabit = goodHabitApps.any { it.packageName == item.packageName && it.isBadHabit }
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
                            minutes = goodHabitUsage.sumOf { it.minutes },
                            count = goodHabitUsage.size,
                            color = Color(0xFF4CAF50),
                            icon = Icons.Default.ThumbUp
                        )
                        
                        CategoryStatItem(
                            title = "壞習慣 App",
                            minutes = badHabitUsage.sumOf { it.minutes },
                            count = badHabitUsage.size,
                            color = Color(0xFFF44336),
                            icon = Icons.Default.ThumbDown
                        )
                        
                        CategoryStatItem(
                            title = "一般 App",
                            minutes = (totalUsageMinutes - goodHabitUsage.sumOf { it.minutes } - badHabitUsage.sumOf { it.minutes }).coerceAtLeast(0),
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
            if (aggregatedUsage.isEmpty() && charges.isEmpty()) {
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
                text = formatUsageMinutes(usageMinutes),
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
            text = formatUsageMinutes(minutes),
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
