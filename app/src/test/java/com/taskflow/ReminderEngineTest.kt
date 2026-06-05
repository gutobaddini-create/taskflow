package com.taskflow

import com.taskflow.core.notifications.ReminderEngine
import com.taskflow.domain.model.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

class ReminderEngineTest {
    @Test
    fun oneTimeReminderReturnsStartWhenFuture() {
        val reminder = Reminder(taskId = "task", userId = "user", startDate = LocalDate.of(2026, 6, 10), startTime = LocalTime.of(9, 0))
        assertEquals(LocalDateTime.of(2026, 6, 10, 9, 0), ReminderEngine.nextOccurrence(reminder, LocalDateTime.of(2026, 6, 5, 8, 0)))
    }

    @Test
    fun oneTimeReminderReturnsNullWhenPast() {
        val reminder = Reminder(taskId = "task", userId = "user", startDate = LocalDate.of(2026, 6, 1), startTime = LocalTime.of(9, 0))
        assertNull(ReminderEngine.nextOccurrence(reminder, LocalDateTime.of(2026, 6, 5, 8, 0)))
    }

    @Test
    fun everyTwoWeeksUsesSelectedWeekdays() {
        val reminder = Reminder(
            taskId = "task",
            userId = "user",
            recurrenceType = RecurrenceType.Custom,
            recurrenceInterval = 2,
            recurrenceUnit = RecurrenceUnit.Weeks,
            selectedWeekDays = listOf(WeekDay.Monday, WeekDay.Thursday),
            startDate = LocalDate.of(2026, 6, 1),
            startTime = LocalTime.of(9, 0)
        )
        assertEquals(LocalDateTime.of(2026, 6, 15, 9, 0), ReminderEngine.nextOccurrence(reminder, LocalDateTime.of(2026, 6, 5, 8, 0)))
    }

    @Test
    fun dailyReminderAdvancesToNextDay() {
        val reminder = Reminder(
            taskId = "task",
            userId = "user",
            recurrenceType = RecurrenceType.Daily,
            startDate = LocalDate.of(2026, 6, 1),
            startTime = LocalTime.of(9, 0)
        )
        assertEquals(LocalDateTime.of(2026, 6, 6, 9, 0), ReminderEngine.nextOccurrence(reminder, LocalDateTime.of(2026, 6, 5, 10, 0)))
    }

    @Test
    fun weeklyReminderUsesSelectedWeekday() {
        val reminder = Reminder(
            taskId = "task",
            userId = "user",
            recurrenceType = RecurrenceType.Weekly,
            selectedWeekDays = listOf(WeekDay.Thursday),
            startDate = LocalDate.of(2026, 6, 1),
            startTime = LocalTime.of(9, 0)
        )
        assertEquals(LocalDateTime.of(2026, 6, 11, 9, 0), ReminderEngine.nextOccurrence(reminder, LocalDateTime.of(2026, 6, 5, 8, 0)))
    }

    @Test
    fun everyFifteenDaysAdvancesByCustomInterval() {
        val reminder = Reminder(
            taskId = "task",
            userId = "user",
            recurrenceType = RecurrenceType.Custom,
            recurrenceInterval = 15,
            recurrenceUnit = RecurrenceUnit.Days,
            startDate = LocalDate.of(2026, 6, 1),
            startTime = LocalTime.of(9, 0)
        )
        assertEquals(LocalDateTime.of(2026, 6, 16, 9, 0), ReminderEngine.nextOccurrence(reminder, LocalDateTime.of(2026, 6, 5, 8, 0)))
    }

    @Test
    fun monthlyFixedDayClampsToEndOfShortMonth() {
        val reminder = Reminder(
            taskId = "task",
            userId = "user",
            recurrenceType = RecurrenceType.Monthly,
            selectedMonthDay = 31,
            startDate = LocalDate.of(2026, 1, 31),
            startTime = LocalTime.of(9, 0)
        )
        assertEquals(LocalDateTime.of(2026, 2, 28, 9, 0), ReminderEngine.nextOccurrence(reminder, LocalDateTime.of(2026, 2, 1, 8, 0)))
    }

    @Test
    fun monthlyFixedDayCanUseDayTen() {
        val reminder = Reminder(
            taskId = "task",
            userId = "user",
            recurrenceType = RecurrenceType.Monthly,
            selectedMonthDay = 10,
            startDate = LocalDate.of(2026, 1, 10),
            startTime = LocalTime.of(9, 0)
        )
        assertEquals(LocalDateTime.of(2026, 2, 10, 9, 0), ReminderEngine.nextOccurrence(reminder, LocalDateTime.of(2026, 2, 1, 8, 0)))
    }

    @Test
    fun lastBusinessDaySkipsWeekend() {
        val reminder = Reminder(
            taskId = "task",
            userId = "user",
            recurrenceType = RecurrenceType.Monthly,
            monthlyRule = MonthlyRule.LastBusinessDay,
            startDate = LocalDate.of(2026, 2, 1),
            startTime = LocalTime.of(9, 0)
        )
        assertEquals(LocalDateTime.of(2026, 2, 27, 9, 0), ReminderEngine.nextOccurrence(reminder, LocalDateTime.of(2026, 2, 1, 8, 0)))
    }

    @Test
    fun endDateStopsFutureOccurrence() {
        val reminder = Reminder(
            taskId = "task",
            userId = "user",
            recurrenceType = RecurrenceType.Daily,
            endType = ReminderEndType.OnDate,
            endDate = LocalDate.of(2026, 6, 5),
            startDate = LocalDate.of(2026, 6, 1),
            startTime = LocalTime.of(9, 0)
        )
        assertNull(ReminderEngine.nextOccurrence(reminder, LocalDateTime.of(2026, 6, 5, 10, 0)))
    }

    @Test
    fun occurrenceLimitStopsWhenReached() {
        val reminder = Reminder(
            taskId = "task",
            userId = "user",
            recurrenceType = RecurrenceType.Daily,
            endType = ReminderEndType.AfterOccurrences,
            maxOccurrences = 3,
            occurrencesCompleted = 3,
            startDate = LocalDate.of(2026, 6, 1),
            startTime = LocalTime.of(9, 0)
        )
        assertNull(ReminderEngine.nextOccurrence(reminder, LocalDateTime.of(2026, 6, 5, 8, 0)))
    }
}
