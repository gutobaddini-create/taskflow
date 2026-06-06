package com.taskflow.feature.tasks

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.taskflow.core.design.ChipText
import com.taskflow.core.design.ChipTone
import com.taskflow.core.design.EmptyState
import com.taskflow.core.design.GradientButton
import com.taskflow.core.design.InfoRow
import com.taskflow.core.design.PriorityPill
import com.taskflow.core.design.ScreenTitle
import com.taskflow.core.design.SectionTitle
import com.taskflow.core.design.Segmented
import com.taskflow.core.design.StatusPill
import com.taskflow.core.design.TaskFlowCard
import com.taskflow.core.app.TaskFlowViewModel
import com.taskflow.core.design.TopRow
import com.taskflow.core.design.LoadingFullScreen
import com.taskflow.core.design.TaskFlowColors
import com.taskflow.core.notifications.ReminderEngine
import com.taskflow.core.permissions.PermissionPolicy
import com.taskflow.core.design.touchFeedback
import com.taskflow.domain.model.Comment
import com.taskflow.domain.model.Reminder
import com.taskflow.domain.model.ReminderType
import com.taskflow.domain.model.RecurrenceType
import com.taskflow.domain.model.RecurrenceUnit
import com.taskflow.domain.model.TaskPriority
import com.taskflow.domain.model.TaskStatus
import com.taskflow.domain.model.now
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
@Composable
fun DetailScreen(vm: TaskFlowViewModel, onBack: () -> Unit, onMaterials: () -> Unit, onShare: () -> Unit, onReminder: () -> Unit) {
    val task = vm.selectedTask()
    val users by vm.users.collectAsState()
    val preferences by vm.preferences.collectAsState()
    val reminders by vm.reminders.collectAsState()
    val attachments by vm.attachments.collectAsState()
    val links by vm.links.collectAsState()
    val fields by vm.customFields.collectAsState()
    val comments by vm.comments.collectAsState()
    val invites by vm.invites.collectAsState()
    val activity by vm.activity.collectAsState()
    if (task == null) {
        LoadingFullScreen("Carregando tarefa...")
        return
    }
    var editing by remember(task.id) { mutableStateOf(false) }
    var commentText by remember(task.id) { mutableStateOf("") }
    var editingCommentId by remember(task.id) { mutableStateOf<String?>(null) }
    var editingCommentText by remember(task.id) { mutableStateOf("") }
    var editTitle by remember(task.id) { mutableStateOf(task.title) }
    var editDescription by remember(task.id) { mutableStateOf(task.description) }
    var editStatus by remember(task.id) { mutableStateOf(task.status) }
    var editPriority by remember(task.id) { mutableStateOf(task.priority) }
    var confirmComplete by remember(task.id) { mutableStateOf(false) }
    var editDueDay by remember(task.id) { mutableStateOf(if (task.dueDate?.toLocalDate() == LocalDate.now().plusDays(1)) "Amanha" else "Hoje") }
    var editDueHour by remember(task.id) { mutableStateOf(task.dueDate?.toLocalTime()?.format(DateTimeFormatter.ofPattern("HH:mm")) ?: "09:00") }
    val currentUser = users.firstOrNull { it.id == preferences.currentUserId } ?: users.firstOrNull() ?: vm.currentUser()
    var editAssignedTo by remember(task.id, currentUser.id) { mutableStateOf(task.assignedTo ?: currentUser.id) }
    val userOptions = users.ifEmpty { listOf(currentUser) }
    val effectivePermission = PermissionPolicy.acceptedPermission(task.id, currentUser.id, invites)
    val canViewTask = PermissionPolicy.canViewTask(task, currentUser.id, effectivePermission)
    val canEditTask = PermissionPolicy.canEditTask(task, currentUser.id, effectivePermission)
    val canComment = PermissionPolicy.canCommentOnTask(task, currentUser.id, effectivePermission)
    val assigneeName = userOptions.firstOrNull { it.id == task.assignedTo }?.name ?: "Manuel"
    val hasActiveReminders = reminders.any { it.taskId == task.id && it.isActive }
    if (confirmComplete) {
        AlertDialog(
            onDismissRequest = { confirmComplete = false },
            title = { Text("Concluir tarefa?") },
            text = { Text(if (hasActiveReminders) "Esta tarefa possui lembretes ativos. Ao concluir, os lembretes serao desativados." else "A tarefa sera marcada como concluida.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmComplete = false
                    vm.repo.completeTask(task.id)
                    onBack()
                }) { Text("Concluir") }
            },
            dismissButton = { TextButton(onClick = { confirmComplete = false }) { Text("Cancelar") } }
        )
    }
    if (!canViewTask) {
        LazyColumn(Modifier.fillMaxSize().statusBarsPadding().padding(22.dp), contentPadding = PaddingValues(bottom = 30.dp)) {
            item {
                TopRow("<", "Detalhe da tarefa", onBack)
                Spacer(Modifier.height(24.dp))
                TaskFlowCard {
                    Icon(Icons.Default.Lock, null, tint = TaskFlowColors.Muted, modifier = Modifier.size(42.dp))
                    Text("Sem acesso a esta tarefa", fontWeight = FontWeight.Bold, color = TaskFlowColors.Text, modifier = Modifier.padding(top = 12.dp))
                    Text("Solicite um convite ao responsavel para visualizar detalhes, anexos, links e campos.", color = TaskFlowColors.Muted, modifier = Modifier.padding(top = 6.dp))
                }
            }
        }
        return
    }
    LazyColumn(Modifier.fillMaxSize().statusBarsPadding().padding(22.dp), contentPadding = PaddingValues(bottom = 30.dp)) {
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onBack) { Text("<") }
                Spacer(Modifier.weight(1f))
                Text("Detalhe da tarefa", fontWeight = FontWeight.Bold, color = TaskFlowColors.Text)
                Spacer(Modifier.weight(1f))
                if (canEditTask) {
                    TextButton(onClick = { editing = !editing }) { Text(if (editing) "Cancelar" else "Editar") }
                } else {
                    Text("Ver", color = TaskFlowColors.Muted, modifier = Modifier.padding(end = 12.dp))
                }
            }
            if (editing) {
                OutlinedTextField(editTitle, { editTitle = it }, label = { Text("Titulo") }, modifier = Modifier.fillMaxWidth().padding(top = 18.dp), shape = RoundedCornerShape(18.dp))
                OutlinedTextField(editDescription, { editDescription = it }, label = { Text("Descricao") }, modifier = Modifier.fillMaxWidth().padding(top = 10.dp), shape = RoundedCornerShape(18.dp), minLines = 3)
                SectionTitle("Status")
                Segmented(TaskStatus.entries.map { it.label }, editStatus.label) { selected -> editStatus = TaskStatus.entries.first { it.label == selected } }
                SectionTitle("Prioridade")
                Segmented(TaskPriority.entries.map { it.label }, editPriority.label) { selected -> editPriority = TaskPriority.entries.first { it.label == selected } }
                SectionTitle("Prazo")
                Segmented(listOf("Hoje", "Amanha"), editDueDay) { editDueDay = it }
                Spacer(Modifier.height(8.dp))
                Segmented(listOf("09:00", "11:00", "14:00", "16:30"), editDueHour) { editDueHour = it }
                SectionTitle("Responsavel")
                Segmented(userOptions.map { it.name }, userOptions.firstOrNull { it.id == editAssignedTo }?.name ?: vm.currentUser().name) { name ->
                    editAssignedTo = userOptions.first { it.name == name }.id
                }
                Spacer(Modifier.height(16.dp))
                GradientButton("Salvar alteracoes", {
                    if (editTitle.isNotBlank()) {
                        val date = if (editDueDay == "Amanha") LocalDate.now().plusDays(1) else LocalDate.now()
                        val done = editStatus == TaskStatus.Done
                        vm.repo.updateTask(task.copy(title = editTitle.trim(), description = editDescription, status = editStatus, priority = editPriority, assignedTo = editAssignedTo, dueDate = LocalDateTime.of(date, LocalTime.parse(editDueHour)), isCompleted = done, completedAt = if (done) task.completedAt ?: now() else null))
                        editing = false
                    }
                }, Modifier.fillMaxWidth().testTag("save-task-edits").semantics { contentDescription = "Salvar alteracoes da tarefa" }, enabled = editTitle.isNotBlank())
                TextButton(onClick = { vm.repo.deleteTask(task.id); onBack() }, modifier = Modifier.fillMaxWidth()) { Text("Excluir tarefa", color = Color(0xFFEF4444)) }
            } else {
                ScreenTitle(task.title, Modifier.padding(top = 18.dp))
                Row(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) { StatusPill(task.status); PriorityPill(task.priority) }
            }
            SectionTitle("Proximo lembrete")
            val reminderModifier = if (canEditTask) Modifier.touchFeedback(onClick = onReminder) else Modifier
            TaskFlowCard(reminderModifier.testTag("open-reminder").semantics { contentDescription = "Configurar lembrete" }) {
                val taskReminders = reminders.filter { it.taskId == task.id }
                val nextReminder = taskReminders.mapNotNull { reminder -> ReminderEngine.nextOccurrence(reminder)?.let { reminder to it } }.minByOrNull { it.second }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text(nextReminder?.first?.displaySummary() ?: "Nenhum lembrete", fontWeight = FontWeight.Bold, color = TaskFlowColors.Text)
                        Text(nextReminder?.second?.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) ?: "Sem disparo agendado", color = TaskFlowColors.Muted)
                    }
                    Switch(
                        checked = taskReminders.any { it.isActive },
                        onCheckedChange = { active -> taskReminders.forEach { vm.repo.saveReminder(it.copy(isActive = active)) } },
                        enabled = canEditTask && taskReminders.isNotEmpty()
                    )
                }
            }
            SectionTitle("Descricao")
            TaskFlowCard { Text((if (editing) editDescription else task.description).ifBlank { "Sem descricao." }, color = TaskFlowColors.Muted, lineHeight = 22.sp) }
            SectionTitle("Dados principais")
            TaskFlowCard {
                InfoRow("Prazo", task.dueDate?.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) ?: "Sem prazo")
                InfoRow("Responsavel", assigneeName)
                InfoRow("Participantes", "2 pessoas")
            }
            SectionTitle("Materiais da tarefa")
            val materialsModifier = if (canViewTask) Modifier.touchFeedback(onClick = onMaterials) else Modifier
            TaskFlowCard(materialsModifier) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ChipText("${attachments.count { it.taskId == task.id }} anexos", tone = ChipTone.Blue)
                    ChipText("${links.count { it.taskId == task.id }} link", tone = ChipTone.Success)
                    ChipText("${fields.count { it.taskId == task.id }} campos", tone = ChipTone.Warning)
                }
                Spacer(Modifier.height(12.dp))
                Text(attachments.firstOrNull { it.taskId == task.id }?.fileName ?: "Nenhum anexo", color = TaskFlowColors.Text)
                Text(links.firstOrNull { it.taskId == task.id }?.title ?: "Nenhum link", color = TaskFlowColors.Text)
            }
            SectionTitle("Comentarios")
            TaskFlowCard {
                val taskComments = comments.filter { it.taskId == task.id }
                if (taskComments.isEmpty()) {
                    EmptyState("Nenhum comentario", "Use comentarios para registrar decisoes ou contexto.")
                } else {
                    taskComments.forEach {
                        val isOwnComment = it.authorId == currentUser.id
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text(users.firstOrNull { user -> user.id == it.authorId }?.name ?: "Usuario", fontWeight = FontWeight.Bold, color = TaskFlowColors.Text, modifier = Modifier.weight(1f))
                            if (isOwnComment && editingCommentId != it.id) {
                                TextButton(onClick = {
                                    editingCommentId = it.id
                                    editingCommentText = it.text
                                }, modifier = Modifier.semantics { contentDescription = "Editar comentario" }) { Text("Editar") }
                                TextButton(onClick = { vm.repo.deleteComment(it.id) }, modifier = Modifier.semantics { contentDescription = "Excluir comentario" }) { Text("Excluir", color = Color(0xFFEF4444)) }
                            }
                        }
                        if (editingCommentId == it.id) {
                            OutlinedTextField(editingCommentText, { editingCommentText = it }, label = { Text("Editar comentario") }, shape = RoundedCornerShape(18.dp), singleLine = true, modifier = Modifier.fillMaxWidth())
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                TextButton(onClick = {
                                    editingCommentId = null
                                    editingCommentText = ""
                                }, modifier = Modifier.weight(1f)) { Text("Cancelar") }
                                Button(onClick = {
                                    if (editingCommentText.isNotBlank()) {
                                        vm.repo.updateComment(it.copy(text = editingCommentText.trim()))
                                        editingCommentId = null
                                        editingCommentText = ""
                                    }
                                }, modifier = Modifier.weight(1f).semantics { contentDescription = "Salvar comentario" }, enabled = editingCommentText.isNotBlank(), shape = RoundedCornerShape(50)) { Text("Salvar") }
                            }
                        } else {
                            Text(it.text, color = TaskFlowColors.Muted, modifier = Modifier.padding(bottom = 4.dp))
                        }
                        Text(formatTimestamp(it.updatedAt), color = TaskFlowColors.Muted, fontSize = 12.sp, modifier = Modifier.padding(bottom = 12.dp))
                    }
                }
                OutlinedTextField(commentText, { commentText = it }, label = { Text("Novo comentario") }, shape = RoundedCornerShape(18.dp), singleLine = true, modifier = Modifier.fillMaxWidth(), enabled = canComment)
                Spacer(Modifier.height(10.dp))
                Button(onClick = {
                    if (commentText.isNotBlank()) {
                        vm.repo.addComment(Comment(taskId = task.id, authorId = currentUser.id, text = commentText.trim()))
                        commentText = ""
                    }
                }, modifier = Modifier.fillMaxWidth().height(54.dp).semantics { contentDescription = "Adicionar comentario" }, enabled = canComment && commentText.isNotBlank(), shape = RoundedCornerShape(50)) {
                    Text("Adicionar comentario", fontWeight = FontWeight.Bold)
                }
                if (!canComment) {
                    Text("Sem permissao para comentar.", color = TaskFlowColors.Muted, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp))
                }
            }
            SectionTitle("Historico")
            TaskFlowCard {
                val taskActivity = activity.filter { it.taskId == task.id }.take(6)
                if (taskActivity.isEmpty()) {
                    EmptyState("Nenhum evento", "Alteracoes relevantes aparecerao aqui.")
                } else {
                    taskActivity.forEach {
                        Text(it.action, color = TaskFlowColors.Text, fontWeight = FontWeight.SemiBold)
                        Text(formatTimestamp(it.createdAt), color = TaskFlowColors.Muted, fontSize = 12.sp, modifier = Modifier.padding(bottom = 10.dp))
                    }
                }
            }
            Spacer(Modifier.height(20.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = onShare, modifier = Modifier.weight(1f), shape = RoundedCornerShape(50)) { Text("Compartilhar") }
                GradientButton("Concluir", { confirmComplete = true }, Modifier.weight(1f), enabled = canEditTask)
            }
        }
    }
}

private fun Reminder.displaySummary(): String {
    if (type == ReminderType.OneTime || recurrenceType == RecurrenceType.None) return "Lembrete unico"
    return when (recurrenceUnit) {
        RecurrenceUnit.Days -> if (recurrenceInterval == 1) "Todo dia" else "A cada $recurrenceInterval dias"
        RecurrenceUnit.Weeks -> {
            val days = selectedWeekDays.sortedBy { it.value }.joinToString { it.short }
            (if (recurrenceInterval == 1) "Toda semana" else "A cada $recurrenceInterval semanas") + if (days.isBlank()) "" else " - $days"
        }
        RecurrenceUnit.Months -> {
            val monthly = selectedMonthDay?.let { "dia $it" } ?: monthlyRule.label.lowercase()
            (if (recurrenceInterval == 1) "Todo mes" else "A cada $recurrenceInterval meses") + " - $monthly"
        }
        RecurrenceUnit.Years -> if (recurrenceInterval == 1) "Todo ano" else "A cada $recurrenceInterval anos"
    }
}

private fun formatTimestamp(value: Long): String = Instant.ofEpochMilli(value).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
