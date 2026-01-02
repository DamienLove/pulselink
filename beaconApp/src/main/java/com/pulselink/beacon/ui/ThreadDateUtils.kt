package com.pulselink.beacon.ui

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

object ThreadDateUtils {
    fun isSameDay(t1: Long, t2: Long): Boolean {
        val z1 = Instant.ofEpochMillis(t1).atZone(ZoneId.systemDefault()).toLocalDate()
        val z2 = Instant.ofEpochMillis(t2).atZone(ZoneId.systemDefault()).toLocalDate()
        return z1 == z2
    }

    fun getDateHeader(t: Long): String {
        return getDateHeader(t, System.currentTimeMillis())
    }

    // Visible for testing allows injecting "now"
    fun getDateHeader(t: Long, now: Long): String {
        val date = Instant.ofEpochMillis(t).atZone(ZoneId.systemDefault()).toLocalDate()
        val today = Instant.ofEpochMillis(now).atZone(ZoneId.systemDefault()).toLocalDate()

        return when {
            date == today -> "Today"
            date == today.minusDays(1) -> "Yesterday"
            else -> {
                val fmt = DateTimeFormatter.ofPattern("MMM d")
                date.format(fmt)
            }
        }
    }
}
