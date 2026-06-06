package com.taskflow

import android.Manifest
import android.os.Bundle
import android.app.Application
import android.content.ActivityNotFoundException
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
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import androidx.core.content.FileProvider
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewModelScope
import com.taskflow.data.local.TaskFlowPreferences
import com.taskflow.data.local.TaskFlowUserPreferences
import com.taskflow.data.local.TaskFlowDatabase
import com.taskflow.data.repository.LocalTaskFlowRepository
import com.taskflow.core.notifications.ReminderEngine
import com.taskflow.core.notifications.ReminderReceiver
import com.taskflow.core.permissions.PermissionPolicy
import com.taskflow.core.utils.attachmentType
import com.taskflow.core.utils.isAllowedAttachment
import com.taskflow.core.utils.isValidUrl
import com.taskflow.core.utils.TaskSearch
import com.taskflow.domain.model.*
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

private val OffWhite = Color(0xFFF7F8FC)
private val Text = Color(0xFF07132F)
private val Muted = Color(0xFF667085)
private val Blue = Color(0xFF2563FF)
private val Purple = Color(0xFF7C3AED)
private val Border = Color(0xFFE5E7EB)
private val Gradient = Brush.horizontalGradient(listOf(Blue, Purple))
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

class MainActivity : ComponentActivity() {
    private val pendingInviteToken = mutableStateOf<String?>(null)
    private val pendingTaskId = mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pendingInviteToken.value = intent.inviteToken()
        pendingTaskId.value = intent.notificationTaskId()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1001)
        }
        setContent {
            TaskFlowRoot(
                inviteToken = pendingInviteToken.value,
                notificationTaskId = pendingTaskId.value,
                onInviteHandled = { pendingInviteToken.value = null },
                onNotificationTaskHandled = { pendingTaskId.value = null }
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        pendingInviteToken.value = intent.inviteToken()
        pendingTaskId.value = intent.notificationTaskId()
    }
}

private fun Intent.inviteToken(): String? = data?.takeIf { it.scheme == "taskflow" && it.host == "invite" }?.lastPathSegment
private fun Intent.notificationTaskId(): String? = getStringExtra(ReminderReceiver.EXTRA_TASK_ID)

class TaskFlowViewModel(application: Application) : AndroidViewModel(application) {
    val repo = LocalTaskFlowRepository(TaskFlowDatabase.get(application).dao(), application, viewModelScope)
    private val preferencesStore = TaskFlowPreferences(application)
    val users = repo.users
    val spaces = repo.spaces
    val lists = repo.lists
    val tasks = repo.tasks
    val reminders = repo.reminders
    val attachments = repo.attachments
    val links = repo.links
    val customFields = repo.customFields
    val checklist = repo.checklist
    val comments = repo.comments
    val invites = repo.invites
    val activity = repo.activity
    private val _preferences = MutableStateFlow(TaskFlowUserPreferences())
    val preferences: StateFlow<TaskFlowUserPreferences> = _preferences
    var remindersVisible by mutableStateOf(true)
        private set
    var homeFilter by mutableStateOf("Hoje")
        private set
    var selectedTaskId by mutableStateOf<String?>(null)
    var materialsTab by mutableStateOf("Anexos")

    init {
        viewModelScope.launch {
            preferencesStore.values.collect { prefs ->
                _preferences.value = prefs
                remindersVisible = prefs.remindersVisible
                homeFilter = prefs.homeFilter
            }
        }
    }

    fun updateHomeFilter(value: String) {
        homeFilter = value
        viewModelScope.launch { preferencesStore.setHomeFilter(value) }
    }

    fun updateRemindersVisible(value: Boolean) {
        remindersVisible = value
        viewModelScope.launch {
            preferencesStore.setRemindersVisible(value)
            preferencesStore.setNotificationsEnabled(value)
        }
    }

    fun setTheme(value: String) {
        viewModelScope.launch { preferencesStore.setTheme(value) }
    }

    fun setNotificationsEnabled(value: Boolean) {
        remindersVisible = value
        viewModelScope.launch {
            preferencesStore.setNotificationsEnabled(value)
            preferencesStore.setRemindersVisible(value)
        }
    }

    fun setCurrentUserIfNeeded(userId: String) {
        if (preferences.value.currentUserId.isBlank()) {
            viewModelScope.launch { preferencesStore.setCurrentUserId(userId) }
        }
    }

    fun setCurrentUser(userId: String) {
        viewModelScope.launch { preferencesStore.setCurrentUserId(userId) }
    }

    fun logoutLocal() {
        viewModelScope.launch { preferencesStore.setCurrentUserId("") }
    }

    fun currentUser(): User {
        val currentId = preferences.value.currentUserId
        return users.value.firstOrNull { it.id == currentId } ?: users.value.firstOrNull() ?: User(name = "Manuel", email = "manuel@taskflow.local")
    }
    fun selectedTask(): Task? = tasks.value.firstOrNull { it.id == selectedTaskId } ?: tasks.value.firstOrNull()
}

sealed class Screen(val label: String) {
    data object Onboarding : Screen("Inicio")
    data object Home : Screen("Hoje")
    data object Spaces : Screen("Listas")
    data object People : Screen("Pessoas")
    data object Settings : Screen("Ajustes")
    data object NewTask : Screen("Nova tarefa")
    data object Detail : Screen("Detalhe")
    data object Reminder : Screen("Lembrete")
    data object Materials : Screen("Materiais")
    data object Share : Screen("Compartilhar")
    data object AcceptInvite : Screen("Convite")
}

@Composable
fun TaskFlowRoot(
    inviteToken: String? = null,
    notificationTaskId: String? = null,
    onInviteHandled: () -> Unit = {},
    onNotificationTaskHandled: () -> Unit = {},
    vm: TaskFlowViewModel = viewModel()
) {
    var screen by remember { mutableStateOf<Screen>(Screen.Onboarding) }
    val tasks by vm.tasks.collectAsState()
    LaunchedEffect(inviteToken) {
        if (!inviteToken.isNullOrBlank()) screen = Screen.AcceptInvite
    }
    LaunchedEffect(notificationTaskId, tasks) {
        if (!notificationTaskId.isNullOrBlank() && tasks.any { it.id == notificationTaskId }) {
            vm.selectedTaskId = notificationTaskId
            screen = Screen.Detail
            onNotificationTaskHandled()
        }
    }
    MaterialTheme(colorScheme = lightColorScheme(primary = Blue, secondary = Purple, background = OffWhite)) {
        Surface(Modifier.fillMaxSize(), color = OffWhite) {
            when (screen) {
                Screen.Onboarding -> OnboardingScreen { screen = Screen.Home }
                Screen.Home -> Shell(screen, { screen = it }) { HomeScreen(vm, { screen = Screen.NewTask }, { vm.selectedTaskId = it; screen = Screen.Detail }) }
                Screen.Spaces -> Shell(screen, { screen = it }) { SpacesScreen(vm, { vm.selectedTaskId = it; screen = Screen.Detail }) }
                Screen.People -> Shell(screen, { screen = it }) { PeopleScreen(vm) }
                Screen.Settings -> Shell(screen, { screen = it }) { SettingsScreen(vm) { vm.logoutLocal(); screen = Screen.Onboarding } }
                Screen.NewTask -> NewTaskScreen(vm, { screen = Screen.Home }, { screen = Screen.Reminder }, { screen = Screen.Materials })
                Screen.Detail -> DetailScreen(vm, { screen = Screen.Home }, { screen = Screen.Materials }, { screen = Screen.Share }, { screen = Screen.Reminder })
                Screen.Reminder -> ReminderScreen(vm) { screen = Screen.NewTask }
                Screen.Materials -> MaterialsScreen(vm) { screen = Screen.Detail }
                Screen.Share -> ShareScreen(vm) { screen = Screen.Detail }
                Screen.AcceptInvite -> AcceptInviteScreen(vm, inviteToken, { onInviteHandled(); screen = Screen.Home }, { onInviteHandled(); screen = Screen.Onboarding })
            }
        }
    }
}

