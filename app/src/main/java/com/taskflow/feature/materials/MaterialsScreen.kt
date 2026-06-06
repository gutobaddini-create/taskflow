package com.taskflow.feature.materials

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.OpenableColumns
import android.util.Size
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.IosShare
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.taskflow.core.design.ChipText
import com.taskflow.core.design.EmptyState
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
import com.taskflow.core.design.TopRow
import com.taskflow.core.design.LoadingFullScreen
import com.taskflow.core.design.NameDialog
import com.taskflow.core.permissions.PermissionPolicy
import com.taskflow.core.utils.attachmentType
import com.taskflow.core.utils.isAllowedAttachment
import com.taskflow.core.utils.isValidUrl
import com.taskflow.domain.model.Attachment
import com.taskflow.domain.model.AttachmentSource
import com.taskflow.domain.model.AttachmentType
import com.taskflow.domain.model.ChecklistItem
import com.taskflow.domain.model.CustomField
import com.taskflow.domain.model.CustomFieldType
import com.taskflow.domain.model.TaskLink
import java.io.File
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
private val CustomFieldTypeLabels = listOf(
    CustomFieldType.Text to "Texto",
    CustomFieldType.Number to "Numero",
    CustomFieldType.Money to "Moeda",
    CustomFieldType.Date to "Data",
    CustomFieldType.Phone to "Telefone",
    CustomFieldType.Email to "E-mail",
    CustomFieldType.Url to "URL",
    CustomFieldType.Location to "Localizacao",
    CustomFieldType.ProcessNumber to "Processo",
    CustomFieldType.Document to "Documento"
)

private fun CustomFieldType.label(): String = CustomFieldTypeLabels.firstOrNull { it.first == this }?.second ?: name

