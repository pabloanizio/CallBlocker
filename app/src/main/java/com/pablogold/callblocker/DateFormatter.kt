package com.pablogold.callblocker

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

object DateFormatter {

    fun format(timestamp: Long): String {
        val formatter = DateTimeFormatter
            .ofPattern("dd/MM/yyyy 'às' HH:mm", Locale.getDefault())
            .withZone(ZoneId.systemDefault())

        return formatter.format(Instant.ofEpochMilli(timestamp))
    }
}