@Composable
fun Shell(current: Screen, navigate: (Screen) -> Unit, content: @Composable () -> Unit) {
    Box(Modifier.fillMaxSize()) {
        content()
        NavigationBar(
            Modifier.align(Alignment.BottomCenter).padding(horizontal = 18.dp, vertical = 14.dp).clip(RoundedCornerShape(26.dp)),
            containerColor = Color.White
        ) {
            navItems().forEach { (screen, icon) ->
                NavigationBarItem(selected = current == screen, onClick = { navigate(screen) }, icon = { Icon(icon, null) }, label = { Text(screen.label) })
            }
        }
    }
}

fun navItems() = listOf(Screen.Home to Icons.Default.CalendarToday, Screen.Spaces to Icons.Default.List, Screen.People to Icons.Default.Groups, Screen.Settings to Icons.Default.Settings)

@Composable
fun OnboardingScreen(onStart: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Icon(Icons.Default.NotificationsActive, null, tint = Purple, modifier = Modifier.size(86.dp))
        Spacer(Modifier.height(26.dp))
        Text("TaskFlow", fontSize = 38.sp, fontWeight = FontWeight.Bold, color = Text)
        Text("Organize tarefas, lembretes e materiais em um fluxo unico.", color = Muted, modifier = Modifier.padding(top = 12.dp), lineHeight = 22.sp)
        Spacer(Modifier.height(52.dp))
        GradientButton("Comecar", onStart, Modifier.fillMaxWidth())
        TextButton(onClick = onStart) { Text("Ja tenho uma conta") }
    }
}

@Composable
fun HomeScreen(vm: TaskFlowViewModel, onNew: () -> Unit, onDetail: (String) -> Unit) {
    val tasks by vm.tasks.collectAsState()
    val reminders by vm.reminders.collectAsState()
    val attachments by vm.attachments.collectAsState()
    val links by vm.links.collectAsState()
    val fields by vm.customFields.collectAsState()
    val lists by vm.lists.collectAsState()
    val users by vm.users.collectAsState()
    val preferences by vm.preferences.collectAsState()
    users.firstOrNull()?.let { LaunchedEffect(it.id) { vm.setCurrentUserIfNeeded(it.id) } }
    val currentUser = users.firstOrNull { it.id == preferences.currentUserId } ?: users.firstOrNull() ?: vm.currentUser()
    var searchQuery by remember { mutableStateOf("") }
    var priorityFilter by remember { mutableStateOf("Todas") }
    var responsibleFilter by remember { mutableStateOf("Todos") }
    var materialFilter by remember { mutableStateOf("Todos") }
    val now = LocalDateTime.now()
    val filteredByTab = when (vm.homeFilter) {
        "Concluidas" -> tasks.filter { it.isCompleted }
        "Atrasadas" -> tasks.filter { !it.isCompleted && it.dueDate?.isBefore(now) == true }
        "Proximas" -> tasks.filter { !it.isCompleted && (it.dueDate?.isAfter(now) ?: true) }
        else -> tasks.filter { !it.isCompleted && (it.dueDate?.toLocalDate() == LocalDate.now() || it.dueDate == null) }
    }
    fun matchesSearch(task: Task): Boolean {
        return TaskSearch.matches(task, searchQuery, users, lists, attachments, links, fields)
    }
    val filtered = filteredByTab
        .filter(::matchesSearch)
        .filter { priorityFilter == "Todas" || it.priority.label == priorityFilter }
        .filter { responsibleFilter == "Todos" || users.firstOrNull { user -> user.id == it.assignedTo }?.name == responsibleFilter }
        .filter {
            when (materialFilter) {
                "Anexos" -> attachments.any { attachment -> attachment.taskId == it.id }
                "Links" -> links.any { link -> link.taskId == it.id }
                "Lembretes" -> reminders.any { reminder -> reminder.taskId == it.id && reminder.isActive }
                else -> true
            }
        }
    Box {
        LazyColumn(Modifier.fillMaxSize().statusBarsPadding().padding(horizontal = 24.dp, vertical = 12.dp), contentPadding = PaddingValues(bottom = 120.dp)) {
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text("Bom dia, ${currentUser.name}", fontSize = 30.sp, fontWeight = FontWeight.Bold, color = Text)
                        Text("Sexta, 5 de junho", color = Muted, fontSize = 18.sp)
                    }
                    Row { IconTile(Icons.Default.Notifications); Spacer(Modifier.width(8.dp)); IconTile(Icons.Default.AutoAwesome, Purple.copy(alpha = .12f), Purple) }
                }
                Spacer(Modifier.height(24.dp))
                Segmented(listOf("Hoje", "Proximas", "Atrasadas", "Concluidas"), vm.homeFilter) { vm.updateHomeFilter(it) }
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(searchQuery, { searchQuery = it }, label = { Text("Buscar tarefas") }, leadingIcon = { Icon(Icons.Default.Search, null) }, singleLine = true, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp))
                Spacer(Modifier.height(12.dp))
                Segmented(listOf("Todas", "Alta", "Media", "Baixa"), priorityFilter) { priorityFilter = it }
                Spacer(Modifier.height(8.dp))
                Segmented(listOf("Todos") + users.map { it.name }, responsibleFilter) { responsibleFilter = it }
                Spacer(Modifier.height(8.dp))
                Segmented(listOf("Todos", "Anexos", "Links", "Lembretes"), materialFilter) { materialFilter = it }
                Spacer(Modifier.height(20.dp))
                TaskFlowCard {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                        Row(verticalAlignment = Alignment.CenterVertically) { IconBubble(Icons.Default.NotificationsActive); Spacer(Modifier.width(14.dp)); Text("Lembretes ativos", fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = Text) }
                        Switch(checked = vm.remindersVisible, onCheckedChange = { vm.updateRemindersVisible(it) })
                    }
                }
                Spacer(Modifier.height(12.dp))
            }
            items(filtered) { task ->
                TaskCard(task, lists.firstOrNull { it.id == task.listId }?.name ?: "Lista", reminders.any { it.taskId == task.id && it.isActive }) { onDetail(task.id) }
                Spacer(Modifier.height(14.dp))
            }
            if (filtered.isEmpty()) {
                item {
                    TaskFlowCard {
                        Text("Nenhuma tarefa encontrada.", fontWeight = FontWeight.Bold, color = Text)
                        Text("Ajuste a busca ou os filtros.", color = Muted, modifier = Modifier.padding(top = 4.dp))
                    }
                    Spacer(Modifier.height(14.dp))
                }
            }
            item {
                val next = reminders.mapNotNull { r -> ReminderEngine.nextOccurrence(r)?.let { r to it } }.minByOrNull { it.second }
                if (next != null) NextReminderCard(tasks.firstOrNull { it.id == next.first.taskId }?.title ?: "Lembrete", next.second)
            }
        }
        FloatingActionButton(onClick = onNew, containerColor = Purple, contentColor = Color.White, modifier = Modifier.align(Alignment.BottomEnd).padding(end = 28.dp, bottom = 96.dp).size(70.dp).testTag("new-task").semantics { contentDescription = "Nova tarefa" }) {
            Icon(Icons.Default.Add, null, Modifier.size(34.dp))
        }
    }
}