@Composable
fun MaterialsScreen(vm: TaskFlowViewModel, onBack: () -> Unit) {
    val context = LocalContext.current
    val task = vm.selectedTask()
    val users by vm.users.collectAsState()
    val preferences by vm.preferences.collectAsState()
    val invites by vm.invites.collectAsState()
    val attachments by vm.attachments.collectAsState()
    val links by vm.links.collectAsState()
    val fields by vm.customFields.collectAsState()
    val checklist by vm.checklist.collectAsState()
    var linkDialog by remember { mutableStateOf(false) }
    var fieldDialog by remember { mutableStateOf(false) }
    var checklistDialog by remember { mutableStateOf(false) }
    var editingLink by remember { mutableStateOf<TaskLink?>(null) }
    var editingField by remember { mutableStateOf<CustomField?>(null) }
    var editingChecklist by remember { mutableStateOf<ChecklistItem?>(null) }
    var message by remember { mutableStateOf<String?>(null) }
    var cameraUri by remember { mutableStateOf<Uri?>(null) }
    var isAddingAttachment by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    if (task == null) {
        LoadingFullScreen("Carregando materiais...")
        return
    }
    val currentUser = users.firstOrNull { it.id == preferences.currentUserId } ?: users.firstOrNull() ?: vm.currentUser()
    val effectivePermission = PermissionPolicy.acceptedPermission(task.id, currentUser.id, invites)
    val canViewMaterial = PermissionPolicy.canViewMaterial(task, currentUser.id, effectivePermission)
    val canManageMaterial = PermissionPolicy.canManageMaterial(task, currentUser.id, effectivePermission)
    if (!canViewMaterial) {
        LazyColumn(Modifier.fillMaxSize().statusBarsPadding().padding(22.dp), contentPadding = PaddingValues(bottom = 30.dp)) {
            item {
                TopRow("<", "Materiais da tarefa", onBack)
                Spacer(Modifier.height(24.dp))
                TaskFlowCard {
                    Icon(Icons.Default.Lock, null, tint = com.taskflow.core.design.TaskFlowColors.Muted, modifier = Modifier.size(42.dp))
                    Text("Sem acesso aos materiais", fontWeight = FontWeight.Bold, color = com.taskflow.core.design.TaskFlowColors.Text, modifier = Modifier.padding(top = 12.dp))
                    Text("Anexos, links e campos ficam disponiveis apenas para participantes autorizados.", color = com.taskflow.core.design.TaskFlowColors.Muted, modifier = Modifier.padding(top = 6.dp))
                }
            }
        }
        return
    }
    fun addUriAttachment(uri: Uri, source: AttachmentSource) {
        if (isAddingAttachment) {
            message = "Aguarde o anexo atual terminar."
            return
        }
        scope.launch {
            isAddingAttachment = true
            val result = runCatching {
                val metadata = context.attachmentMetadata(uri)
                if (!isAllowedAttachment(metadata.name, metadata.sizeBytes)) {
                    error("Arquivo invalido ou maior que 20 MB.")
                }
                vm.repo.addAttachment(
                    Attachment(
                        taskId = task.id,
                        uploadedBy = vm.currentUser().id,
                        fileName = metadata.name,
                        originalFileName = metadata.name,
                        fileType = attachmentType(metadata.name),
                        mimeType = metadata.mimeType,
                        fileSize = metadata.sizeBytes,
                        storagePath = uri.toString(),
                        source = source
                    )
                )
            }
            delay(300)
            message = result.fold(
                onSuccess = { "Anexo adicionado." },
                onFailure = { it.message ?: "Nao foi possivel adicionar o anexo." }
            )
            isAddingAttachment = false
        }
    }

    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let {
            context.contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addUriAttachment(it, AttachmentSource.FilePicker)
        }
    }
    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let {
            runCatching { context.contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
            addUriAttachment(it, AttachmentSource.Gallery)
        }
    }
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) cameraUri?.let { addUriAttachment(it, AttachmentSource.Camera) }
    }
    fun openCamera() {
        cameraUri = context.createCameraUri()
        cameraUri?.let(cameraLauncher::launch)
    }
    val cameraPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) openCamera() else message = "Permissao de camera negada."
    }
    fun openFilePicker(mimeTypes: Array<String>) {
        when {
            !canManageMaterial -> message = "Sem permissao para alterar materiais."
            isAddingAttachment -> message = "Aguarde o anexo atual terminar."
            else -> filePicker.launch(mimeTypes)
        }
    }

    fun openImagePicker() {
        when {
            !canManageMaterial -> message = "Sem permissao para alterar materiais."
            isAddingAttachment -> message = "Aguarde o anexo atual terminar."
            else -> photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }
    }

    fun requestCamera() {
        when {
            !canManageMaterial -> message = "Sem permissao para alterar materiais."
            isAddingAttachment -> message = "Aguarde o anexo atual terminar."
            context.checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED -> openCamera()
            else -> cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }
    fun openAttachment(attachment: Attachment) {
        val uri = runCatching { Uri.parse(attachment.storagePath) }.getOrNull()
        if (uri == null || uri.scheme !in setOf("content", "file")) {
            message = "Anexo de exemplo sem arquivo local para abrir."
            return
        }
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, attachment.mimeType)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        runCatching { context.startActivity(intent) }
            .onFailure { message = "Nenhum app disponivel para abrir este anexo." }
    }
    fun shareAttachment(attachment: Attachment) {
        val uri = runCatching { Uri.parse(attachment.storagePath) }.getOrNull()
        if (uri == null || uri.scheme !in setOf("content", "file")) {
            message = "Anexo de exemplo sem arquivo local para compartilhar."
            return
        }
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = attachment.mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        runCatching { context.startActivity(Intent.createChooser(intent, "Compartilhar anexo")) }
            .onFailure { message = "Nao foi possivel compartilhar o anexo." }
    }
    fun openLink(link: TaskLink) {
        runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(link.url))) }
            .onFailure { message = "Nao foi possivel abrir o link." }
    }
    fun copyLink(link: TaskLink) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText(link.title, link.url))
        message = "Link copiado."
    }
    if (linkDialog || editingLink != null) {
        val current = editingLink
        LinkDialog(
            initialTitle = current?.title ?: "Link de referencia",
            initialUrl = current?.url ?: "https://taskflow.local/referencia",
            initialDescription = current?.description ?: "",
            initialCategory = current?.category ?: "Geral",
            onDismiss = { linkDialog = false; editingLink = null },
            onSave = { title, url, description, category ->
                if (!isValidUrl(url)) {
                    message = "URL invalida."
                } else {
                    if (current == null) {
                        vm.repo.addLink(TaskLink(taskId = task.id, createdBy = vm.currentUser().id, title = title, url = url, description = description, category = category))
                    } else {
                        vm.repo.updateLink(current.copy(title = title, url = url, description = description, category = category))
                    }
                    linkDialog = false
                    editingLink = null
                    message = if (current == null) "Link salvo." else "Link atualizado."
                }
            }
        )
    }
    if (fieldDialog || editingField != null) {
        val current = editingField
        FieldDialog(
            initialName = current?.fieldName ?: "Contato",
            initialType = current?.fieldType ?: CustomFieldType.Phone,
            initialValue = current?.fieldValue ?: "(11) 99999-0000",
            onDismiss = { fieldDialog = false; editingField = null },
            onSave = { name, type, value ->
                if (current == null) {
                    vm.repo.addCustomField(CustomField(taskId = task.id, createdBy = vm.currentUser().id, fieldName = name, fieldType = type, fieldValue = value))
                } else {
                    vm.repo.updateCustomField(current.copy(fieldName = name, fieldType = type, fieldValue = value))
                }
                fieldDialog = false
                editingField = null
                message = if (current == null) "Campo salvo." else "Campo atualizado."
            }
        )
    }
    if (checklistDialog || editingChecklist != null) {
        val current = editingChecklist
        NameDialog(if (current == null) "Novo item" else "Editar item", current?.title ?: "", { checklistDialog = false; editingChecklist = null }) { value ->
            if (current == null) {
                vm.repo.addChecklistItem(ChecklistItem(taskId = task.id, title = value))
            } else {
                vm.repo.updateChecklistItem(current.copy(title = value))
            }
            checklistDialog = false
            editingChecklist = null
            message = if (current == null) "Item adicionado." else "Item atualizado."
        }
    }
    LazyColumn(Modifier.fillMaxSize().statusBarsPadding().padding(22.dp), contentPadding = PaddingValues(bottom = 30.dp)) {
        item {
            TopRow("<", "Materiais da tarefa", onBack)
            Spacer(Modifier.height(18.dp))
            Segmented(listOf("Anexos", "Links", "Campos"), vm.materialsTab) { vm.materialsTab = it }
            Spacer(Modifier.height(16.dp))
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    SmallAction(Icons.Default.AttachFile, "Arquivo", Modifier.weight(1f)) {
                        openFilePicker(arrayOf("application/pdf", "application/msword", "application/vnd.openxmlformats-officedocument.wordprocessingml.document", "application/vnd.ms-excel", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "text/plain"))
                    }
                    SmallAction(Icons.Default.PhotoCamera, "Foto", Modifier.weight(1f)) {
                        requestCamera()
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    SmallAction(Icons.Default.Image, "Imagem", Modifier.weight(1f)) {
                        openImagePicker()
                    }
                    SmallAction(Icons.Default.Link, "Link", Modifier.weight(1f)) {
                        if (canManageMaterial) linkDialog = true else message = "Sem permissao para alterar materiais."
                    }
                }
            }
            AttachmentUploadProgress(isAddingAttachment)
            FeedbackBanner(message, materialFeedbackKind(message), Modifier.padding(top = 10.dp))
            Spacer(Modifier.height(16.dp))
            TaskFlowCard(Modifier.border(1.dp, com.taskflow.core.design.TaskFlowColors.Purple.copy(.35f), RoundedCornerShape(22.dp)).clickable {
                openFilePicker(arrayOf("*/*"))
            }) {
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.CloudUpload, null, tint = com.taskflow.core.design.TaskFlowColors.Purple, modifier = Modifier.size(42.dp))
                    Text("Toque para selecionar", fontWeight = FontWeight.Bold, color = com.taskflow.core.design.TaskFlowColors.Text)
                    Text("PDF, DOC, XLS, JPG, PNG ate 20 MB", color = com.taskflow.core.design.TaskFlowColors.Muted, fontSize = 13.sp)
                }
            }
            SectionTitle(vm.materialsTab)
            when (vm.materialsTab) {
                "Anexos" -> {
                    val taskAttachments = attachments.filter { it.taskId == task.id }
                    if (taskAttachments.isEmpty()) {
                        EmptyState("Nenhum anexo", "Adicione arquivos, fotos ou documentos vinculados a esta tarefa.")
                    } else {
                        taskAttachments.forEach {
                            AttachmentRow(
                                attachment = it,
                                canManage = canManageMaterial,
                                onOpen = { openAttachment(it) },
                                onShare = { shareAttachment(it) },
                                onDelete = {
                                    vm.repo.deleteAttachment(it.id)
                                    message = "Anexo removido."
                                }
                            )
                        }
                    }
                }
                "Links" -> {
                    val taskLinks = links.filter { it.taskId == task.id }
                    if (taskLinks.isEmpty()) {
                        EmptyState("Nenhum link", "Adicione URLs importantes para consulta rapida.")
                    } else {
                        taskLinks.forEach {
                            LinkRow(
                                link = it,
                                canManage = canManageMaterial,
                                onOpen = { openLink(it) },
                                onCopy = { copyLink(it) },
                                onEdit = { editingLink = it },
                                onDelete = {
                                    vm.repo.deleteLink(it.id)
                                    message = "Link removido."
                                }
                            )
                        }
                    }
                }
                else -> {
                    val taskFields = fields.filter { it.taskId == task.id }
                    if (taskFields.isEmpty()) {
                        EmptyState("Nenhum campo", "Adicione informacoes extras como contatos, protocolos ou valores.")
                    } else {
                        taskFields.forEach {
                            FieldRow(
                                field = it,
                                canManage = canManageMaterial,
                                onEdit = { editingField = it },
                                onDelete = {
                                    vm.repo.deleteCustomField(it.id)
                                    message = "Campo removido."
                                }
                            )
                        }
                    }
                }
            }
            SectionTitle("Checklist")
            val taskChecklist = checklist.filter { it.taskId == task.id }
            TaskFlowCard {
                if (taskChecklist.isEmpty()) {
                    EmptyState("Checklist vazio", "Adicione itens para acompanhar subtarefas.")
                } else {
                    taskChecklist.forEach { item ->
                        Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = item.isDone,
                                onCheckedChange = { if (canManageMaterial) vm.repo.toggleChecklistItem(item.id) else message = "Sem permissao para alterar materiais." },
                                enabled = canManageMaterial
                            )
                            Text(item.title, color = com.taskflow.core.design.TaskFlowColors.Text, modifier = Modifier.weight(1f))
                            if (canManageMaterial) {
                                IconButton({ editingChecklist = item }) { Icon(Icons.Default.Edit, null, tint = com.taskflow.core.design.TaskFlowColors.Muted) }
                                IconButton({ vm.repo.deleteChecklistItem(item.id); message = "Item removido." }) { Icon(Icons.Default.DeleteOutline, null, tint = com.taskflow.core.design.TaskFlowColors.Muted) }
                            }
                        }
                    }
                    val done = taskChecklist.count { it.isDone }
                    Text("$done/${taskChecklist.size} concluidos", color = com.taskflow.core.design.TaskFlowColors.Muted)
                }
            }
            Spacer(Modifier.height(18.dp))
            if (canManageMaterial && vm.materialsTab == "Campos") TextButton({ fieldDialog = true }, Modifier.fillMaxWidth()) { Text("Adicionar campo personalizado") }
            if (canManageMaterial) TextButton({ checklistDialog = true }, Modifier.fillMaxWidth()) { Text("Adicionar item do checklist") }
        }
    }
}

