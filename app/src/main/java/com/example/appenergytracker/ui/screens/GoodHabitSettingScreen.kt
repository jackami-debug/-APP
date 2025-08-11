package com.example.appenergytracker.ui.screens

import android.content.Intent
import android.content.pm.PackageManager
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private data class AppEntry(
    val label: String,
    val packageName: String,
    val iconBitmap: android.graphics.Bitmap?
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoodHabitSettingScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val pm = context.packageManager
    var search by remember { mutableStateOf(TextFieldValue("")) }
    var apps by remember { mutableStateOf(emptyList<AppEntry>()) }
    var isLoading by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()

    fun loadApps() {
        isLoading = true
        scope.launch {
            val list = withContext(Dispatchers.IO) {
                val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
                pm.queryIntentActivities(intent, 0)
                    .map { ri ->
                        val label = ri.loadLabel(pm)?.toString().orEmpty()
                        val pkg = ri.activityInfo?.packageName ?: ri.resolvePackageName ?: ""
                        val drawable = pm.getApplicationIcon(pkg)
                        val w = drawable.intrinsicWidth.takeIf { it > 0 } ?: 128
                        val h = drawable.intrinsicHeight.takeIf { it > 0 } ?: 128
                        val bmp = android.graphics.Bitmap.createBitmap(w, h, android.graphics.Bitmap.Config.ARGB_8888)
                        val canvas = android.graphics.Canvas(bmp)
                        drawable.setBounds(0, 0, canvas.width, canvas.height)
                        drawable.draw(canvas)
                        AppEntry(label, pkg, bmp)
                    }
                    .sortedBy { it.label.lowercase() }
            }
            apps = list
            isLoading = false
        }
    }

    // 每次進入畫面都重新抓
    LaunchedEffect(Unit) {
        loadApps()
    }

    val filtered = remember(search.text, apps) {
        if (search.text.isBlank()) apps
        else apps.filter { it.label.contains(search.text, ignoreCase = true) }
    }

    Column(modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("選擇你的 App") },
            actions = {
                IconButton(onClick = { loadApps() }) {
                    Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                }
            }
        )

        OutlinedTextField(
            value = search,
            onValueChange = { search = it },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            placeholder = { Text("搜尋應用程式…") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp)
        )

        if (isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                items(filtered, key = { it.packageName }) { app ->
                    AppRow(app)
                }
            }
        }
    }
}

@Composable
private fun AppRow(app: AppEntry) {
    var checked by remember { mutableStateOf(false) }
    var ratio by remember { mutableStateOf("1:1") }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (app.iconBitmap != null) {
            Image(bitmap = app.iconBitmap.asImageBitmap(), contentDescription = null, modifier = Modifier.size(42.dp))
        } else {
            Box(Modifier.size(42.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(text = app.label, style = MaterialTheme.typography.titleMedium)
            Text(text = app.packageName, style = MaterialTheme.typography.bodySmall)
        }
        Switch(
            checked = checked,
            onCheckedChange = { checked = it },
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                checkedTrackColor = MaterialTheme.colorScheme.primary,
                uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant,
            )
        )
        Spacer(Modifier.width(8.dp))
        OutlinedTextField(
            value = ratio,
            onValueChange = { ratio = it },
            singleLine = true,
            modifier = Modifier.width(84.dp)
        )
    }
}