@Composable
fun NewTaskScreen(vm: TaskFlowViewModel, onCancel: () -> Unit, onReminder: () -> Unit, onMaterials: () -> Unit) {
    val lists by vm.lists.collectAsState()
    val users by vm.users.collectAsState()
    val user = vm.currentUser()
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var priority by remember { mutableStateOf(TaskPriority.Medium) }
    var selectedListId by remember { mutableStateOf<String?>(null) }
    var selectedAssigneeId by remember(user.id) { mutableStateOf(user.id) }
    var dueDay by remember { mutableStateOf("Hoje") }
    var dueHour by remember { mutableStateOf("09:00") }
    var attemptedSave by remember { mutableStateOf(false) }
    val selectedList = lists.firstOrNull { it.id == selectedListId } ?: lists.firstOrNull()
    val assigneeOptions = users.ifEmpty { listOf(user) }
    val selectedAssignee = assigneeOptions.firstOrNull { it.id == selectedAssigneeId } ?: user
    LaunchedEffect(lists) {
        if (selectedListId == null && lists.isNotEmpty()) selectedListId = lists.first().id
    }
    LazyColumn(Modifier.fillMaxSize().statusBarsPadding().padding(22.dp), contentPadding = PaddingValues(bottom = 36.dp)) {
        item {
            TopRow("Cancelar", "Nova tarefa", onCancel)
            Spacer(Modifier.height(18.dp))
            OutlinedTextField(title, { title = it }, label = { Text("Titulo da tarefa *") }, modifier = Modifier.fillMaxWidth().testTag("new-task-title").semantics { contentDescription = "Campo titulo da tarefa" }, shape = RoundedCornerShape(18.dp), isError = attemptedSave && title.isBlank(), supportingText = { if (attemptedSave && title.isBlank()) Text("Informe um titulo para salvar.") })
            OutlinedTextField(description, { description = it }, label = { Text("Descricao") }, modifier = Modifier.fillMaxWidth().padding(top = 10.dp).testTag("new-task-description").semantics { contentDescription = "Campo descricao da tarefa" }, shape = RoundedCornerShape(18.dp), minLines = 3)
            Spacer(Modifier.height(16.dp))
            TaskFlowCard {
                Text("Lista", color = Muted)
                if (lists.isEmpty()) {
                    Text("Crie uma lista antes de salvar tarefas.", color = Color(0xFFEF4444), modifier = Modifier.padding(top = 8.dp))
                } else {
                    Segmented(lists.map { it.name }, selectedList?.name ?: lists.first().name) { name ->
                        selectedListId = lists.first { it.name == name }.id
                    }
                }
                Text("Prazo", color = Muted, modifier = Modifier.padding(top = 14.dp))
                Segmented(listOf("Hoje", "Amanha"), dueDay) { dueDay = it }
                Spacer(Modifier.height(8.dp))
                Segmented(listOf("09:00", "11:00", "14:00", "16:30"), dueHour) { dueHour = it }
                Text("Responsavel", color = Muted, modifier = Modifier.padding(top = 14.dp))
                Segmented(assigneeOptions.map { it.name }, selectedAssignee.name) { name ->
                    selectedAssigneeId = assigneeOptions.first { it.name == name }.id
                }
                InfoRow("Convidar pessoas", "WhatsApp, e-mail ou link")
            }
            SectionTitle("Lembretes")
            TaskFlowCard(Modifier.clickable(onClick = onReminder)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column { Text("A cada 2 semanas", fontWeight = FontWeight.Bold, color = Text); Text("seg e qui - termina em 31/12/2026", color = Muted) }
                    Icon(Icons.Default.ChevronRight, null, tint = Muted)
                }
            }
            SectionTitle("Materiais da tarefa")
            TaskFlowCard(Modifier.clickable(onClick = onMaterials)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { ChipText("2 anexos"); ChipText("1 link"); ChipText("3 campos") }
                Spacer(Modifier.height(14.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    SmallAction(Icons.Default.AttachFile, "Arquivo")
                    SmallAction(Icons.Default.PhotoCamera, "Foto")
                    SmallAction(Icons.Default.Link, "Link")
                }
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
                        vm.repo.createTask(Task(spaceId = list.spaceId, listId = list.id, title = title.trim(), description = description, priority = priority, createdBy = user.id, assignedTo = selectedAssignee.id, dueDate = LocalDateTime.of(date, time)))
                        onCancel()
                    }
                }
            }, Modifier.fillMaxWidth().testTag("save-new-task").semantics { contentDescription = "Salvar nova tarefa" }, enabled = lists.isNotEmpty())
        }
    }
}

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
    if (!canViewTask) {
        LazyColumn(Modifier.fillMaxSize().statusBarsPadding().padding(22.dp), contentPadding = PaddingValues(bottom = 30.dp)) {
            item {
                TopRow("<", "Detalhe da tarefa", onBack)
                Spacer(Modifier.height(24.dp))
                TaskFlowCard {
                    Icon(Icons.Default.Lock, null, tint = Muted, modifier = Modifier.size(42.dp))
                    Text("Sem acesso a esta tarefa", fontWeight = FontWeight.Bold, color = Text, modifier = Modifier.padding(top = 12.dp))
                    Text("Solicite um convite ao responsavel para visualizar detalhes, anexos, links e campos.", color = Muted, modifier = Modifier.padding(top = 6.dp))
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
                Text("Detalhe da tarefa", fontWeight = FontWeight.Bold, color = Text)
                Spacer(Modifier.weight(1f))
                if (canEditTask) {
                    TextButton(onClick = { editing = !editing }) { Text(if (editing) "Cancelar" else "Editar") }
                } else {
                    Text("Ver", color = Muted, modifier = Modifier.padding(end = 12.dp))
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
                Text(task.title, fontSize = 30.sp, fontWeight = FontWeight.Bold, color = Text, modifier = Modifier.padding(top = 18.dp))
                Row(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) { StatusPill(task.status); PriorityPill(task.priority) }
            }
            SectionTitle("Proximo lembrete")
            val reminderModifier = if (canEditTask) Modifier.clickable(onClick = onReminder) else Modifier
            TaskFlowCard(reminderModifier.testTag("open-reminder").semantics { contentDescription = "Configurar lembrete" }) {
                val next = reminders.firstOrNull { it.taskId == task.id }?.let { ReminderEngine.nextOccurrence(it) }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column { Text("Recorrencia personalizada", fontWeight = FontWeight.Bold, color = Text); Text(next?.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) ?: "Nenhum lembrete", color = Muted) }
                    Switch(checked = reminders.any { it.taskId == task.id && it.isActive }, onCheckedChange = {}, enabled = canEditTask)
                }
            }
            SectionTitle("Descricao")
            TaskFlowCard { Text((if (editing) editDescription else task.description).ifBlank { "Sem descricao." }, color = Muted, lineHeight = 22.sp) }
            SectionTitle("Dados principais")
            TaskFlowCard {
                InfoRow("Prazo", task.dueDate?.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) ?: "Sem prazo")
                InfoRow("Responsavel", assigneeName)
                InfoRow("Participantes", "2 pessoas")
            }
            SectionTitle("Materiais da tarefa")
            val materialsModifier = if (canEditTask) Modifier.clickable(onClick = onMaterials) else Modifier
            TaskFlowCard(materialsModifier) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ChipText("${attachments.count { it.taskId == task.id }} anexos")
                    ChipText("${links.count { it.taskId == task.id }} link")
                    ChipText("${fields.count { it.taskId == task.id }} campos")
                }
                Spacer(Modifier.height(12.dp))
                Text(attachments.firstOrNull { it.taskId == task.id }?.fileName ?: "Nenhum anexo", color = Text)
                Text(links.firstOrNull { it.taskId == task.id }?.title ?: "Nenhum link", color = Text)
            }
            SectionTitle("Comentarios")
            TaskFlowCard {
                val taskComments = comments.filter { it.taskId == task.id }
                if (taskComments.isEmpty()) {
                    Text("Nenhum comentario.", color = Muted)
                } else {
                    taskComments.forEach {
                        val isOwnComment = it.authorId == currentUser.id
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text(users.firstOrNull { user -> user.id == it.authorId }?.name ?: "Usuario", fontWeight = FontWeight.Bold, color = Text, modifier = Modifier.weight(1f))
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
                            Text(it.text, color = Muted, modifier = Modifier.padding(bottom = 4.dp))
                        }
                        Text(formatTimestamp(it.updatedAt), color = Muted, fontSize = 12.sp, modifier = Modifier.padding(bottom = 12.dp))
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
                    Text("Sem permissao para comentar.", color = Muted, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp))
                }
            }
            SectionTitle("Historico")
            TaskFlowCard {
                val taskActivity = activity.filter { it.taskId == task.id }.take(6)
                if (taskActivity.isEmpty()) {
                    Text("Nenhum evento.", color = Muted)
                } else {
                    taskActivity.forEach {
                        Text(it.action, color = Text, fontWeight = FontWeight.SemiBold)
                        Text(formatTimestamp(it.createdAt), color = Muted, fontSize = 12.sp, modifier = Modifier.padding(bottom = 10.dp))
                    }
                }
            }
            Spacer(Modifier.height(20.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = onShare, modifier = Modifier.weight(1f), shape = RoundedCornerShape(50)) { Text("Compartilhar") }
                GradientButton("Concluir", { vm.repo.completeTask(task.id); onBack() }, Modifier.weight(1f), enabled = canEditTask)
            }
        }
    }
}

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
            TaskFlowCard { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) { Text("Notificar esta tarefa", fontWeight = FontWeight.Bold); Switch(enabled, { enabled = it }) } }
            SectionTitle("Quando")
            TaskFlowCard { InfoRow("Data inicial", "10/06/2026"); InfoRow("Horario", "09:00") }
            SectionTitle("Avisar antes")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { listOf("5 min", "15 min", "30 min", "1 h").forEach { ChipText(it) } }
            SectionTitle("Repeticao")
            Segmented(listOf("Nao repetir", "Simples", "Personalizada"), repeatMode) { repeatMode = it }
            TaskFlowCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Repetir a cada", color = Muted)
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
                    Text("Dias selecionados: ${selectedWeekDays.sortedBy { it.value }.joinToString { it.short }}", color = Muted, modifier = Modifier.padding(top = 8.dp))
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
                    Icon(Icons.Default.Lock, null, tint = Muted, modifier = Modifier.size(42.dp))
                    Text("Sem acesso aos materiais", fontWeight = FontWeight.Bold, color = Text, modifier = Modifier.padding(top = 12.dp))
                    Text("Anexos, links e campos ficam disponiveis apenas para participantes autorizados.", color = Muted, modifier = Modifier.padding(top = 6.dp))
                }
            }
        }
        return
    }
    fun addUriAttachment(uri: Uri, source: AttachmentSource) {
        val metadata = context.attachmentMetadata(uri)
        if (!isAllowedAttachment(metadata.name, metadata.sizeBytes)) {
            message = "Arquivo invalido ou maior que 20 MB."
            return
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
        message = "Anexo adicionado."
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
                        if (canManageMaterial) filePicker.launch(arrayOf("application/pdf", "application/msword", "application/vnd.openxmlformats-officedocument.wordprocessingml.document", "application/vnd.ms-excel", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "text/plain")) else message = "Sem permissao para alterar materiais."
                    }
                    SmallAction(Icons.Default.PhotoCamera, "Foto", Modifier.weight(1f)) {
                        if (!canManageMaterial) {
                            message = "Sem permissao para alterar materiais."
                        } else if (context.checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                            openCamera()
                        } else {
                            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    SmallAction(Icons.Default.Image, "Imagem", Modifier.weight(1f)) {
                        if (canManageMaterial) photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) else message = "Sem permissao para alterar materiais."
                    }
                    SmallAction(Icons.Default.Link, "Link", Modifier.weight(1f)) {
                        if (canManageMaterial) linkDialog = true else message = "Sem permissao para alterar materiais."
                    }
                }
            }
            message?.let { Text(it, color = if (it.contains("invalida", true) || it.contains("invalido", true)) Color(0xFFEF4444) else Purple, modifier = Modifier.padding(top = 10.dp)) }
            Spacer(Modifier.height(16.dp))
            TaskFlowCard(Modifier.border(1.dp, Purple.copy(.35f), RoundedCornerShape(22.dp)).clickable {
                if (canManageMaterial) filePicker.launch(arrayOf("*/*")) else message = "Sem permissao para alterar materiais."
            }) {
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.CloudUpload, null, tint = Purple, modifier = Modifier.size(42.dp))
                    Text("Toque para selecionar", fontWeight = FontWeight.Bold, color = Text)
                    Text("PDF, DOC, XLS, JPG, PNG ate 20 MB", color = Muted, fontSize = 13.sp)
                }
            }
            SectionTitle(vm.materialsTab)
            when (vm.materialsTab) {
                "Anexos" -> attachments.filter { it.taskId == task.id }.forEach {
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
                "Links" -> links.filter { it.taskId == task.id }.forEach {
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
                else -> fields.filter { it.taskId == task.id }.forEach {
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
            SectionTitle("Checklist")
            val taskChecklist = checklist.filter { it.taskId == task.id }
            TaskFlowCard {
                if (taskChecklist.isEmpty()) {
                    Text("Nenhum item.", color = Muted)
                } else {
                    taskChecklist.forEach { item ->
                        Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = item.isDone,
                                onCheckedChange = { if (canManageMaterial) vm.repo.toggleChecklistItem(item.id) else message = "Sem permissao para alterar materiais." },
                                enabled = canManageMaterial
                            )
                            Text(item.title, color = Text, modifier = Modifier.weight(1f))
                            if (canManageMaterial) {
                                IconButton({ editingChecklist = item }) { Icon(Icons.Default.Edit, null, tint = Muted) }
                                IconButton({ vm.repo.deleteChecklistItem(item.id); message = "Item removido." }) { Icon(Icons.Default.DeleteOutline, null, tint = Muted) }
                            }
                        }
                    }
                    val done = taskChecklist.count { it.isDone }
                    Text("$done/${taskChecklist.size} concluidos", color = Muted)
                }
            }
            Spacer(Modifier.height(18.dp))
            if (canManageMaterial && vm.materialsTab == "Campos") TextButton({ fieldDialog = true }, Modifier.fillMaxWidth()) { Text("Adicionar campo personalizado") }
            if (canManageMaterial) TextButton({ checklistDialog = true }, Modifier.fillMaxWidth()) { Text("Adicionar item do checklist") }
        }
    }
}

