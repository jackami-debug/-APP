// File: app/src/main/java/com/example/appenergytracker/viewmodel/AppListViewModel.kt
package com.example.appenergytracker.viewmodel

import android.app.Application
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.util.LruCache
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class AppEntry(val label: String, val packageName: String)

class AppListViewModel(app: Application) : AndroidViewModel(app) {

    // UI 直接觀察這兩個狀態即可
    val appList = mutableStateListOf<AppEntry>()
    val iconMap = mutableStateMapOf<String, Bitmap?>() // pkg -> Bitmap?

    private val pm: PackageManager = app.packageManager

    // LruCache：避免重抓與節省記憶體
    private val iconCache: LruCache<String, Bitmap> = object : LruCache<String, Bitmap>(
        (Runtime.getRuntime().maxMemory() / 1024).toInt() / 8
    ) {
        override fun sizeOf(key: String, value: Bitmap): Int = value.byteCount / 1024
    }

    init {
        refresh() // 首次自動載入
    }

    /** 重新載入可啟動 App 清單（不清 iconMap，保留已載入的圖示） */
    fun refresh() {
        viewModelScope.launch {
            val list = withContext(Dispatchers.IO) {
                val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
                val activities = pm.queryIntentActivities(intent, 0)
                activities.map { ri ->
                    val label = ri.loadLabel(pm)?.toString().orEmpty()
                    val pkg = ri.activityInfo?.packageName ?: ri.resolvePackageName ?: ""
                    AppEntry(label = label, packageName = pkg)
                }
                    .distinctBy { it.packageName }
                    .sortedBy { it.label.lowercase() }
            }
            appList.clear()
            appList.addAll(list)
        }
    }

    /** 強制重刷（清掉快取與目前 iconMap） */
    fun hardRefresh() {
        viewModelScope.launch {
            iconCache.evictAll()
            iconMap.clear()
            refresh()
        }
    }

    /** 確保某個 pkg 的 icon 已經載入；若沒有就背景載入一次 */
    fun ensureIcon(pkg: String) {
        if (iconMap.containsKey(pkg)) return
        viewModelScope.launch {
            val bmp = withContext(Dispatchers.IO) {
                iconCache.get(pkg) ?: run {
                    try {
                        val d = pm.getApplicationIcon(pkg)
                        val b = drawableToBitmap(d)
                        iconCache.put(pkg, b)
                        b
                    } catch (_: Exception) {
                        null
                    }
                }
            }
            iconMap[pkg] = bmp
        }
    }

    // 通用 Drawable → Bitmap（minSdk 24 OK）
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
