package com.pulselink.beacon.ui

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.concurrent.TimeUnit

class ThreadDateUtilsTest {

    @Test
    fun testIsSameDay() {
        val now = 1700000000000L // arbitrary fixed time
        val sameDayLater = now + 1000

        assert(ThreadDateUtils.isSameDay(now, sameDayLater))

        // 24 hours later might be next day or same day depending on timezone/time,
        // but 48 hours is definitely different.
        val diffDay = now + TimeUnit.DAYS.toMillis(2)
        assert(!ThreadDateUtils.isSameDay(now, diffDay))
    }

    @Test
    fun testGetDateHeader() {
        val now = 1700000000000L // Tue Nov 14 2023 22:13:20 UTC

        // Test Today
        assertEquals("Today", ThreadDateUtils.getDateHeader(now, now))

        // Test Yesterday
        val yesterday = now - TimeUnit.DAYS.toMillis(1)
        assertEquals("Yesterday", ThreadDateUtils.getDateHeader(yesterday, now))

        // Test Older Date
        val older = now - TimeUnit.DAYS.toMillis(5)
        // Note: The specific output "Nov 9" depends on the system default timezone of the runner.
        // For a robust test, we check it's not Today/Yesterday
        val header = ThreadDateUtils.getDateHeader(older, now)
        assert(header != "Today")
        assert(header != "Yesterday")
        assert(header.contains("Nov") || header.contains("Oct")) // Rough check
    }
}