@Composable
fun ShareScreen(vm: TaskFlowViewModel, onBack: () -> Unit) {
    val context = LocalContext.current
    val task = vm.selectedTask()
    val attachments by vm.attachments.collectAsState()
    val links by vm.links.collectAsState()
    val invites by vm.invites.collectAsState()
    var permission by remember { mutableStateOf("Editar") }
    var message by remember { mutableStateOf<String?>(null) }
    if (task == null) {
        LoadingFullScreen("Carregando convite...")
        return
    }
    fun selectedPermission() = when (permission) {
        "Editar" -> UserPermission.Owner
        "Ver" -> UserPermission.Viewer
        else -> UserPermission.Participant
    }
    fun buildInviteText(invite: Invite): String {
        val due = task.dueDate?.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) ?: "sem prazo"
        return "Voce foi convidado para participar da tarefa \"${task.title}\" no TaskFlow.\nPrazo: $due\nStatus: ${task.status.label}\nPermissao: ${invite.permission.label}\nLink: taskflow://invite/${invite.token}"
    }
    fun createInviteAndText(): Pair<Invite, String> {
        val invite = Invite(taskId = task.id, createdBy = vm.currentUser().id, permission = selectedPermission())
        vm.repo.createInvite(invite)
        return invite to buildInviteText(invite)
    }
    fun sendShare(packageName: String? = null) {
        val (_, text) = createInviteAndText()
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "Convite TaskFlow: ${task.title}")
            putExtra(Intent.EXTRA_TEXT, text)
            packageName?.let(::setPackage)
        }
        try {
            context.startActivity(Intent.createChooser(send, "Compartilhar tarefa"))
            message = "Convite gerado."
        } catch (_: ActivityNotFoundException) {
            context.startActivity(Intent.createChooser(send.apply { setPackage(null) }, "Compartilhar tarefa"))
            message = "App especifico indisponivel; usando seletor."
        }
    }
    fun sendEmail() {
        val (_, text) = createInviteAndText()
        val email = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:")
            putExtra(Intent.EXTRA_SUBJECT, "Convite TaskFlow: ${task.title}")
            putExtra(Intent.EXTRA_TEXT, text)
        }
        try {
            context.startActivity(email)
            message = "Convite por e-mail gerado."
        } catch (_: ActivityNotFoundException) {
            sendShare()
        }
    }
    fun copyInvite() {
        val (_, text) = createInviteAndText()
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("Convite TaskFlow", text))
        message = "Link copiado."
    }
    LazyColumn(Modifier.fillMaxSize().statusBarsPadding().padding(22.dp), contentPadding = PaddingValues(bottom = 30.dp)) {
        item {
            TopRow("<", "Compartilhar", onBack)
            Text("Convide alguem para esta tarefa", color = Muted, modifier = Modifier.padding(top = 8.dp))
            SectionTitle("Permissao")
            Segmented(listOf("Editar", "Comentar", "Ver"), permission) { permission = it }
            SectionTitle("Compartilhar por")
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SmallAction(Icons.Default.Chat, "WhatsApp") { sendShare("com.whatsapp") }
                SmallAction(Icons.Default.Email, "E-mail") { sendEmail() }
                SmallAction(Icons.Default.ContentCopy, "Copiar") { copyInvite() }
            }
            message?.let { Text(it, color = Purple, modifier = Modifier.padding(top = 10.dp)) }
            SectionTitle("Previa da mensagem")
            TaskFlowCard {
                Text("Voce foi convidado para participar desta tarefa:", color = Muted)
                Text(task.title, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Text, modifier = Modifier.padding(top = 8.dp))
                Text("Prazo: ${task.dueDate?.format(DateTimeFormatter.ofPattern("dd/MM HH:mm")) ?: "sem prazo"}", color = Muted)
                Text("Status: ${task.status.label}", color = Muted)
                Row(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ChipText("${attachments.count { it.taskId == task.id }} anexos")
                    ChipText("${links.count { it.taskId == task.id }} links")
                }
                Text("taskflow://invite/${task.shareToken}", color = Purple, modifier = Modifier.padding(top = 12.dp))
            }
            val taskInvites = invites.filter { it.taskId == task.id }
            if (taskInvites.isNotEmpty()) {
                SectionTitle("Convites locais")
                taskInvites.take(3).forEach {
                    TaskFlowCard(Modifier.padding(bottom = 10.dp)) {
                        InfoRow("Permissao", it.permission.label)
                        InfoRow("Token", it.token.take(8))
                        InfoRow("Status", if (it.acceptedBy == null) "Pendente" else "Aceito")
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
            GradientButton("Enviar convite", {
                createInviteAndText()
                message = "Convite salvo."
            }, Modifier.fillMaxWidth())
        }
    }
}

@Composable
fun AcceptInviteScreen(vm: TaskFlowViewModel, token: String?, onDone: () -> Unit, onCancel: () -> Unit) {
    val invites by vm.invites.collectAsState()
    val tasks by vm.tasks.collectAsState()
    val users by vm.users.collectAsState()
    val preferences by vm.preferences.collectAsState()
    val invite = invites.firstOrNull { it.token == token }
    val task = invite?.let { value -> tasks.firstOrNull { it.id == value.taskId } }
    val currentUser = users.firstOrNull { it.id == preferences.currentUserId } ?: users.firstOrNull() ?: vm.currentUser()
    val expired = invite?.expiresAt?.let { it < now() } == true
    LazyColumn(Modifier.fillMaxSize().statusBarsPadding().padding(24.dp), contentPadding = PaddingValues(bottom = 40.dp)) {
        item {
            Text("Convite TaskFlow", fontSize = 30.sp, fontWeight = FontWeight.Bold, color = Text)
            Text("Revise os detalhes antes de entrar na tarefa.", color = Muted, modifier = Modifier.padding(top = 8.dp))
            Spacer(Modifier.height(22.dp))
            when {
                token.isNullOrBlank() -> {
                    TaskFlowCard { Text("Link de convite sem token.", color = Color(0xFFEF4444), fontWeight = FontWeight.Bold) }
                    Spacer(Modifier.height(20.dp))
                    GradientButton("Voltar", onCancel, Modifier.fillMaxWidth())
                }
                invite == null || task == null -> {
                    TaskFlowCard {
                        Text("Convite invalido.", color = Color(0xFFEF4444), fontWeight = FontWeight.Bold)
                        Text("O token nao foi encontrado neste dispositivo.", color = Muted, modifier = Modifier.padding(top = 6.dp))
                    }
                    Spacer(Modifier.height(20.dp))
                    GradientButton("Voltar", onCancel, Modifier.fillMaxWidth())
                }
                expired -> {
                    TaskFlowCard {
                        Text("Convite expirado.", color = Color(0xFFEF4444), fontWeight = FontWeight.Bold)
                        Text(task.title, color = Text, modifier = Modifier.padding(top = 8.dp))
                    }
                    Spacer(Modifier.height(20.dp))
                    GradientButton("Voltar", onCancel, Modifier.fillMaxWidth())
                }
                invite.acceptedBy != null -> {
                    TaskFlowCard {
                        Text("Convite ja aceito.", color = Purple, fontWeight = FontWeight.Bold)
                        InfoRow("Tarefa", task.title)
                        InfoRow("Permissao", invite.permission.label)
                    }
                    Spacer(Modifier.height(20.dp))
                    GradientButton("Abrir TaskFlow", onDone, Modifier.fillMaxWidth())
                }
                else -> {
                    TaskFlowCard {
                        Text(task.title, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Text)
                        Text(task.description.ifBlank { "Sem descricao." }, color = Muted, modifier = Modifier.padding(top = 8.dp))
                        InfoRow("Prazo", task.dueDate?.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) ?: "Sem prazo")
                        InfoRow("Permissao", invite.permission.label)
                        InfoRow("Token", invite.token.take(8))
                    }
                    Spacer(Modifier.height(16.dp))
                    GradientButton("Aceitar convite", {
                        vm.repo.acceptInvite(invite.token, currentUser.id)
                        vm.selectedTaskId = task.id
                        onDone()
                    }, Modifier.fillMaxWidth())
                    TextButton({
                        vm.repo.declineInvite(invite.token)
                        onCancel()
                    }, Modifier.fillMaxWidth()) { Text("Recusar", color = Color(0xFFEF4444)) }
                }
            }
        }
    }
}

