package com.taskflow.core.notifications

import com.taskflow.domain.model.*
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.YearMonth

object ReminderEngine {
    fun nextOccurrence(reminder: Reminder, from: LocalDateTime = LocalDateTime.now()): LocalDateTime? {
        if (!reminder.isActive) return null
        if (reminder.endType == ReminderEndType.AfterOccurrences && reminder.maxOccurrences != null && reminder.occurrencesCompleted >= reminder.maxOccurrences) return null

        val start = LocalDateTime.of(reminder.startDate, reminder.startTime)
        val candidate = when (reminder.recurrenceType) {
            RecurrenceType.None -> if (start.isAfter(from)) start else null
            RecurrenceType.Daily -> step(start, from) { it.plusDays(reminder.recurrenceInterval.toLong()) }
            RecurrenceType.Weekly -> nextWeekly(reminder, from)
            RecurrenceType.Monthly -> nextMonthly(reminder, from)
            RecurrenceType.Yearly -> step(start, from) { it.plusYears(reminder.recurrenceInterval.toLong()) }
            RecurrenceType.Custom -> when (reminder.recurrenceUnit) {
                RecurrenceUnit.Days -> step(start, from) { it.plusDays(reminder.recurrenceInterval.toLong()) }
                RecurrenceUnit.Weeks -> nextWeekly(reminder, from)
                RecurrenceUnit.Months -> nextMonthly(reminder, from)
                RecurrenceUnit.Years -> step(start, from) { it.plusYears(reminder.recurrenceInterval.toLong()) }
            }
        }
        return if (candidate != null && reminder.endType == ReminderEndType.OnDate && reminder.endDate != null && candidate.toLocalDate().isAfter(reminder.endDate)) null else candidate
    }

    private fun step(start: LocalDateTime, from: LocalDateTime, advance: (LocalDateTime) -> LocalDateTime): LocalDateTime {
        var cursor = start
        while (!cursor.isAfter(from)) cursor = advance(cursor)
        return cursor
    }

    private fun nextWeekly(reminder: Reminder, from: LocalDateTime): LocalDateTime {
        val days = reminder.selectedWeekDays.ifEmpty { listOf(WeekDay.Monday) }.map { DayOfWeek.of(it.value) }.toSet()
        var date = maxOf(reminder.startDate, from.toLocalDate())
        repeat(370) {
            val occurrence = LocalDateTime.of(date, reminder.startTime)
            val weeksFromStart = java.time.temporal.ChronoUnit.WEEKS.between(reminder.startDate, date).coerceAtLeast(0)
            if (date.dayOfWeek in days && weeksFromStart % reminder.recurrenceInterval == 0L && occurrence.isAfter(from)) return occurrence
            date = date.plusDays(1)
        }
        return LocalDateTime.of(reminder.startDate, reminder.startTime).plusWeeks(reminder.recurrenceInterval.toLong())
    }

    private fun nextMonthly(reminder: Reminder, from: LocalDateTime): LocalDateTime {
        var month = YearMonth.from(maxOf(reminder.startDate, from.toLocalDate()))
        repeat(48) {
            val date = monthlyDate(month, reminder)
            val occurrence = LocalDateTime.of(date, reminder.startTime)
            val monthsFromStart = java.time.temporal.ChronoUnit.MONTHS.between(YearMonth.from(reminder.startDate), month).coerceAtLeast(0)
            if (!date.isBefore(reminder.startDate) && monthsFromStart % reminder.recurrenceInterval == 0L && occurrence.isAfter(from)) return occurrence
            month = month.plusMonths(1)
        }
        return LocalDateTime.of(reminder.startDate.plusMonths(reminder.recurrenceInterval.toLong()), reminder.startTime)
    }

    private fun monthlyDate(month: YearMonth, reminder: Reminder): LocalDate {
        return when (reminder.monthlyRule) {
            MonthlyRule.LastDay -> month.atEndOfMonth()
            MonthlyRule.FirstBusinessDay -> businessDays(month).first()
            MonthlyRule.LastBusinessDay -> businessDays(month).last()
            MonthlyRule.FirstMonday -> (1..7).map { month.atDay(it) }.first { it.dayOfWeek == DayOfWeek.MONDAY }
            MonthlyRule.LastFriday -> (0..6).map { month.atEndOfMonth().minusDays(it.toLong()) }.first { it.dayOfWeek == DayOfWeek.FRIDAY }
            MonthlyRule.None -> month.atDay((reminder.selectedMonthDay ?: reminder.startDate.dayOfMonth).coerceAtMost(month.lengthOfMonth()))
        }
    }

    private fun businessDays(month: YearMonth): List<LocalDate> = (1..month.lengthOfMonth())
        .map { month.atDay(it) }
        .filter { it.dayOfWeek != DayOfWeek.SATURDAY && it.dayOfWeek != DayOfWeek.SUNDAY }
}
