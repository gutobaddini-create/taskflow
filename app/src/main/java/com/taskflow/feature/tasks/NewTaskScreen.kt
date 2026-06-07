package com.taskflow.feature.tasks

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.filled.AddTask
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.taskflow.core.design.ChipText
import com.taskflow.core.design.ChipTone
import com.taskflow.core.design.DesignTokens
import com.taskflow.core.design.FeedbackBanner
import com.taskflow.core.design.FeedbackKind
import com.taskflow.core.design.GradientButton
import com.taskflow.core.design.IconBubble
import com.taskflow.core.design.InfoRow
import com.taskflow.core.design.SectionTitle
import com.taskflow.core.design.Segmented
import com.taskflow.core.design.SmallAction
import com.taskflow.core.design.TaskFlowCard
import com.taskflow.core.app.TaskFlowViewModel
import com.taskflow.core.design.TaskFlowColors
import com.taskflow.core.design.TopRow
import com.taskflow.core.sharing.InviteLinks
import com.taskflow.core.utils.attachmentType
import com.taskflow.core.utils.isAllowedAttachment
import com.taskflow.domain.model.Attachment
import com.taskflow.domain.model.AttachmentSource
import com.taskflow.domain.model.ChecklistItem
import com.taskflow.domain.model.CustomField
import com.taskflow.domain.model.CustomFieldType
import com.taskflow.domain.model.Invite
import com.taskflow.domain.model.Task
import com.taskflow.domain.model.TaskLink
import com.taskflow.domain.model.TaskPriority
import com.taskflow.domain.model.UserPermission
import com.taskflow.feature.materials.FieldDialog
import com.taskflow.feature.materials.LinkDialog
import com.taskflow.feature.materials.attachmentMetadata
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private data class DraftAttachment(
    val uri: Uri,
    val name: String,
    val mimeType: String,
    val sizeBytes: Long,
    val source: AttachmentSource
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewTaskScreen(vm: TaskFlowViewModel, onCancel: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val lists by vm.lists.collectAsState()
    val spaces by vm.spaces.collectAsState()
    val users by vm.users.collectAsState()
    val user = vm.currentUser()
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var priority by remember { mutableStateOf(TaskPriority.Medium) }
    var selectedListId by remember { mutableStateOf<String?>(null) }
    var selectedAssigneeId by remember(user.id) { mutableStateOf(user.id) }
    var invitePermission by remember { mutableStateOf("Sem convite") }
    var inviteChannel by remember { mutableStateOf("Copiar") }
    var dueDate by remember { mutableStateOf(LocalDate.now()) }
    var dueTime by remember { mutableStateOf(LocalTime.of(9, 0)) }
    var attemptedSave by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var showLinkDialog by remember { mutableStateOf(false) }
    var showFieldDialog by remember { mutableStateOf(false) }
    var checklistText by remember { mutableStateOf("") }
    val draftAttachments = remember { mutableStateListOf<DraftAttachment>() }
    val draftLinks = remember { mutableStateListOf<TaskLink>() }
    val draftFields = remember { mutableStateListOf<CustomField>() }
    val draftChecklist = remember { mutableStateListOf<ChecklistItem>() }

    val visibleSpaceIds = spaces.filter { it.ownerId == user.id || user.id in it.members }.map { it.id }.toSet()
    val visibleLists = lists.filter { it.spaceId in visibleSpaceIds }
    val selectedList = visibleLists.firstOrNull { it.id == selectedListId } ?: visibleLists.firstOrNull()
    val assigneeOptions = users.ifEmpty { listOf(user) }
    val selectedAssignee = assigneeOptions.firstOrNull { it.id == selectedAssigneeId } ?: user
    val saveLabel = when {
        invitePermission != "Sem convite" -> "Salvar e compartilhar"
        draftAttachments.isNotEmpty() || draftLinks.isNotEmpty() || draftFields.isNotEmpty() || draftChecklist.isNotEmpty() -> "Salvar com materiais"
        else -> "Salvar"
    }

    LaunchedEffect(visibleLists) {
        if (selectedListId !in visibleLists.map { it.id }) selectedListId = visibleLists.firstOrNull()?.id
    }

    fun addDraftAttachment(uri: Uri, source: AttachmentSource) {
        val metadata = context.attachmentMetadata(uri)
        if (!isAllowedAttachment(metadata.name, metadata.sizeBytes)) {
            message = "Arquivo invalido ou maior que 20 MB."
            return
        }
        draftAttachments += DraftAttachment(uri, metadata.name, metadata.mimeType, metadata.sizeBytes, source)
        message = "Material adicionado ao rascunho."
    }

    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let {
            runCatching { context.contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
            addDraftAttachment(it, AttachmentSource.FilePicker)
        }
    }
    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { addDraftAttachment(it, AttachmentSource.Gallery) }
    }

    if (showDatePicker) {
        val state = rememberDatePickerState(dueDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli())
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton({
                    state.selectedDateMillis?.let { millis ->
                        dueDate = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()
                    }
                    showDatePicker = false
                }) { Text("Confirmar") }
            },
            dismissButton = { TextButton({ showDatePicker = false }) { Text("Cancelar") } }
        ) {
            DatePicker(state = state)
        }
    }

    if (showTimePicker) {
        val state = rememberTimePickerState(dueTime.hour, dueTime.minute, true)
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                TextButton({
                    dueTime = LocalTime.of(state.hour, state.minute)
                    showTimePicker = false
                }) { Text("Confirmar") }
            },
            dismissButton = { TextButton({ showTimePicker = false }) { Text("Cancelar") } },
            text = { TimePicker(state = state) }
        )
    }

    if (showLinkDialog) {
        LinkDialog("", "https://", "", "", { showLinkDialog = false }) { linkTitle, url, linkDescription, category ->
            draftLinks += TaskLink(taskId = "draft", createdBy = user.id, title = linkTitle, url = url, description = linkDescription, category = category)
            showLinkDialog = false
        }
    }

    if (showFieldDialog) {
        FieldDialog("", CustomFieldType.Text, "", { showFieldDialog = false }) { name, type, value ->
            draftFields += CustomField(taskId = "draft", createdBy = user.id, fieldName = name, fieldType = type, fieldValue = value)
            showFieldDialog = false
        }
    }

    LazyColumn(Modifier.fillMaxSize().statusBarsPadding().padding(DesignTokens.screenPadding), contentPadding = PaddingValues(bottom = DesignTokens.screenBottomPadding)) {
        item {
            TopRow("Cancelar", "Nova tarefa", onCancel)
            Spacer(Modifier.height(14.dp))
            OutlinedTextField(title, { title = it }, label = { Text("Titulo *") }, modifier = Modifier.fillMaxWidth().testTag("new-task-title").semantics { contentDescription = "Campo titulo da tarefa" }, shape = RoundedCornerShape(16.dp), isError = attemptedSave && title.isBlank(), supportingText = { if (attemptedSave && title.isBlank()) Text("Informe um titulo.") })
            OutlinedTextField(description, { description = it }, label = { Text("Descricao") }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp).testTag("new-task-description").semantics { contentDescription = "Campo descricao da tarefa" }, shape = RoundedCornerShape(16.dp), minLines = 2)

            SectionTitle("Organizacao")
            TaskFlowCard {
                Text("Lista", color = TaskFlowColors.Muted)
                if (visibleLists.isEmpty()) {
                    Text("Crie uma lista antes de salvar.", color = TaskFlowColors.Danger, modifier = Modifier.padding(top = 8.dp))
                } else {
                    Segmented(visibleLists.map { it.name }, selectedList?.name ?: visibleLists.first().name) { name ->
                        selectedListId = visibleLists.first { it.name == name }.id
                    }
                }
                Row(Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton({ showDatePicker = true }, Modifier.weight(1f)) {
                        Icon(Icons.Default.CalendarToday, null)
                        Spacer(Modifier.padding(3.dp))
                        Text(dueDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")), maxLines = 1)
                    }
                    OutlinedButton({ showTimePicker = true }, Modifier.weight(1f)) {
                        Icon(Icons.Default.Schedule, null)
                        Spacer(Modifier.padding(3.dp))
                        Text(dueTime.format(DateTimeFormatter.ofPattern("HH:mm")))
                    }
                }
                Text("Responsavel", color = TaskFlowColors.Muted, modifier = Modifier.padding(top = 12.dp))
                Segmented(assigneeOptions.map { it.name }, selectedAssignee.name) { name ->
                    selectedAssigneeId = assigneeOptions.first { it.name == name }.id
                }
            }

            SectionTitle("Materiais")
            TaskFlowCard {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ChipText("${draftAttachments.size} anexos", tone = ChipTone.Blue)
                    ChipText("${draftLinks.size} links", tone = ChipTone.Success)
                    ChipText("${draftFields.size} campos", tone = ChipTone.Warning)
                }
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SmallAction(Icons.Default.AttachFile, "Arquivo", Modifier.weight(1f)) { filePicker.launch(arrayOf("application/pdf", "application/msword", "application/vnd.openxmlformats-officedocument.wordprocessingml.document", "application/vnd.ms-excel", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "text/plain")) }
                    SmallAction(Icons.Default.PhotoCamera, "Foto", Modifier.weight(1f)) { imagePicker.launch("image/*") }
                    SmallAction(Icons.Default.Link, "Link", Modifier.weight(1f)) { showLinkDialog = true }
                }
                Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SmallAction(Icons.Default.AddTask, "Campo", Modifier.weight(1f)) { showFieldDialog = true }
                }
                draftAttachments.take(3).forEach { DraftLine(it.name, "${it.sizeBytes / 1024} KB") }
                draftLinks.take(2).forEach { DraftLine(it.title, it.url) }
            }

            SectionTitle("Checklist")
            TaskFlowCard {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(checklistText, { checklistText = it }, label = { Text("Item") }, singleLine = true, modifier = Modifier.weight(1f))
                    OutlinedButton({
                        val clean = checklistText.trim()
                        if (clean.isNotBlank()) {
                            draftChecklist += ChecklistItem(taskId = "draft", title = clean)
                            checklistText = ""
                        }
                    }) { Text("Adicionar") }
                }
                draftChecklist.take(4).forEach { DraftLine(it.title, "Checklist") }
            }

            SectionTitle("Convite")
            TaskFlowCard {
                Segmented(listOf("Sem convite", "Comentar", "Ver"), invitePermission) { invitePermission = it }
                if (invitePermission != "Sem convite") {
                    Spacer(Modifier.height(10.dp))
                    Segmented(listOf("Copiar", "WhatsApp", "E-mail"), inviteChannel) { inviteChannel = it }
                    InfoRow("Link", "HTTPS gerado ao salvar")
                }
            }

            SectionTitle("Prioridade")
            Segmented(TaskPriority.entries.map { it.label }, priority.label) { priority = TaskPriority.entries.first { p -> p.label == it } }
            FeedbackBanner(message, if (message?.contains("invalido", true) == true) FeedbackKind.Error else FeedbackKind.Success, Modifier.padding(top = 10.dp))
            Spacer(Modifier.height(18.dp))
            GradientButton(saveLabel, {
                attemptedSave = true
                val list = selectedList
                if (title.isBlank() || list == null) return@GradientButton
                val task = Task(spaceId = list.spaceId, listId = list.id, title = title.trim(), description = description.trim(), priority = priority, createdBy = user.id, assignedTo = selectedAssignee.id, dueDate = LocalDateTime.of(dueDate, dueTime))
                vm.repo.createTask(task)
                draftAttachments.forEach {
                    vm.repo.addAttachment(Attachment(taskId = task.id, uploadedBy = user.id, fileName = it.name, originalFileName = it.name, fileType = attachmentType(it.name), mimeType = it.mimeType, fileSize = it.sizeBytes, storagePath = it.uri.toString(), source = it.source))
                }
                draftLinks.forEach { vm.repo.addLink(it.copy(taskId = task.id, createdBy = user.id)) }
                draftFields.forEach { vm.repo.addCustomField(it.copy(taskId = task.id, createdBy = user.id)) }
                draftChecklist.forEach { vm.repo.addChecklistItem(it.copy(taskId = task.id)) }
                if (invitePermission == "Sem convite") {
                    onCancel()
                } else {
                    scope.launch {
                        val permission = if (invitePermission == "Ver") UserPermission.Viewer else UserPermission.Participant
                        val invite = Invite(taskId = task.id, createdBy = user.id, permission = permission)
                        vm.createRemoteInvite(invite, task)
                            .onSuccess {
                                shareDraftInvite(context, inviteChannel, task.title, buildDraftInviteText(task, invite))
                                message = "Convite criado: ${InviteLinks.urlForToken(invite.token)}"
                                onCancel()
                            }
                            .onFailure { message = "Tarefa salva, mas o link real nao foi gerado." }
                    }
                }
            }, Modifier.fillMaxWidth().testTag("save-new-task").semantics { contentDescription = "Salvar nova tarefa" }, enabled = visibleLists.isNotEmpty())
        }
    }
}

