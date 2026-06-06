package com.taskflow.feature.reminders

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.taskflow.ChipText
import com.taskflow.GradientButton
import com.taskflow.InfoRow
import com.taskflow.SectionTitle
import com.taskflow.Segmented
import com.taskflow.TaskFlowCard
import com.taskflow.TaskFlowViewModel
import com.taskflow.TopRow
import com.taskflow.core.design.LoadingFullScreen
import com.taskflow.core.design.TaskFlowColors
import com.taskflow.domain.model.MonthlyRule
import com.taskflow.domain.model.RecurrenceType
import com.taskflow.domain.model.RecurrenceUnit
import com.taskflow.domain.model.Reminder
import com.taskflow.domain.model.ReminderEndType
import com.taskflow.domain.model.ReminderType
import com.taskflow.domain.model.WeekDay
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@Composable
fun ReminderScreen(vm: TaskFlowViewModel, onSave: () -> Unit) {
    val task = vm.selectedTask()
    if (task == null) {
        LoadingFullScreen("Carregando lembrete...")
        return
    }
    var enabled by remember { mutableStateOf(true) }
    var interval by remember { mutableIntStateOf(2) }
    var repeatMode by remember { mutableStateOf("Personalizada") }
    var unit by remember { mutableStateOf(RecurrenceUnit.Weeks) }
    var selectedWeekDays by remember { mutableStateOf(setOf(WeekDay.Monday, WeekDay.Thursday)) }
    var monthlyMode by remember { mutableStateOf("Data fixa") }
    var selectedMonthDay by remember { mutableStateOf("10") }
    var monthlyRule by remember { mutableStateOf(MonthlyRule.LastBusinessDay) }
    var endMode by remember { mutableStateOf("Em uma data") }
    var endDate by remember { mutableStateOf("31/12/2026") }
    var maxOccurrences by remember { mutableStateOf("5") }
    LazyColumn(Modifier.fillMaxSize().statusBarsPadding().padding(22.dp), contentPadding = PaddingValues(bottom = 30.dp)) {
        item {
            TopRow("<", "Lembrete personalizado", onSave)
            SectionTitle("Ativar lembrete")
            TaskFlowCard {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Notificar esta tarefa", fontWeight = FontWeight.Bold)
                    Switch(enabled, { enabled = it })
                }
            }
            SectionTitle("Quando")
            TaskFlowCard {
                InfoRow("Data inicial", "10/06/2026")
                InfoRow("Horario", "09:00")
            }
            SectionTitle("Avisar antes")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("5 min", "15 min", "30 min", "1 h").forEach { ChipText(it) }
            }
            SectionTitle("Repeticao")
            Segmented(listOf("Nao repetir", "Simples", "Personalizada"), repeatMode) { repeatMode = it }
            TaskFlowCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Repetir a cada", color = TaskFlowColors.Muted)
                    Spacer(Modifier.weight(1f))
                    IconButton({ if (interval > 1) interval-- }) { Icon(Icons.Default.Remove, null) }
                    Text("$interval", fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    IconButton({ interval++ }) { Icon(Icons.Default.Add, null) }
                }
                ReminderChoiceRow(
                    listOf(
                        RecurrenceUnit.Days to "dias",
                        RecurrenceUnit.Weeks to "semanas",
                        RecurrenceUnit.Months to "meses",
                        RecurrenceUnit.Years to "anos"
                    ),
                    unit
                ) { unit = it }
                if (unit == RecurrenceUnit.Weeks) {
                    ReminderChoiceRow(
                        WeekDay.entries.map { it to it.short },
                        selected = null,
                        onSelect = { day ->
                            selectedWeekDays = if (day in selectedWeekDays) selectedWeekDays - day else selectedWeekDays + day
                        },
                        isSelected = { it in selectedWeekDays }
                    )
                    Text("Dias selecionados: ${selectedWeekDays.sortedBy { it.value }.joinToString { it.short }}", color = TaskFlowColors.Muted, modifier = Modifier.padding(top = 8.dp))
                }
                if (unit == RecurrenceUnit.Months) {
                    ReminderChoiceRow(listOf("Data fixa" to "data", "Regra mensal" to "regra"), monthlyMode) { monthlyMode = it }
                    if (monthlyMode == "Data fixa") {
                        OutlinedTextField(selectedMonthDay, { selectedMonthDay = it.filter(Char::isDigit).take(2) }, label = { Text("Dia do mes") }, shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth())
                    } else {
                        ReminderChoiceRow(
                            listOf(
                                MonthlyRule.FirstBusinessDay to "1o util",
                                MonthlyRule.LastBusinessDay to "ultimo util",
                                MonthlyRule.LastDay to "ultimo dia",
                                MonthlyRule.FirstMonday to "1a seg",
                                MonthlyRule.LastFriday to "ultima sex"
                            ),
                            monthlyRule
                        ) { monthlyRule = it }
                    }
                }
            }
            SectionTitle("Fim da repeticao")
            TaskFlowCard {
                ReminderChoiceRow(listOf("Nunca" to "nunca", "Em uma data" to "data", "Apos repeticoes" to "repeticoes"), endMode) { endMode = it }
                if (endMode == "Em uma data") {
                    OutlinedTextField(endDate, { endDate = it }, label = { Text("Data final") }, shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth())
                }
                if (endMode == "Apos repeticoes") {
                    OutlinedTextField(maxOccurrences, { maxOccurrences = it.filter(Char::isDigit).take(3) }, label = { Text("Quantidade de repeticoes") }, shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth())
                }
            }
            Spacer(Modifier.height(24.dp))
            GradientButton("Salvar lembrete", {
                val recurrenceType = when (repeatMode) {
                    "Nao repetir" -> RecurrenceType.None
                    "Simples" -> when (unit) {
                        RecurrenceUnit.Days -> RecurrenceType.Daily
                        RecurrenceUnit.Weeks -> RecurrenceType.Weekly
                        RecurrenceUnit.Months -> RecurrenceType.Monthly
                        RecurrenceUnit.Years -> RecurrenceType.Yearly
                    }
                    else -> RecurrenceType.Custom
                }
                val parsedEndDate = runCatching { LocalDate.parse(endDate, DateTimeFormatter.ofPattern("dd/MM/yyyy")) }.getOrNull()
                vm.repo.saveReminder(
                    Reminder(
                        taskId = task.id,
                        userId = vm.currentUser().id,
                        type = if (recurrenceType == RecurrenceType.None) ReminderType.OneTime else ReminderType.Recurring,
                        startDate = LocalDate.of(2026, 6, 10),
                        startTime = LocalTime.of(9, 0),
                        recurrenceType = recurrenceType,
                        recurrenceInterval = interval,
                        recurrenceUnit = unit,
                        selectedWeekDays = selectedWeekDays.ifEmpty { setOf(WeekDay.Monday) }.toList(),
                        selectedMonthDay = if (unit == RecurrenceUnit.Months && monthlyMode == "Data fixa") selectedMonthDay.toIntOrNull()?.coerceIn(1, 31) else null,
                        monthlyRule = if (unit == RecurrenceUnit.Months && monthlyMode == "Regra mensal") monthlyRule else MonthlyRule.None,
                        endType = when (endMode) {
                            "Em uma data" -> ReminderEndType.OnDate
                            "Apos repeticoes" -> ReminderEndType.AfterOccurrences
                            else -> ReminderEndType.Never
                        },
                        endDate = if (endMode == "Em uma data") parsedEndDate ?: LocalDate.of(2026, 12, 31) else null,
                        maxOccurrences = if (endMode == "Apos repeticoes") maxOccurrences.toIntOrNull() else null,
                        isActive = enabled
                    )
                )
                onSave()
            }, Modifier.fillMaxWidth(), enabled = enabled)
        }
    }
}

@Composable
fun <T> ReminderChoiceRow(
    options: List<Pair<T, String>>,
    selected: T?,
    isSelected: (T) -> Boolean = { selected == it },
    onSelect: (T) -> Unit
) {
    Column(Modifier.fillMaxWidth().padding(top = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        options.chunked(3).forEach { rowOptions ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                rowOptions.forEach { (value, label) ->
                    ChipText(label, active = isSelected(value), modifier = Modifier.clickable { onSelect(value) })
                }
            }
        }
    }
}
