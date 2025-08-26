// File: app/src/main/java/com/example/appenergytracker/data/IconDiskCache.kt
package com.example.appenergytracker.data

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import com.example.appenergytracker.data.AppEntry


data class PreloadResult(
    val total: Int,
    val created: Int,
    val skipped: Int,
    val failed: Int
)

object IconDiskCache {

    private fun dir(context: Context): File {
        val d = File(context.cacheDir, "app_icons")
        if (!d.exists()) d.mkdirs()
        return d
    }

    private fun fileOf(context: Context, pkg: String): File =
        File(dir(context), "$pkg.png")

    /** 同步：有檔案就直接解碼回傳（不做任何 I/O 以外的工作） */
    fun getSync(context: Context, pkg: String): Bitmap? {
        val f = fileOf(context, pkg)
        return if (f.exists()) BitmapFactory.decodeFile(f.absolutePath) else null
    }

    /** 只把 Drawable 畫成 Bitmap（minSdk 24 OK） */
    private fun drawableToBitmap(drawable: Drawable): Bitmap {
        val w = drawable.intrinsicWidth.takeIf { it > 0 } ?: 128
        val h = drawable.intrinsicHeight.takeIf { it > 0 } ?: 128
        return if (drawable is BitmapDrawable) {
            drawable.bitmap
        } else {
            val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            val c = Canvas(bmp)
            drawable.setBounds(0, 0, c.width, c.height)
            drawable.draw(c)
            bmp
        }
    }

    /** 取得「可啟動」App 清單 */
    private fun queryLaunchable(pm: PackageManager): List<AppEntry> {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val acts = pm.queryIntentActivities(intent, 0)
        return acts.map { ri ->
            AppEntry(
                label = ri.loadLabel(pm)?.toString().orEmpty(),
                packageName = ri.activityInfo?.packageName ?: ri.resolvePackageName ?: ""
            )
        }.distinctBy { it.packageName }
    }

    /**
     * 批次預載：把所有「可啟動」App 的 icon 存到 cacheDir/app_icons/{pkg}.png
     * - 已存在就跳過（skip）
     * - 失敗就計數（fail）
     * - 回傳統計結果
     * - onProgress(完成數, 總數) 可用來更新 UI 進度
     */
    suspend fun preloadAll(
        context: Context,
        onProgress: ((done: Int, total: Int) -> Unit)? = null
    ): PreloadResult = withContext(Dispatchers.IO) {
        val pm = context.packageManager
        val list = queryLaunchable(pm)
        val total = list.size
        var created = 0
        var skipped = 0
        var failed = 0
        var done = 0

        for (app in list) {
            val f = fileOf(context, app.packageName)
            if (f.exists()) {
                skipped++
                done++
                onProgress?.invoke(done, total)
                continue
            }
            try {
                val d = pm.getApplicationIcon(app.packageName)
                val bmp = drawableToBitmap(d)
                FileOutputStream(f).use { out ->
                    bmp.compress(Bitmap.CompressFormat.PNG, 100, out)
                }
                created++
            } catch (_: Exception) {
                failed++
            } finally {
                done++
                onProgress?.invoke(done, total)
            }
        }
        PreloadResult(total, created, skipped, failed)
    }

    /** 清某一個或全部快取（選用） */
    fun clear(context: Context, pkg: String? = null) {
        if (pkg != null) fileOf(context, pkg).delete()
        else dir(context).listFiles()?.forEach { it.delete() }
    }
}
