package com.example.appenergytracker.ui.screens

import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.appenergytracker.model.GoodHabitApp
import com.example.appenergytracker.ui.screens.components.GoodHabitAppItem
import com.example.appenergytracker.viewmodel.GoodHabitViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SettingScreen() {
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("壞習慣 App", "好習慣 App")

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val energyViewModel: com.example.appenergytracker.viewmodel.EnergyViewModel = viewModel()

    Scaffold(
        topBar = { TopAppBar(title = { Text("設定") }) },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            TabRow(selectedTabIndex = selectedTab) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title) }
                    )
                }
            }
            
            // 清除歷史記錄按鈕
            Button(
                onClick = {
                    energyViewModel.clearAllHistory()
                    scope.launch {
                        snackbarHostState.showSnackbar("已清除所有歷史記錄")
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Clear,
                    contentDescription = null
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("清除所有歷史記錄")
            }
            
            // 重置鎖定狀態按鈕
            val context = LocalContext.current
            Button(
                onClick = {
                    val appLockService = com.example.appenergytracker.service.AppLockService.getInstance(context)
                    appLockService.resetLockingState()
                    scope.launch {
                        snackbarHostState.showSnackbar("已重置鎖定狀態")
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.tertiary
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = null
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("重置鎖定狀態")
            }
            
            // 檢查無障礙服務狀態按鈕
            Button(
                onClick = {
                    val accessibilityService = com.example.appenergytracker.service.AppLockAccessibilityService.getInstance()
                    val isRunning = accessibilityService?.isServiceRunning() ?: false
                    val message = if (isRunning) "無障礙服務正在運行" else "無障礙服務未運行"
                    scope.launch {
                        snackbarHostState.showSnackbar(message)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondary
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("檢查無障礙服務狀態")
            }
            
            // 檢查 AppLockService 狀態按鈕
            Button(
                onClick = {
                    val appLockService = com.example.appenergytracker.service.AppLockService.getInstance(context)
                    val status = appLockService.getServiceStatus()
                    scope.launch {
                        snackbarHostState.showSnackbar("AppLockService: $status")
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("檢查 AppLockService 狀態")
            }

            when (selectedTab) {
                0 -> BadHabitAppSection(snackbarHostState = snackbarHostState)
                1 -> GoodHabitAppSection(snackbarHostState = snackbarHostState)
            }
        }
    }
}

@Composable
private fun GeneralSettingsContent() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("一般設定", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        Text("（這裡放你原本的設定選項…）")
    }
}

/** 好習慣 App 分頁，加入「重新掃描」與「插入測試 App」兩個可視化驗證按鈕 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun GoodHabitAppSection(
    snackbarHostState: SnackbarHostState
) {
    val scope = rememberCoroutineScope()
    val viewModel: GoodHabitViewModel = viewModel()
    val context = LocalContext.current
    val pm: PackageManager = context.packageManager

    val goodHabitApps by viewModel.goodHabitApps.collectAsState()

    // ✅ 用 packageName 當 key
    val appIcons = remember { mutableStateMapOf<String, ImageBitmap?>() }

    var searchQuery by remember { mutableStateOf(TextFieldValue("")) }
    var lastUpdated by remember { mutableStateOf("—") } // 不自動掃描，初始為「—」
    var isRescanning by remember { mutableStateOf(false) } // 重新掃描狀態

    // 進到畫面即嘗試為目前資料庫中的 App 載入圖示（僅補缺）
    LaunchedEffect(goodHabitApps) {
        withContext(Dispatchers.IO) {
            goodHabitApps.forEach { app ->
                if (!appIcons.containsKey(app.packageName)) {
                    val bmp = runCatching { pm.getApplicationIcon(app.packageName).toBitmap().asImageBitmap() }
                        .getOrNull()
                    withContext(Dispatchers.Main) {
                        appIcons[app.packageName] = bmp
                    }
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // 上方工具列：左按鈕，右側兩行資訊
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = {
                    if (!isRescanning) {
                        scope.launch {
                            isRescanning = true
                            rescanAndReload(
                                pm = pm,
                                appIcons = appIcons,
                                viewModel = viewModel,
                                onDone = { count ->
                                    lastUpdated = currentTimeText()
                                    isRescanning = false
                                    // 這行是 suspend，要再包一層 coroutine
                                    scope.launch {
                                        snackbarHostState.showSnackbar("重新掃描完成：$count 筆")
                                    }
                                }
                            )
                        }
                    }
                },
                enabled = !isRescanning
            ) {
                if (isRescanning) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Icon(Icons.Default.Refresh, contentDescription = null)
                }
                Spacer(Modifier.width(6.dp))
                Text(if (isRescanning) "掃描中..." else "重新掃描")
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "目前筆數：${goodHabitApps.size}",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "最後更新：$lastUpdated",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        // 顯示已加入的好習慣 App
        val selectedGoodApps = goodHabitApps.filter { app -> app.isGoodHabit }
        if (selectedGoodApps.isNotEmpty()) {
            Text(
                text = "已加入的好習慣 App：",
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
            )
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                selectedGoodApps.forEach { app ->
                    var longPressTriggered by remember { mutableStateOf(false) }
                    val interactionSource = remember { MutableInteractionSource() }
                    val isPressed by interactionSource.collectIsPressedAsState()
                    
                    LaunchedEffect(isPressed) {
                        if (isPressed && !longPressTriggered) {
                            delay(500) // 500ms 長按
                            if (isPressed) {
                                longPressTriggered = true
                                searchQuery = TextFieldValue(app.name)
                            }
                        } else if (!isPressed) {
                            longPressTriggered = false
                        }
                    }
                     
                     AssistChip(
                         onClick = { },
                         interactionSource = interactionSource,
                         label = { Text(app.name) },
                         leadingIcon = {
                             Icon(
                                 imageVector = Icons.Default.Check,
                                 contentDescription = null,
                                 tint = MaterialTheme.colorScheme.primary
                             )
                         }
                     )
                }
            }
        }

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            label = { Text("搜尋應用程式") },
            modifier = Modifier.fillMaxWidth(),
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "搜尋") },
            trailingIcon = {
                if (searchQuery.text.isNotEmpty()) {
                    IconButton(
                        onClick = { searchQuery = TextFieldValue("") }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "清除搜尋"
                        )
                    }
                }
            },
            supportingText = { Text("💡 長按上方已選取的 App 可快速搜尋") }
        )

        Spacer(Modifier.height(8.dp))

        val filteredApps = remember(goodHabitApps, searchQuery) {
            goodHabitApps.filter { app ->
                app.name.contains(searchQuery.text, ignoreCase = true) ||
                        app.packageName.contains(searchQuery.text, ignoreCase = true)
            }.sortedBy { app -> app.name }
        }

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(filteredApps, key = { app -> app.packageName }) { app ->
                GoodHabitAppItem(
                    app = app,
                    icon = appIcons.getOrElse(app.packageName) {
                        // 若尚未有快取，嘗試即時取一次
                        val bmp = runCatching { pm.getApplicationIcon(app.packageName).toBitmap().asImageBitmap() }
                            .getOrNull()
                        if (bmp != null) appIcons[app.packageName] = bmp
                        bmp
                    },
                    isSelected = app.isGoodHabit,
                    onToggle = { isOn ->
                        // ON → 設為好習慣，並確保非壞習慣
                        viewModel.updateApp(app.copy(
                            isGoodHabit = isOn,
                            isBadHabit = if (isOn) false else app.isBadHabit
                        ))
                    },
                    onRatioChange = { newRatio ->
                        viewModel.updateApp(app.copy(ratio = newRatio))
                    },
                    backgroundColor = Color(0xFFE8F5E8), // 好習慣：淺綠色背景
                    showRatioField = true
                )
            }
        }
    }
}

/** 壞習慣 App 分頁：與好習慣頁相同 UI，但開關 ON 代表設定成壞習慣（isGoodHabit = false） */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BadHabitAppSection(
    snackbarHostState: SnackbarHostState
) {
    val scope = rememberCoroutineScope()
    val viewModel: GoodHabitViewModel = viewModel()
    val context = LocalContext.current
    val pm: PackageManager = context.packageManager

    val goodHabitApps by viewModel.goodHabitApps.collectAsState()

    val appIcons = remember { mutableStateMapOf<String, ImageBitmap?>() }

    var searchQuery by remember { mutableStateOf(TextFieldValue("")) }
    var lastUpdated by remember { mutableStateOf("—") }
    var isRescanning by remember { mutableStateOf(false) } // 重新掃描狀態

    LaunchedEffect(goodHabitApps) {
        withContext(Dispatchers.IO) {
            goodHabitApps.forEach { app ->
                if (!appIcons.containsKey(app.packageName)) {
                    val bmp = runCatching { pm.getApplicationIcon(app.packageName).toBitmap().asImageBitmap() }
                        .getOrNull()
                    withContext(Dispatchers.Main) {
                        appIcons[app.packageName] = bmp
                    }
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = {
                    if (!isRescanning) {
                        scope.launch {
                            isRescanning = true
                            rescanAndReload(
                                pm = pm,
                                appIcons = appIcons,
                                viewModel = viewModel,
                                onDone = { count ->
                                    lastUpdated = currentTimeText()
                                    isRescanning = false
                                    scope.launch { snackbarHostState.showSnackbar("重新掃描完成：$count 筆") }
                                }
                            )
                        }
                    }
                },
                enabled = !isRescanning
            ) {
                if (isRescanning) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Icon(Icons.Default.Refresh, contentDescription = null)
                }
                Spacer(Modifier.width(6.dp))
                Text(if (isRescanning) "掃描中..." else "重新掃描")
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "目前筆數：${goodHabitApps.size}",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "最後更新：$lastUpdated",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        // 顯示已加入的壞習慣 App
        val selectedBadApps = goodHabitApps.filter { app -> app.isBadHabit }
        if (selectedBadApps.isNotEmpty()) {
            Text(
                text = "已加入的壞習慣 App：",
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
            )
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                selectedBadApps.forEach { app ->
                    var longPressTriggered by remember { mutableStateOf(false) }
                    val interactionSource = remember { MutableInteractionSource() }
                    val isPressed by interactionSource.collectIsPressedAsState()
                    
                    LaunchedEffect(isPressed) {
                        if (isPressed && !longPressTriggered) {
                            delay(500) // 500ms 長按
                            if (isPressed) {
                                longPressTriggered = true
                                searchQuery = TextFieldValue(app.name)
                            }
                        } else if (!isPressed) {
                            longPressTriggered = false
                        }
                    }
                     
                     AssistChip(
                         onClick = { },
                         interactionSource = interactionSource,
                         label = { Text(app.name) },
                         leadingIcon = {
                             Icon(
                                 imageVector = Icons.Default.Check,
                                 contentDescription = null,
                                 tint = MaterialTheme.colorScheme.error
                             )
                         }
                     )
                }
            }
        }

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            label = { Text("搜尋應用程式") },
            modifier = Modifier.fillMaxWidth(),
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "搜尋") },
            trailingIcon = {
                if (searchQuery.text.isNotEmpty()) {
                    IconButton(
                        onClick = { searchQuery = TextFieldValue("") }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "清除搜尋"
                        )
                    }
                }
            },
            supportingText = { Text("💡 長按上方已選取的 App 可快速搜尋") }
        )

        Spacer(Modifier.height(8.dp))

        val filteredApps = remember(goodHabitApps, searchQuery) {
            goodHabitApps.filter { app ->
                app.name.contains(searchQuery.text, ignoreCase = true) ||
                        app.packageName.contains(searchQuery.text, ignoreCase = true)
            }.sortedBy { app -> app.name }
        }

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(filteredApps, key = { app -> app.packageName }) { app ->
                GoodHabitAppItem(
                    app = app,
                    icon = appIcons.getOrElse(app.packageName) {
                        val bmp = runCatching { pm.getApplicationIcon(app.packageName).toBitmap().asImageBitmap() }
                            .getOrNull()
                        if (bmp != null) appIcons[app.packageName] = bmp
                        bmp
                    },
                    isSelected = app.isBadHabit,
                    onToggle = { isOn ->
                        // ON → 設為壞習慣（isBadHabit = true, isGoodHabit = false）；
                        // OFF → 非壞習慣（isBadHabit = false），不自動轉為好習慣
                        viewModel.updateApp(app.copy(
                            isGoodHabit = if (isOn) false else app.isGoodHabit,
                            isBadHabit = isOn
                        ))
                    },
                    onRatioChange = { newRatio ->
                        viewModel.updateApp(app.copy(ratio = newRatio))
                    },
                    backgroundColor = Color(0xFFFFF0F0), // 壞習慣：淺紅色背景
                    showRatioField = false
                )
            }
        }
    }
}



