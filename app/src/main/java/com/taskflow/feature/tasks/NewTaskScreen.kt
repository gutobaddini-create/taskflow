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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.taskflow.core.design.DesignTokens
import com.taskflow.core.design.GradientButton
import com.taskflow.core.design.SectionTitle
import com.taskflow.core.design.Segmented
import com.taskflow.core.design.SmallAction
import com.taskflow.core.design.TaskFlowCard
import com.taskflow.core.app.TaskFlowViewModel
import com.taskflow.core.design.TopRow
import com.taskflow.core.design.TaskFlowColors
import com.taskflow.domain.model.Invite
import com.taskflow.domain.model.Task
import com.taskflow.domain.model.TaskPriority
import com.taskflow.domain.model.UserPermission
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

@Composable
fun NewTaskScreen(vm: TaskFlowViewModel, onCancel: () -> Unit) {
    val lists by vm.lists.collectAsState()
    val users by vm.users.collectAsState()
    val user = vm.currentUser()
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var priority by remember { mutableStateOf(TaskPriority.Medium) }
    var selectedListId by remember { mutableStateOf<String?>(null) }
    var selectedAssigneeId by remember(user.id) { mutableStateOf(user.id) }
    var invitePermission by remember { mutableStateOf("Sem convite") }
    var dueDay by remember { mutableStateOf("Hoje") }
    var dueHour by remember { mutableStateOf("09:00") }
    var attemptedSave by remember { mutableStateOf(false) }
    val selectedList = lists.firstOrNull { it.id == selectedListId } ?: lists.firstOrNull()
    val assigneeOptions = users.ifEmpty { listOf(user) }
    val selectedAssignee = assigneeOptions.firstOrNull { it.id == selectedAssigneeId } ?: user
    LaunchedEffect(lists) {
        if (selectedListId == null && lists.isNotEmpty()) selectedListId = lists.first().id
    }
    LazyColumn(Modifier.fillMaxSize().statusBarsPadding().padding(DesignTokens.screenPadding), contentPadding = PaddingValues(bottom = DesignTokens.screenBottomPadding)) {
        item {
            TopRow("Cancelar", "Nova tarefa", onCancel)
            Spacer(Modifier.height(18.dp))
            OutlinedTextField(title, { title = it }, label = { Text("Titulo da tarefa *") }, modifier = Modifier.fillMaxWidth().testTag("new-task-title").semantics { contentDescription = "Campo titulo da tarefa" }, shape = RoundedCornerShape(18.dp), isError = attemptedSave && title.isBlank(), supportingText = { if (attemptedSave && title.isBlank()) Text("Informe um titulo para salvar.") })
            OutlinedTextField(description, { description = it }, label = { Text("Descricao") }, modifier = Modifier.fillMaxWidth().padding(top = 10.dp).testTag("new-task-description").semantics { contentDescription = "Campo descricao da tarefa" }, shape = RoundedCornerShape(18.dp), minLines = 3)
            Spacer(Modifier.height(16.dp))
            TaskFlowCard {
                Text("Lista", color = TaskFlowColors.Muted)
                if (lists.isEmpty()) {
                    Text("Crie uma lista antes de salvar tarefas.", color = Color(0xFFEF4444), modifier = Modifier.padding(top = 8.dp))
                } else {
                    Segmented(lists.map { it.name }, selectedList?.name ?: lists.first().name) { name ->
                        selectedListId = lists.first { it.name == name }.id
                    }
                }
                Text("Prazo", color = TaskFlowColors.Muted, modifier = Modifier.padding(top = 14.dp))
                Segmented(listOf("Hoje", "Amanha"), dueDay) { dueDay = it }
                Spacer(Modifier.height(8.dp))
                Segmented(listOf("09:00", "11:00", "14:00", "16:30"), dueHour) { dueHour = it }
                Text("Responsavel", color = TaskFlowColors.Muted, modifier = Modifier.padding(top = 14.dp))
                Segmented(assigneeOptions.map { it.name }, selectedAssignee.name) { name ->
                    selectedAssigneeId = assigneeOptions.first { it.name == name }.id
                }
                Text("Convite de pessoas", color = TaskFlowColors.Muted, modifier = Modifier.padding(top = 14.dp))
                Segmented(listOf("Sem convite", "Comentar", "Ver"), invitePermission) { invitePermission = it }
                Text("O convite local sera criado ao salvar a tarefa.", color = TaskFlowColors.Muted, fontSize = 13.sp, modifier = Modifier.padding(top = 8.dp))
            }
            SectionTitle("Lembretes")
            TaskFlowCard {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text("Configurar apos salvar", fontWeight = FontWeight.Bold, color = TaskFlowColors.Text)
                        Text("Evita aplicar lembretes em uma tarefa incorreta.", color = TaskFlowColors.Muted)
                    }
                    Icon(Icons.Default.NotificationsActive, null, tint = TaskFlowColors.Muted)
                }
            }
            SectionTitle("Materiais da tarefa")
            TaskFlowCard {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ChipText("Anexos", tone = ChipTone.Blue)
                    ChipText("Links", tone = ChipTone.Success)
                    ChipText("Campos", tone = ChipTone.Warning)
                }
                Spacer(Modifier.height(14.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    SmallAction(Icons.Default.AttachFile, "Arquivo")
                    SmallAction(Icons.Default.PhotoCamera, "Foto")
                    SmallAction(Icons.Default.Link, "Link")
                }
                Text("Materiais ficam disponiveis no detalhe depois que a tarefa existir.", color = TaskFlowColors.Muted, fontSize = 13.sp, modifier = Modifier.padding(top = 10.dp))
            }
            SectionTitle("Prioridade")
            Segmented(TaskPriority.entries.map { it.label }, priority.label) { priority = TaskPriority.entries.first { p -> p.label == it } }
            Spacer(Modifier.height(24.dp))
            GradientButton("Salvar", {
                attemptedSave = true
                if (title.isNotBlank()) {
                    val list = selectedList
                    if (list != null) {
                        val time = LocalTime.parse(dueHour)
                        val date = if (dueDay == "Amanha") LocalDate.now().plusDays(1) else LocalDate.now()
                        val task = Task(spaceId = list.spaceId, listId = list.id, title = title.trim(), description = description, priority = priority, createdBy = user.id, assignedTo = selectedAssignee.id, dueDate = LocalDateTime.of(date, time))
                        vm.repo.createTask(task)
                        when (invitePermission) {
                            "Comentar" -> vm.repo.createInvite(Invite(taskId = task.id, createdBy = user.id, permission = UserPermission.Participant))
                            "Ver" -> vm.repo.createInvite(Invite(taskId = task.id, createdBy = user.id, permission = UserPermission.Viewer))
                        }
                        onCancel()
                    }
                }
            }, Modifier.fillMaxWidth().testTag("save-new-task").semantics { contentDescription = "Salvar nova tarefa" }, enabled = lists.isNotEmpty())
        }
    }
}
