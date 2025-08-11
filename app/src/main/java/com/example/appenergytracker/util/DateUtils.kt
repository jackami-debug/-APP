package com.example.appenergytracker.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object DateUtils {
    private val dayFmt = SimpleDateFormat("yyyy-MM-dd", Locale.TAIWAN)
    fun today(): String = dayFmt.format(Date())
}