@Composable
fun SpacesScreen(vm: TaskFlowViewModel, onDetail: (String) -> Unit) {
    val spaces by vm.spaces.collectAsState()
    val lists by vm.lists.collectAsState()
    val tasks by vm.tasks.collectAsState()
    var dialog by remember { mutableStateOf<CrudDialogState?>(null) }
    var message by remember { mutableStateOf<String?>(null) }
    dialog?.let { state ->
        NameDialog(
            title = state.title,
            initialValue = state.initialValue,
            onDismiss = { dialog = null },
            onConfirm = { value ->
                when (state.kind) {
                    CrudKind.CreateSpace -> vm.repo.createSpace(value)
                    CrudKind.EditSpace -> state.space?.let { vm.repo.updateSpace(it.copy(name = value)) }
                    CrudKind.CreateList -> state.space?.let { vm.repo.createList(it.id, value) }
                    CrudKind.EditList -> state.list?.let { vm.repo.updateList(it.copy(name = value)) }
                }
                dialog = null
            }
        )
    }
    LazyColumn(Modifier.fillMaxSize().statusBarsPadding().padding(24.dp), contentPadding = PaddingValues(bottom = 120.dp)) {
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Espacos e listas", fontSize = 30.sp, fontWeight = FontWeight.Bold, color = Text)
                FloatingActionButton(
                    onClick = { dialog = CrudDialogState(CrudKind.CreateSpace, "Novo espaco") },
                    containerColor = Purple,
                    contentColor = Color.White,
                    modifier = Modifier.size(48.dp).testTag("create-space").semantics { contentDescription = "Criar espaco" }
                ) { Icon(Icons.Default.Add, null) }
            }
            message?.let { Text(it, color = Color(0xFFEF4444), modifier = Modifier.padding(top = 8.dp)) }
            Spacer(Modifier.height(16.dp))
        }
        items(spaces) { space ->
            TaskFlowCard {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(space.name, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Text)
                    Row {
                        IconButton(onClick = { dialog = CrudDialogState(CrudKind.EditSpace, "Renomear espaco", space.name, space = space) }, modifier = Modifier.testTag("edit-space-${space.name}").semantics { contentDescription = "Renomear espaco ${space.name}" }) { Icon(Icons.Default.Edit, null, tint = Muted) }
                        IconButton(onClick = { dialog = CrudDialogState(CrudKind.CreateList, "Nova lista", space = space) }, modifier = Modifier.testTag("create-list-${space.name}").semantics { contentDescription = "Criar lista em ${space.name}" }) { Icon(Icons.Default.PlaylistAdd, null, tint = Blue) }
                        IconButton(onClick = {
                            if (tasks.any { it.spaceId == space.id }) message = "Exclua ou mova as tarefas antes de remover este espaco." else vm.repo.deleteSpace(space.id)
                        }, modifier = Modifier.testTag("delete-space-${space.name}").semantics { contentDescription = "Excluir espaco ${space.name}" }) { Icon(Icons.Default.Delete, null, tint = Color(0xFFEF4444)) }
                    }
                }
                lists.filter { it.spaceId == space.id }.forEach { list ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f).clickable { tasks.firstOrNull { it.listId == list.id }?.id?.let(onDetail) }.testTag("open-list-${list.name}").semantics { contentDescription = "Abrir lista ${list.name}" }) {
                            Text(list.name, color = Text)
                            Text("${tasks.count { it.listId == list.id && !it.isCompleted }} abertas", color = Muted, fontSize = 13.sp)
                        }
                        IconButton(onClick = { dialog = CrudDialogState(CrudKind.EditList, "Renomear lista", list.name, list = list) }, modifier = Modifier.testTag("edit-list-${list.name}").semantics { contentDescription = "Renomear lista ${list.name}" }) { Icon(Icons.Default.Edit, null, tint = Muted) }
                        IconButton(onClick = {
                            if (tasks.any { it.listId == list.id }) message = "Exclua ou mova as tarefas antes de remover esta lista." else vm.repo.deleteList(list.id)
                        }, modifier = Modifier.testTag("delete-list-${list.name}").semantics { contentDescription = "Excluir lista ${list.name}" }) { Icon(Icons.Default.Delete, null, tint = Color(0xFFEF4444)) }
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}

