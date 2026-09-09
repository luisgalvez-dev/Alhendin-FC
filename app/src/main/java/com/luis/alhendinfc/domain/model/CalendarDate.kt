package com.luis.alhendinfc.domain.model

import java.time.DateTimeException
import java.time.LocalDate

/**
 * Convierte fechas visibles `dd/MM/yyyy` (también `d/M/yyyy`) a epoch day.
 * No modifica el texto original. Inválidas o vacías → null.
 */
object CalendarDate {

    fun toEpochDay(date: String): Long? {
        val trimmed = date.trim()
        if (trimmed.isEmpty()) return null
        val parts = trimmed.split('/')
        if (parts.size != 3) return null
        val day = parts[0].toIntOrNull() ?: return null
        val month = parts[1].toIntOrNull() ?: return null
        val year = parts[2].toIntOrNull() ?: return null
        if (year !in 1900..2100) return null
        return try {
            LocalDate.of(year, month, day).toEpochDay()
        } catch (_: DateTimeException) {
            null
        }
    }

    fun format(epochDay: Long): String {
        val date = LocalDate.ofEpochDay(epochDay)
        return "%02d/%02d/%04d".format(date.dayOfMonth, date.monthValue, date.year)
    }

    fun epochDayOf(date: String): Long? = toEpochDay(date)
}
