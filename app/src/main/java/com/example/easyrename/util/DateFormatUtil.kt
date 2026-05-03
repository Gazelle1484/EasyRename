package com.example.easyrename.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object DateFormatUtil {

    fun format(timestamp: Long?): String {
        if (timestamp == null || timestamp <= 0L) {
            return ""
        }

        return SimpleDateFormat(DATE_PATTERN, Locale.getDefault()).format(Date(timestamp))
    }

    private const val DATE_PATTERN = "yyyy/MM/dd HH:mm"
}