@Composable
fun LinkDialog(initialTitle: String, initialUrl: String, initialDescription: String, initialCategory: String, onDismiss: () -> Unit, onSave: (String, String, String, String) -> Unit) {
    var title by remember(initialTitle) { mutableStateOf(initialTitle) }
    var url by remember(initialUrl) { mutableStateOf(initialUrl) }
    var description by remember(initialDescription) { mutableStateOf(initialDescription) }
    var category by remember(initialCategory) { mutableStateOf(initialCategory) }
    var urlError by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Novo link", color = com.taskflow.core.design.TaskFlowColors.Text, fontWeight = FontWeight.Bold) },
        text = {
            Column {
                OutlinedTextField(title, { title = it }, label = { Text("Titulo") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(
                    value = url,
                    onValueChange = {
                        url = it
                        urlError = false
                    },
                    label = { Text("URL") },
                    singleLine = true,
                    isError = urlError,
                    supportingText = { if (urlError) Text("URL invalida.") },
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                )
                OutlinedTextField(description, { description = it }, label = { Text("Descricao") }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
                OutlinedTextField(category, { category = it }, label = { Text("Categoria") }, singleLine = true, modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
            }
        },
        confirmButton = {
            TextButton({
                if (title.isNotBlank() && url.isNotBlank()) {
                    val trimmedUrl = url.trim()
                    if (isValidUrl(trimmedUrl)) {
                        onSave(title.trim(), trimmedUrl, description.trim(), category.trim())
                    } else {
                        urlError = true
                    }
                }
            }) { Text("Salvar") }
        },
        dismissButton = { TextButton(onDismiss) { Text("Cancelar") } }
    )
}

@Composable
fun FieldDialog(initialName: String, initialType: CustomFieldType, initialValue: String, onDismiss: () -> Unit, onSave: (String, CustomFieldType, String) -> Unit) {
    var name by remember(initialName) { mutableStateOf(initialName) }
    var value by remember(initialValue) { mutableStateOf(initialValue) }
    var type by remember(initialType) { mutableStateOf(initialType) }
    var typeMenuExpanded by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Campo complementar", color = com.taskflow.core.design.TaskFlowColors.Text, fontWeight = FontWeight.Bold) },
        text = {
            Column {
                OutlinedTextField(name, { name = it }, label = { Text("Nome") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                Box {
                    OutlinedButton(
                        onClick = { typeMenuExpanded = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp)
                    ) {
                        Text(type.label(), modifier = Modifier.weight(1f))
                        Icon(Icons.Default.ArrowDropDown, null)
                    }
                    DropdownMenu(expanded = typeMenuExpanded, onDismissRequest = { typeMenuExpanded = false }) {
                        CustomFieldTypeLabels.forEach { (option, label) ->
                            DropdownMenuItem(
                                text = { Text(label) },
                                onClick = {
                                    type = option
                                    typeMenuExpanded = false
                                }
                            )
                        }
                    }
                }
                OutlinedTextField(value, { value = it }, label = { Text("Valor") }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
            }
        },
        confirmButton = { TextButton({ if (name.isNotBlank() && value.isNotBlank()) onSave(name.trim(), type, value.trim()) }) { Text("Salvar") } },
        dismissButton = { TextButton(onDismiss) { Text("Cancelar") } }
    )
}

@Composable
private fun AttachmentUploadProgress(isVisible: Boolean) {
    if (!isVisible) return
    TaskFlowCard(Modifier.padding(top = 10.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            CircularProgressIndicator(
                modifier = Modifier.size(22.dp),
                strokeWidth = 2.dp,
                color = com.taskflow.core.design.TaskFlowColors.Purple
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("Preparando anexo", fontWeight = FontWeight.Bold, color = com.taskflow.core.design.TaskFlowColors.Text)
                Text("Validando tipo, tamanho e metadados locais.", color = com.taskflow.core.design.TaskFlowColors.Muted, fontSize = 13.sp)
            }
        }
    }
}

@Composable
fun AttachmentRow(attachment: Attachment, canManage: Boolean = true, onOpen: () -> Unit, onShare: () -> Unit, onDelete: () -> Unit) = TaskFlowCard(Modifier.padding(bottom = 10.dp)) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        AttachmentPreview(attachment)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(attachment.fileName, fontWeight = FontWeight.Bold, color = com.taskflow.core.design.TaskFlowColors.Text)
            Text("${attachment.fileType.name} - ${attachment.fileSize / 1024} KB", color = com.taskflow.core.design.TaskFlowColors.Muted, maxLines = 1)
        }
        ItemActionMenu(
            contentDescription = "Menu do anexo ${attachment.fileName}",
            actions = listOfNotNull(
                ItemAction("Abrir", Icons.AutoMirrored.Filled.OpenInNew, onOpen),
                ItemAction("Compartilhar", Icons.Default.IosShare, onShare),
                if (canManage) ItemAction("Excluir", Icons.Default.DeleteOutline, onDelete) else null
            )
        )
    }
    Row(Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        SmallAction(Icons.AutoMirrored.Filled.OpenInNew, "Abrir", Modifier.weight(1f), onOpen)
        SmallAction(Icons.Default.IosShare, "Compart.", Modifier.weight(1f), onShare)
        if (canManage) SmallAction(Icons.Default.DeleteOutline, "Excluir", Modifier.weight(1f), onDelete)
    }
}

@Composable
fun AttachmentPreview(attachment: Attachment) {
    val context = LocalContext.current
    val imageBitmap = remember(attachment.storagePath, attachment.fileType) {
        if (attachment.fileType != AttachmentType.Image) {
            null
        } else loadAttachmentImageBitmap(context, attachment.storagePath)
    }
    if (imageBitmap != null) {
        Image(
            bitmap = imageBitmap,
            contentDescription = "Miniatura de ${attachment.fileName}",
            contentScale = ContentScale.Crop,
            modifier = Modifier.size(42.dp).clip(RoundedCornerShape(14.dp))
        )
    } else {
        IconBubble(if (attachment.fileType == AttachmentType.Image) Icons.Default.Image else Icons.Default.Description, com.taskflow.core.design.TaskFlowColors.Purple.copy(.10f), com.taskflow.core.design.TaskFlowColors.Purple)
    }
}

private fun loadAttachmentImageBitmap(context: Context, storagePath: String) = runCatching {
    val uri = Uri.parse(storagePath)
    when (uri.scheme) {
        "content" -> {
            val decoded = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) runCatching {
                ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, uri)).asImageBitmap()
            }.getOrNull() else null
            val thumbnail = if (decoded == null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) runCatching {
                context.contentResolver.loadThumbnail(uri, Size(96, 96), null).asImageBitmap()
            }.getOrNull() else null
            decoded ?: thumbnail ?: context.contentResolver.openInputStream(uri)?.use { stream -> BitmapFactory.decodeStream(stream)?.asImageBitmap() }
        }
        "file" -> context.contentResolver.openInputStream(uri)?.use { stream -> BitmapFactory.decodeStream(stream)?.asImageBitmap() }
        else -> null
    }
}.getOrNull()
@Composable
fun LinkRow(link: TaskLink, canManage: Boolean = true, onOpen: () -> Unit, onCopy: () -> Unit, onEdit: () -> Unit, onDelete: () -> Unit) = TaskFlowCard(Modifier.padding(bottom = 10.dp)) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconBubble(Icons.Default.Link, com.taskflow.core.design.TaskFlowColors.Purple.copy(.10f), com.taskflow.core.design.TaskFlowColors.Purple)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(link.title, fontWeight = FontWeight.Bold, color = com.taskflow.core.design.TaskFlowColors.Text)
            Text(link.url, color = com.taskflow.core.design.TaskFlowColors.Muted, maxLines = 1)
        }
        ItemActionMenu(
            contentDescription = "Menu do link ${link.title}",
            actions = listOfNotNull(
                ItemAction("Abrir", Icons.Default.OpenInBrowser, onOpen),
                ItemAction("Copiar", Icons.Default.ContentCopy, onCopy),
                if (canManage) ItemAction("Editar", Icons.Default.Edit, onEdit) else null,
                if (canManage) ItemAction("Excluir", Icons.Default.DeleteOutline, onDelete) else null
            )
        )
    }
    Row(Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        SmallAction(Icons.Default.OpenInBrowser, "Abrir", Modifier.weight(1f), onOpen)
        SmallAction(Icons.Default.ContentCopy, "Copiar", Modifier.weight(1f), onCopy)
    }
    if (canManage) {
        Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SmallAction(Icons.Default.Edit, "Editar", Modifier.weight(1f), onEdit)
            SmallAction(Icons.Default.DeleteOutline, "Excluir", Modifier.weight(1f), onDelete)
        }
    }
}
@Composable
fun FieldRow(field: CustomField, canManage: Boolean = true, onEdit: () -> Unit, onDelete: () -> Unit) = TaskFlowCard(Modifier.padding(bottom = 10.dp)) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconBubble(Icons.Default.EditNote, com.taskflow.core.design.TaskFlowColors.Purple.copy(.10f), com.taskflow.core.design.TaskFlowColors.Purple)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(field.fieldName, fontWeight = FontWeight.Bold, color = com.taskflow.core.design.TaskFlowColors.Text)
            Text("${field.fieldType.label()} - ${field.fieldValue}", color = com.taskflow.core.design.TaskFlowColors.Muted, maxLines = 1)
        }
        if (canManage) {
            ItemActionMenu(
                contentDescription = "Menu do campo ${field.fieldName}",
                actions = listOf(
                    ItemAction("Editar", Icons.Default.Edit, onEdit),
                    ItemAction("Excluir", Icons.Default.DeleteOutline, onDelete)
                )
            )
        }
    }
    if (canManage) {
        Row(Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SmallAction(Icons.Default.Edit, "Editar", Modifier.weight(1f), onEdit)
            SmallAction(Icons.Default.DeleteOutline, "Excluir", Modifier.weight(1f), onDelete)
        }
    }
}

