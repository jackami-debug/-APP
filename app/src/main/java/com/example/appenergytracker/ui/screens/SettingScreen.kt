package com.example.appenergytracker.ui.screens

import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Add
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingScreen() {
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("壞習慣 App", "好習慣 App")

    val snackbarHostState = remember { SnackbarHostState() }

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
                    scope.launch {
                        rescanAndReload(
                            pm = pm,
                            appIcons = appIcons,
                            viewModel = viewModel,
                            onDone = { count ->
                                lastUpdated = currentTimeText()
                                // 這行是 suspend，要再包一層 coroutine
                                scope.launch {
                                    snackbarHostState.showSnackbar("重新掃描完成：$count 筆")
                                }
                            }
                        )
                    }
                }

            ) {
                Icon(Icons.Default.Refresh, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text("重新掃描")
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

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            label = { Text("搜尋應用程式") },
            modifier = Modifier.fillMaxWidth(),
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "搜尋") }
        )

        Spacer(Modifier.height(8.dp))

        val filteredApps = remember(goodHabitApps, searchQuery) {
            goodHabitApps.filter {
                it.name.contains(searchQuery.text, ignoreCase = true) ||
                        it.packageName.contains(searchQuery.text, ignoreCase = true)
            }.sortedBy { it.name }
        }

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(filteredApps, key = { it.packageName }) { app ->
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
                    scope.launch {
                        rescanAndReload(
                            pm = pm,
                            appIcons = appIcons,
                            viewModel = viewModel,
                            onDone = { count ->
                                lastUpdated = currentTimeText()
                                scope.launch { snackbarHostState.showSnackbar("重新掃描完成：$count 筆") }
                            }
                        )
                    }
                }
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text("重新掃描")
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
        val selectedBadApps = goodHabitApps.filter { it.isBadHabit }
        if (selectedBadApps.isNotEmpty()) {
            Text(
                text = "已加入的壞習慣 App：",
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
            )
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(selectedBadApps) { app ->
                    Chip(
                        onClick = { },
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
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "搜尋") }
        )

        Spacer(Modifier.height(8.dp))

        val filteredApps = remember(goodHabitApps, searchQuery) {
            goodHabitApps.filter {
                it.name.contains(searchQuery.text, ignoreCase = true) ||
                        it.packageName.contains(searchQuery.text, ignoreCase = true)
            }.sortedBy { it.name }
        }

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(filteredApps, key = { it.packageName }) { app ->
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
        val existingMap = existingApps.associateBy { it.packageName }

        val apps = pm.getInstalledApplications(PackageManager.GET_META_DATA)
            .filter { pm.getLaunchIntentForPackage(it.packageName) != null } // 只留可啟動
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
            .sortedBy { it.name }

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