enum class CrudKind { CreateSpace, EditSpace, CreateList, EditList }

data class CrudDialogState(
    val kind: CrudKind,
    val title: String,
    val initialValue: String = "",
    val space: Space? = null,
    val list: TaskList? = null
)

@Composable
fun NameDialog(title: String, initialValue: String, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var value by remember(title, initialValue) { mutableStateOf(initialValue) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, color = Text, fontWeight = FontWeight.Bold) },
        text = {
            OutlinedTextField(value, { value = it }, label = { Text("Nome") }, shape = RoundedCornerShape(18.dp), singleLine = true, modifier = Modifier.testTag("name-dialog-field").semantics { contentDescription = "Campo nome" })
        },
        confirmButton = {
            TextButton(onClick = { if (value.isNotBlank()) onConfirm(value.trim()) }, enabled = value.isNotBlank(), modifier = Modifier.testTag("name-dialog-save").semantics { contentDescription = "Salvar nome" }) { Text("Salvar") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.testTag("name-dialog-cancel").semantics { contentDescription = "Cancelar nome" }) { Text("Cancelar") }
        }
    )
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
        title = { Text("Novo link", color = Text, fontWeight = FontWeight.Bold) },
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
        title = { Text("Campo complementar", color = Text, fontWeight = FontWeight.Bold) },
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
fun PeopleScreen(vm: TaskFlowViewModel) {
    val users by vm.users.collectAsState()
    val invites by vm.invites.collectAsState()
    val preferences by vm.preferences.collectAsState()
    LazyColumn(Modifier.fillMaxSize().statusBarsPadding().padding(24.dp), contentPadding = PaddingValues(bottom = 120.dp)) {
        item { Text("Pessoas", fontSize = 30.sp, fontWeight = FontWeight.Bold, color = Text); Text("Convites e participantes", color = Muted); Spacer(Modifier.height(16.dp)) }
        item {
            SectionTitle("Usuarios locais")
            users.forEach { user ->
                TaskFlowCard {
                    InfoRow(user.name, user.email)
                    Button(
                        onClick = { vm.setCurrentUser(user.id) },
                        enabled = preferences.currentUserId != user.id,
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        shape = RoundedCornerShape(50)
                    ) {
                        Text(if (preferences.currentUserId == user.id) "Usuario atual" else "Tornar atual")
                    }
                }
                Spacer(Modifier.height(12.dp))
            }
            SectionTitle("Convites")
        }
        items(invites.ifEmpty { listOf(Invite(taskId = "demo", createdBy = "demo", permission = UserPermission.Participant)) }) {
            TaskFlowCard { InfoRow("Permissao", it.permission.label); InfoRow("Token", it.token.take(8)); InfoRow("Status", if (it.acceptedBy == null) "Pendente" else "Aceito") }
            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
fun SettingsScreen(vm: TaskFlowViewModel, onLogout: () -> Unit) {
    val preferences by vm.preferences.collectAsState()
    LazyColumn(Modifier.fillMaxSize().statusBarsPadding().padding(24.dp), contentPadding = PaddingValues(bottom = 120.dp)) {
        item {
            Text("Ajustes", fontSize = 30.sp, fontWeight = FontWeight.Bold, color = Text)
            Spacer(Modifier.height(16.dp))
            TaskFlowCard {
                InfoRow("Meu perfil", "Manuel")
                Row(Modifier.fillMaxWidth().padding(vertical = 7.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Notificacoes", color = Muted)
                    Switch(checked = preferences.notificationsEnabled, onCheckedChange = vm::setNotificationsEnabled)
                }
                Text("Tema", color = Muted, modifier = Modifier.padding(top = 10.dp))
                Segmented(listOf("Claro", "Escuro futuro"), preferences.theme) { vm.setTheme(it) }
                InfoRow("Filtro inicial", preferences.homeFilter)
                InfoRow("Backups e sincronizacao", "Local-first")
            }
            SectionTitle("Conta")
            TaskFlowCard {
                InfoRow("E-mail", vm.currentUser().email)
                InfoRow("Sessao", if (preferences.currentUserId.isBlank()) "Local" else "Usuario local")
                InfoRow("Sincronizacao Firebase", "Preparada para credenciais")
            }
            SectionTitle("Privacidade")
            TaskFlowCard {
                InfoRow("Dados", "Persistencia local Room/DataStore")
                InfoRow("Compartilhamento", "Tokens locais por convite")
                InfoRow("Galeria", "Photo Picker sem acesso amplo")
            }
            SectionTitle("Ajuda")
            TaskFlowCard {
                InfoRow("Suporte", "Disponivel")
                InfoRow("Versao", "MVP local")
                InfoRow("Diagnostico", "Logcat sem dados sensiveis")
            }
            TextButton(onClick = onLogout, modifier = Modifier.fillMaxWidth()) { Text("Sair da conta", color = Color(0xFFEF4444)) }
        }
    }
}

@Composable
fun LoadingFullScreen(label: String) {
    Box(Modifier.fillMaxSize().background(OffWhite), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = Purple)
            Spacer(Modifier.height(12.dp))
            Text(label, color = Muted)
        }
    }
}

@Composable
fun TaskFlowCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Card(modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(3.dp)) {
        Column(Modifier.padding(18.dp), content = content)
    }
}

@Composable
fun TaskCard(task: Task, listName: String, hasReminder: Boolean, onClick: () -> Unit) {
    TaskFlowCard(Modifier.clickable(onClick = onClick)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.width(5.dp).height(66.dp).clip(RoundedCornerShape(20.dp)).background(priorityColor(task.priority)))
            Spacer(Modifier.width(14.dp))
            Box(Modifier.size(34.dp).border(2.dp, Border, CircleShape).clip(CircleShape))
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(task.title, fontSize = 21.sp, fontWeight = FontWeight.Bold, color = Text)
                Text("${task.dueDate?.format(DateTimeFormatter.ofPattern("HH:mm")) ?: "--:--"}  -  $listName", color = Muted)
            }
            PriorityPill(task.priority)
            if (hasReminder) Icon(Icons.Default.Notifications, null, tint = Muted, modifier = Modifier.padding(start = 8.dp))
        }
    }
}