data class ItemAction(val label: String, val icon: ImageVector, val onClick: () -> Unit)

@Composable
fun ItemActionMenu(contentDescription: String, actions: List<ItemAction>) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        IconButton(
            onClick = { expanded = true },
            modifier = Modifier.semantics { this.contentDescription = contentDescription }
        ) {
            Icon(Icons.Default.MoreVert, null, tint = com.taskflow.core.design.TaskFlowColors.Muted)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            actions.forEach { action ->
                DropdownMenuItem(
                    text = { Text(action.label) },
                    leadingIcon = { Icon(action.icon, null, tint = com.taskflow.core.design.TaskFlowColors.Muted) },
                    onClick = {
                        expanded = false
                        action.onClick()
                    }
                )
            }
        }
    }
}

data class AttachmentMetadata(val name: String, val sizeBytes: Long, val mimeType: String)

private fun materialFeedbackKind(message: String?): FeedbackKind {
    val value = message ?: return FeedbackKind.Info
    return if (
        value.contains("invalida", ignoreCase = true) ||
        value.contains("invalido", ignoreCase = true) ||
        value.contains("negada", ignoreCase = true) ||
        value.contains("Sem permissao", ignoreCase = true) ||
        value.contains("Nao foi possivel", ignoreCase = true) ||
        value.contains("Nenhum app", ignoreCase = true)
    ) {
        FeedbackKind.Error
    } else {
        FeedbackKind.Success
    }
}

