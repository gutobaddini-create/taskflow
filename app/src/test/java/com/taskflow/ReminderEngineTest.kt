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
}
