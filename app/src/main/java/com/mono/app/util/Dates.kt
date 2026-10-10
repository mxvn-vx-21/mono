package com.mono.app.util

import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

/** All stored dates are LocalDate epoch days (no timezone shifts). */
object Dates {
    fun fromEpochDay(day: Long): LocalDate = LocalDate.ofEpochDay(day)

    /** Material DatePicker works in UTC-midnight millis. */
    fun pickerMillisToEpochDay(ms: Long): Long =
        Instant.ofEpochMilli(ms).atZone(ZoneOffset.UTC).toLocalDate().toEpochDay()

    fun epochDayToPickerMillis(day: Long): Long =
        LocalDate.ofEpochDay(day).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

    fun formatDay(day: Long, withYear: Boolean = true): String =
        fromEpochDay(day).format(DateTimeFormatter.ofPattern(if (withYear) "d MMM yyyy" else "d MMM", Locale.getDefault()))

    fun formatTime(minutes: Int): String =
        LocalTime.of(minutes / 60, minutes % 60).format(DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault()))

    fun formatStamp(millis: Long): String =
        java.time.LocalDateTime.ofInstant(Instant.ofEpochMilli(millis), java.time.ZoneId.systemDefault())
            .format(DateTimeFormatter.ofPattern("d MMM, h:mm a", Locale.getDefault()))

    fun dueLabel(day: Long?, minutes: Int?, today: Long): String {
        if (day == null) return "No date"
        val base = when (day - today) {
            0L -> "Today"; 1L -> "Tomorrow"; -1L -> "Yesterday"
            else -> formatDay(day, withYear = false)
        }
        return if (minutes != null) base + ", " + formatTime(minutes) else base
    }

    fun greeting(hour: Int): String = when (hour) {
        in 5..11 -> "Good morning"
        in 12..16 -> "Good afternoon"
        in 17..21 -> "Good evening"
        else -> "Good night"
    }

    fun daysUntil(target: Long, today: Long): Long = target - today
}
