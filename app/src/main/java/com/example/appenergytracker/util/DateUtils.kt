package com.example.appenergytracker.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.Calendar

object DateUtils {
    private val dayFmt = SimpleDateFormat("yyyy-MM-dd", Locale.TAIWAN)
    private val dayWithWeekFmt = SimpleDateFormat("yyyy-MM-dd (EEE)", Locale.TAIWAN)

    fun today(): String = dayFmt.format(Date())

    fun addDays(date: String, offsetDays: Int): String {
        val cal = Calendar.getInstance()
        cal.time = dayFmt.parse(date) ?: Date()
        cal.add(Calendar.DATE, offsetDays)
        return dayFmt.format(cal.time)
    }

    fun isToday(date: String): Boolean {
        return date == today()
    }

    fun formatWithWeekday(date: String): String {
        val parsed = dayFmt.parse(date) ?: Date()
        return dayWithWeekFmt.format(parsed)
    }
}