fun Context.attachmentMetadata(uri: Uri): AttachmentMetadata {
    var name = uri.lastPathSegment?.substringAfterLast('/') ?: "anexo-${System.currentTimeMillis()}"
    var size = 0L
    var cursor: Cursor? = null
    try {
        cursor = contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE), null, null, null)
        if (cursor != null && cursor.moveToFirst()) {
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
            if (nameIndex >= 0) name = cursor.getString(nameIndex) ?: name
            if (sizeIndex >= 0 && !cursor.isNull(sizeIndex)) size = cursor.getLong(sizeIndex)
        }
    } finally {
        cursor?.close()
    }
    val mimeType = contentResolver.getType(uri) ?: when (attachmentType(name)) {
        AttachmentType.Image -> "image/jpeg"
        AttachmentType.Pdf -> "application/pdf"
        AttachmentType.Document -> "application/msword"
        AttachmentType.Spreadsheet -> "application/vnd.ms-excel"
        AttachmentType.Text -> "text/plain"
        AttachmentType.Other -> "application/octet-stream"
    }
    if (size <= 0L) size = 1L
    if (name.substringAfterLast('.', "").isBlank()) {
        val ext = when {
            mimeType.startsWith("image/") -> "jpg"
            mimeType == "application/pdf" -> "pdf"
            mimeType == "text/plain" -> "txt"
            else -> "dat"
        }
        name = "$name.$ext"
    }
    return AttachmentMetadata(name, size, mimeType)
}

fun Context.createCameraUri(): Uri {
    val dir = File(cacheDir, "camera").apply { mkdirs() }
    val file = File(dir, "taskflow-${System.currentTimeMillis()}.jpg")
    return FileProvider.getUriForFile(this, "$packageName.fileprovider", file)
}