/** 抽出共用：重新掃描 + 清空 + 寫回 */
/** 抽出共用：重新掃描 + 清空 + 寫回（含逐筆 Log） */
/** 重新掃描 + 清空 + 寫回（只在點「重新掃描」時呼叫） */
private suspend fun rescanAndReload(
    pm: PackageManager,
    appIcons: MutableMap<String, ImageBitmap?>, // 傳入 viewModel.appIcons
    viewModel: GoodHabitViewModel,
    onDone: (count: Int) -> Unit
) {
    withContext(Dispatchers.IO) {
        // 先讀取現有設定，保留開關狀態
        val existingApps = viewModel.goodHabitApps.value
        val existingMap = existingApps.associateBy { app -> app.packageName }

        val apps = pm.getInstalledApplications(PackageManager.GET_META_DATA)
            .filter { ai -> pm.getLaunchIntentForPackage(ai.packageName) != null } // 只留可啟動
            .map { ai ->
                val pkg = ai.packageName
                val name = pm.getApplicationLabel(ai).toString()

                // 直接把圖示塞到快取 map（這樣參數 appIcons 就有被使用，不會出現未使用警告）
                val iconBmp = runCatching { pm.getApplicationIcon(ai).toBitmap().asImageBitmap() }
                    .getOrNull()
                appIcons[pkg] = iconBmp

                // 如果資料庫已存在，保留其 isGoodHabit/isBadHabit/ratio；否則給預設值
                val old = existingMap[pkg]
                GoodHabitApp(
                    name = name,
                    packageName = pkg,
                    ratio = old?.ratio ?: 1f,
                    isGoodHabit = old?.isGoodHabit ?: false,
                    isBadHabit = old?.isBadHabit ?: false
                )
            }
            .sortedBy { app -> app.name }

        // 清空後重新插入，保留原有設定
        viewModel.clearAllApps()
        viewModel.insertAll(apps)

        withContext(Dispatchers.Main) {
            onDone(apps.size)
        }
    }
}

private fun currentTimeText(): String {
    val fmt = SimpleDateFormat("HH:mm:ss", Locale.TAIWAN)
    return fmt.format(System.currentTimeMillis())
}
