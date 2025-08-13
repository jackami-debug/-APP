package com.example.appenergytracker.data

import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import com.example.appenergytracker.data.database.AppDatabase
import com.example.appenergytracker.data.entity.AppUsageLog
import com.example.appenergytracker.util.DateUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class UsageStatsRepository(private val context: Context) {

    private val appDao = AppDatabase.getDatabase(context).appDao()

    suspend fun collectTodayUsageAndStore(): Int = withContext(Dispatchers.IO) {
        val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val pm: PackageManager = context.packageManager

        val start = dayStartMillis()
        val end = System.currentTimeMillis()

        val stats: List<UsageStats> = if (Build.VERSION.SDK_INT >= 28) {
            usageStatsManager.queryUsageStats(
                UsageStatsManager.INTERVAL_DAILY, start, end
            )
        } else {
            @Suppress("DEPRECATION")
            usageStatsManager.queryUsageStats(
                UsageStatsManager.INTERVAL_DAILY, start, end
            )
        }

        val perPackageMinutes: Map<String, Int> = stats
            .filter { stats -> stats.totalTimeInForeground > 0 }
            .groupBy { stats -> stats.packageName }
            .mapValues { entry ->
                val totalMs = entry.value.sumOf { stats -> stats.totalTimeInForeground }
                (totalMs / 1000L / 60L).toInt()
            }
            .filterValues { it > 0 }

        val date = DateUtils.today()

        val logs = perPackageMinutes.entries.map { (pkg, minutes) ->
            val appName = runCatching { pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString() }
                .getOrDefault(pkg)
            AppUsageLog(
                packageName = pkg,
                appName = appName,
                usageDate = date,
                usageMinutes = minutes
            )
        }

        appDao.clearUsageLogsByDate(date)
        if (logs.isNotEmpty()) appDao.insertUsageLogs(logs)
        logs.size
    }

    private fun dayStartMillis(): Long {
        val now = java.util.Calendar.getInstance()
        now.set(java.util.Calendar.HOUR_OF_DAY, 0)
        now.set(java.util.Calendar.MINUTE, 0)
        now.set(java.util.Calendar.SECOND, 0)
        now.set(java.util.Calendar.MILLISECOND, 0)
        return now.timeInMillis
    }
}