private fun buildDraftInviteText(task: Task, invite: Invite): String {
    return "Voce foi convidado para participar da tarefa \"${task.title}\" no TaskFlow.\nPermissao: ${invite.permission.label}\nLink: ${InviteLinks.urlForToken(invite.token)}"
}

private fun shareDraftInvite(context: Context, channel: String, taskTitle: String, text: String) {
    when (channel) {
        "Copiar" -> {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboard.setPrimaryClip(ClipData.newPlainText("Convite TaskFlow", text))
        }
        "E-mail" -> {
            val email = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("mailto:")
                putExtra(Intent.EXTRA_SUBJECT, "Convite TaskFlow: $taskTitle")
                putExtra(Intent.EXTRA_TEXT, text)
            }
            runCatching { context.startActivity(email) }
        }
        else -> {
            val send = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, "Convite TaskFlow: $taskTitle")
                putExtra(Intent.EXTRA_TEXT, text)
                if (channel == "WhatsApp") setPackage("com.whatsapp")
            }
            try {
                context.startActivity(Intent.createChooser(send, "Compartilhar tarefa"))
            } catch (_: ActivityNotFoundException) {
                context.startActivity(Intent.createChooser(send.apply { setPackage(null) }, "Compartilhar tarefa"))
            }
        }
    }
}

@Composable
private fun DraftLine(title: String, subtitle: String) {
    Row(Modifier.fillMaxWidth().padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        IconBubble(Icons.Default.AttachFile, TaskFlowColors.Blue.copy(.10f), TaskFlowColors.Blue)
        Column(Modifier.weight(1f)) {
            Text(title, color = TaskFlowColors.Text, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(subtitle, color = TaskFlowColors.Muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}