@Composable
fun NextReminderCard(title: String, date: LocalDateTime) {
    TaskFlowCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconBubble(Icons.Default.Event, Purple.copy(.14f), Purple)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) { Text("Proximo lembrete", color = Purple, fontWeight = FontWeight.Bold); Text(title, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Text); Text(date.format(DateTimeFormatter.ofPattern("'amanha' - HH:mm")), color = Muted) }
            Icon(Icons.Default.ChevronRight, null, tint = Text)
        }
    }
}

@Composable
fun Segmented(options: List<String>, selected: String, onSelect: (String) -> Unit) {
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(Color.White).border(1.dp, Border, RoundedCornerShape(22.dp)).padding(4.dp)) {
        options.forEach { option ->
            val active = option == selected
            Box(Modifier.weight(1f).clip(RoundedCornerShape(18.dp)).background(if (active) Gradient else Brush.linearGradient(listOf(Color.Transparent, Color.Transparent))).clickable { onSelect(option) }.padding(vertical = 12.dp), contentAlignment = Alignment.Center) {
                Text(option, color = if (active) Color.White else Text, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
fun GradientButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    Button(onClick = onClick, modifier = modifier.height(54.dp), enabled = enabled, shape = RoundedCornerShape(50), colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent), contentPadding = PaddingValues()) {
        Box(Modifier.fillMaxSize().background(if (enabled) Gradient else Brush.linearGradient(listOf(Border, Border))), contentAlignment = Alignment.Center) { Text(text, color = Color.White, fontWeight = FontWeight.Bold) }
    }
}

@Composable
fun IconTile(icon: ImageVector, bg: Color = Color.White, tint: Color = Text) = Box(Modifier.size(48.dp).clip(RoundedCornerShape(16.dp)).background(bg), contentAlignment = Alignment.Center) { Icon(icon, null, tint = tint) }
@Composable
fun IconBubble(icon: ImageVector, bg: Color = Blue.copy(.12f), tint: Color = Blue) = Box(Modifier.size(42.dp).clip(CircleShape).background(bg), contentAlignment = Alignment.Center) { Icon(icon, null, tint = tint) }
@Composable
fun SectionTitle(text: String) = Text(text, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Text, modifier = Modifier.padding(top = 22.dp, bottom = 10.dp))
@Composable
fun ChipText(text: String, active: Boolean = true, modifier: Modifier = Modifier) = Text(text, color = if (active) Purple else Muted, fontWeight = FontWeight.SemiBold, modifier = modifier.clip(RoundedCornerShape(50)).background(if (active) Purple.copy(.10f) else Border.copy(.45f)).padding(horizontal = 12.dp, vertical = 8.dp))
@Composable
fun PriorityPill(priority: TaskPriority) = Text(priority.label, color = priorityColor(priority), modifier = Modifier.clip(RoundedCornerShape(50)).background(priorityColor(priority).copy(.12f)).padding(horizontal = 12.dp, vertical = 8.dp))
@Composable
fun StatusPill(status: TaskStatus) = Text(status.label, color = Blue, modifier = Modifier.clip(RoundedCornerShape(50)).background(Blue.copy(.10f)).padding(horizontal = 12.dp, vertical = 8.dp))
@Composable
fun SmallAction(icon: ImageVector, label: String, modifier: Modifier = Modifier, onClick: () -> Unit = {}) = OutlinedButton(onClick = onClick, modifier = modifier, shape = RoundedCornerShape(18.dp), contentPadding = PaddingValues(10.dp)) { Icon(icon, null); Spacer(Modifier.width(6.dp)); Text(label) }
@Composable
fun TopRow(action: String, title: String, onAction: () -> Unit) = Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) { TextButton(onClick = onAction) { Text(action) }; Spacer(Modifier.weight(1f)); Text(title, fontWeight = FontWeight.Bold, color = Text); Spacer(Modifier.weight(1f)); Spacer(Modifier.width(76.dp)) }
@Composable
fun InfoRow(label: String, value: String) = Row(Modifier.fillMaxWidth().padding(vertical = 7.dp), horizontalArrangement = Arrangement.SpaceBetween) { Text(label, color = Muted); Text(value, color = Text, fontWeight = FontWeight.SemiBold) }
@Composable
fun MaterialRow(icon: ImageVector, title: String, subtitle: String) = TaskFlowCard(Modifier.padding(bottom = 10.dp)) { Row(verticalAlignment = Alignment.CenterVertically) { IconBubble(icon, Purple.copy(.10f), Purple); Spacer(Modifier.width(12.dp)); Column(Modifier.weight(1f)) { Text(title, fontWeight = FontWeight.Bold, color = Text); Text(subtitle, color = Muted, maxLines = 1) }; Icon(Icons.Default.MoreVert, null, tint = Muted) } }
@Composable
fun AttachmentRow(attachment: Attachment, canManage: Boolean = true, onOpen: () -> Unit, onShare: () -> Unit, onDelete: () -> Unit) = TaskFlowCard(Modifier.padding(bottom = 10.dp)) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        AttachmentPreview(attachment)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(attachment.fileName, fontWeight = FontWeight.Bold, color = Text)
            Text("${attachment.fileType.name} - ${attachment.fileSize / 1024} KB", color = Muted, maxLines = 1)
        }
        ItemActionMenu(
            contentDescription = "Menu do anexo ${attachment.fileName}",
            actions = listOfNotNull(
                ItemAction("Abrir", Icons.Default.OpenInNew, onOpen),
                ItemAction("Compartilhar", Icons.Default.IosShare, onShare),
                if (canManage) ItemAction("Excluir", Icons.Default.DeleteOutline, onDelete) else null
            )
        )
    }
    Row(Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        SmallAction(Icons.Default.OpenInNew, "Abrir", Modifier.weight(1f), onOpen)
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
        IconBubble(if (attachment.fileType == AttachmentType.Image) Icons.Default.Image else Icons.Default.Description, Purple.copy(.10f), Purple)
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
        IconBubble(Icons.Default.Link, Purple.copy(.10f), Purple)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(link.title, fontWeight = FontWeight.Bold, color = Text)
            Text(link.url, color = Muted, maxLines = 1)
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
        IconBubble(Icons.Default.EditNote, Purple.copy(.10f), Purple)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(field.fieldName, fontWeight = FontWeight.Bold, color = Text)
            Text("${field.fieldType.label()} - ${field.fieldValue}", color = Muted, maxLines = 1)
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
            Icon(Icons.Default.MoreVert, null, tint = Muted)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            actions.forEach { action ->
                DropdownMenuItem(
                    text = { Text(action.label) },
                    leadingIcon = { Icon(action.icon, null, tint = Muted) },
                    onClick = {
                        expanded = false
                        action.onClick()
                    }
                )
            }
        }
    }
}
fun priorityColor(priority: TaskPriority) = when (priority) { TaskPriority.High -> Color(0xFFEF4444); TaskPriority.Medium -> Blue; TaskPriority.Low -> Color(0xFF22C55E) }
fun formatTimestamp(value: Long): String = Instant.ofEpochMilli(value).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))

data class AttachmentMetadata(val name: String, val sizeBytes: Long, val mimeType: String)

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
