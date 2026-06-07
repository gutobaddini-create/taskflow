package com.taskflow.feature.home

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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.taskflow.core.design.IconBubble
import com.taskflow.core.design.IconTile
import com.taskflow.core.design.DesignTokens
import com.taskflow.core.design.NextReminderCard
import com.taskflow.core.design.ScreenTitle
import com.taskflow.core.design.Segmented
import com.taskflow.core.design.TaskCard
import com.taskflow.core.design.TaskFlowCard
import com.taskflow.core.app.TaskFlowViewModel
import com.taskflow.core.design.TaskFlowColors
import com.taskflow.core.notifications.ReminderEngine
import com.taskflow.core.utils.TaskSearch
import com.taskflow.domain.model.Task
import com.taskflow.domain.usecase.TaskQueries
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import com.taskflow.core.design.EmptyState
import com.taskflow.core.design.FloatingAddButton

@Composable
fun HomeScreen(vm: TaskFlowViewModel, onNew: () -> Unit, onDetail: (String) -> Unit) {
    val tasks by vm.tasks.collectAsState()
    val reminders by vm.reminders.collectAsState()
    val invites by vm.invites.collectAsState()
    val attachments by vm.attachments.collectAsState()
    val links by vm.links.collectAsState()
    val fields by vm.customFields.collectAsState()
    val lists by vm.lists.collectAsState()
    val users by vm.users.collectAsState()
    val preferences by vm.preferences.collectAsState()
    val currentUser = users.firstOrNull { it.id == preferences.currentUserId } ?: vm.currentUser()
    var searchQuery by remember { mutableStateOf("") }
    val today = LocalDate.now()
    val homeTabs = listOf("Hoje", "Proximas", "Concluidas")
    val selectedHomeTab = if (vm.homeFilter in homeTabs) vm.homeFilter else "Hoje"
    val visibleTasks = TaskQueries.visibleForUser(tasks, currentUser.id, invites)
    val filteredByTab = when (selectedHomeTab) {
        "Concluidas" -> TaskQueries.completed(visibleTasks)
        "Proximas" -> TaskQueries.upcoming(visibleTasks, today)
        else -> TaskQueries.todayOrUnscheduled(visibleTasks, today)
    }
    fun matchesSearch(task: Task): Boolean {
        return TaskSearch.matches(task, searchQuery, users, lists, attachments, links, fields)
    }
    val filtered = filteredByTab
        .filter(::matchesSearch)
    Box {
        LazyColumn(Modifier.fillMaxSize().statusBarsPadding().padding(horizontal = DesignTokens.screenPadding, vertical = 12.dp), contentPadding = PaddingValues(bottom = DesignTokens.navigationBottomPadding)) {
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        ScreenTitle("Bom dia, ${currentUser.name}")
                        Text(today.format(DateTimeFormatter.ofPattern("EEEE, d 'de' MMMM", Locale("pt", "BR"))).replaceFirstChar { it.uppercase() }, color = TaskFlowColors.Muted, fontSize = 18.sp)
                    }
                    Row {
                        IconTile(Icons.Default.Notifications)
                        Spacer(Modifier.width(8.dp))
                        IconTile(Icons.Default.AutoAwesome, TaskFlowColors.Purple.copy(alpha = .12f), TaskFlowColors.Purple)
                    }
                }
                Spacer(Modifier.height(24.dp))
                Segmented(homeTabs, selectedHomeTab) { vm.updateHomeFilter(it) }
                if (visibleTasks.isNotEmpty()) {
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(searchQuery, { searchQuery = it }, label = { Text("Buscar tarefas") }, leadingIcon = { Icon(Icons.Default.Search, null) }, singleLine = true, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp))
                }
                Spacer(Modifier.height(20.dp))
                TaskFlowCard {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconBubble(Icons.Default.NotificationsActive)
                            Spacer(Modifier.width(14.dp))
                            Text("Lembretes ativos", fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = TaskFlowColors.Text)
                        }
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
                    EmptyState("Nenhuma tarefa encontrada", "Ajuste a busca ou os filtros.")
                    Spacer(Modifier.height(14.dp))
                }
            }
            item {
                val next = reminders.mapNotNull { r -> ReminderEngine.nextOccurrence(r)?.let { r to it } }.minByOrNull { it.second }
                if (next != null) NextReminderCard(tasks.firstOrNull { it.id == next.first.taskId }?.title ?: "Lembrete", next.second)
            }
        }
        FloatingAddButton(
            onClick = onNew,
            modifier = Modifier.align(Alignment.BottomEnd).padding(end = 28.dp, bottom = DesignTokens.floatingActionBottomPadding).testTag("new-task").semantics { contentDescription = "Nova tarefa" }
        )
    }
}
