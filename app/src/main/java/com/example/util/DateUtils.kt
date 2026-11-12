package com.example.util

import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

object DateUtils {
    val KOLKATA_ZONE: ZoneId = ZoneId.of("Asia/Kolkata")

    // Current local date in Asia/Kolkata
    fun getTodayInKolkata(): LocalDate {
        return ZonedDateTime.now(KOLKATA_ZONE).toLocalDate()
    }

    // Work date string in ISO format YYYY-MM-DD
    fun formatIsoDate(date: LocalDate): String {
        return date.format(DateTimeFormatter.ISO_LOCAL_DATE)
    }

    // Format for Day Name (e.g., THURSDAY)
    fun formatDayName(date: LocalDate): String {
        return date.format(DateTimeFormatter.ofPattern("EEEE", Locale.ENGLISH)).uppercase(Locale.ENGLISH)
    }

    // Format for Large Date Heading (e.g., 12 NOVEMBER 2026)
    fun formatDisplayDate(date: LocalDate): String {
        return date.format(DateTimeFormatter.ofPattern("dd MMMM yyyy", Locale.ENGLISH)).uppercase(Locale.ENGLISH)
    }

    // Format for Short Date (e.g., 12 NOV 2026)
    fun formatShortDate(date: LocalDate): String {
        return date.format(DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH)).uppercase(Locale.ENGLISH)
    }

    // Parse ISO date string
    fun parseIsoDate(dateString: String): LocalDate {
        return try {
            LocalDate.parse(dateString, DateTimeFormatter.ISO_LOCAL_DATE)
        } catch (_: Exception) {
            getTodayInKolkata()
        }
    }

    // Format submission ISO timestamp for human reading (e.g., "12 Nov 2026, 06:45 PM IST")
    fun formatTimestamp(isoTimestamp: String?): String {
        if (isoTimestamp.isNullOrBlank()) return "N/A"
        return try {
            val zdt = ZonedDateTime.parse(isoTimestamp).withZoneSameInstant(KOLKATA_ZONE)
            zdt.format(DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a 'IST'", Locale.ENGLISH))
        } catch (_: Exception) {
            try {
                // If it was already formatted or simple string
                isoTimestamp.substringBefore("T")
            } catch (_: Exception) {
                isoTimestamp
            }
        }
    }

    // Current ISO timestamp string with timezone
    fun nowIsoTimestamp(): String {
        return ZonedDateTime.now(KOLKATA_ZONE).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
    }
}
