// File: app/src/main/java/com/example/appenergytracker/data/IconRepository.kt
package com.example.appenergytracker.data

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.util.LruCache
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class AppEntry(val label: String, val packageName: String)

object IconRepository {
    private lateinit var appContext: Context
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    // 清單 & Icon 全域保存（跨頁面都還在）
    private val _apps = MutableStateFlow<List<AppEntry>>(emptyList())
    val apps = _apps.asStateFlow()

    // pkg -> icon
    val iconMap = mutableMapOf<String, Bitmap?>()

    // 記憶體快取
    private val iconCache: LruCache<String, Bitmap> = object : LruCache<String, Bitmap>(
        (Runtime.getRuntime().maxMemory() / 1024).toInt() / 8
    ) {
        override fun sizeOf(key: String, value: Bitmap): Int = value.byteCount / 1024
    }

    fun init(context: Context) {
        if (::appContext.isInitialized) return
        appContext = context.applicationContext
        refresh() // 第一次自動載入
    }

    fun refresh() {
        scope.launch {
            val list = withContext(Dispatchers.IO) {
                val pm = appContext.packageManager
                val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
                pm.queryIntentActivities(intent, 0)
                    .map { ri ->
                        val label = ri.loadLabel(pm)?.toString().orEmpty()
                        val pkg = ri.activityInfo?.packageName ?: ri.resolvePackageName ?: ""
                        AppEntry(label, pkg)
                    }
                    .distinctBy { it.packageName }
                    .sortedBy { it.label.lowercase() }
            }
            _apps.value = list
        }
    }

    fun hardRefresh() {
        scope.launch {
            iconCache.evictAll()
            iconMap.clear()
            refresh()
        }
    }

    fun ensureIcon(pkg: String) {
        if (iconMap.containsKey(pkg)) return
        scope.launch {
            val bmp = withContext(Dispatchers.IO) {
                iconCache.get(pkg) ?: run {
                    try {
                        val d = appContext.packageManager.getApplicationIcon(pkg)
                        val b = drawableToBitmap(d)
                        iconCache.put(pkg, b)
                        b
                    } catch (_: Exception) { null }
                }
            }
            iconMap[pkg] = bmp
        }
    }

    private fun drawableToBitmap(drawable: Drawable): Bitmap {
        val w = drawable.intrinsicWidth.takeIf { it > 0 } ?: 128
        val h = drawable.intrinsicHeight.takeIf { it > 0 } ?: 128
        return when (drawable) {
            is BitmapDrawable -> drawable.bitmap
            else -> {
                val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                val c = Canvas(bmp)
                drawable.setBounds(0, 0, c.width, c.height)
                drawable.draw(c)
                bmp
            }
        }
    }
}
