package com.taskflow.feature.spaces

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.taskflow.core.design.ChipText
import com.taskflow.core.design.InfoRow
import com.taskflow.core.design.SectionTitle
import com.taskflow.core.design.TaskCard
import com.taskflow.core.design.TaskFlowCard
import com.taskflow.TaskFlowViewModel
import com.taskflow.core.design.NameDialog
import com.taskflow.core.design.TaskFlowColors
import com.taskflow.domain.model.Space
import com.taskflow.domain.model.TaskList

@Composable
fun SpacesScreen(vm: TaskFlowViewModel, onDetail: (String) -> Unit) {
    val spaces by vm.spaces.collectAsState()
    val lists by vm.lists.collectAsState()
    val tasks by vm.tasks.collectAsState()
    val reminders by vm.reminders.collectAsState()
    val users by vm.users.collectAsState()
    val preferences by vm.preferences.collectAsState()
    var dialog by remember { mutableStateOf<CrudDialogState?>(null) }
    var message by remember { mutableStateOf<String?>(null) }
    var selectedSpaceId by remember { mutableStateOf<String?>(null) }
    var selectedListId by remember { mutableStateOf<String?>(null) }
    val currentUser = users.firstOrNull { it.id == preferences.currentUserId } ?: users.firstOrNull() ?: vm.currentUser()
    val selectedSpace = spaces.firstOrNull { it.id == selectedSpaceId }
    val selectedSpaceTasks = selectedSpace?.let { space -> tasks.filter { it.spaceId == space.id } } ?: emptyList()
    val selectedList = lists.firstOrNull { it.id == selectedListId }
    val selectedListTasks = selectedList?.let { list -> tasks.filter { it.listId == list.id } } ?: emptyList()
    fun orderedLists(spaceId: String) = lists.filter { it.spaceId == spaceId }.sortedBy { it.order }
    fun moveList(list: TaskList, delta: Int) {
        val siblings = orderedLists(list.spaceId)
        val index = siblings.indexOfFirst { it.id == list.id }
        val target = siblings.getOrNull(index + delta) ?: return
        vm.repo.updateList(list.copy(order = target.order))
        vm.repo.updateList(target.copy(order = list.order))
    }
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
                Text("Espacos e listas", fontSize = 30.sp, fontWeight = FontWeight.Bold, color = TaskFlowColors.Text)
                FloatingActionButton(
                    onClick = { dialog = CrudDialogState(CrudKind.CreateSpace, "Novo espaco") },
                    containerColor = TaskFlowColors.Purple,
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
                    Column(
                        Modifier.weight(1f).clip(RoundedCornerShape(14.dp)).clickable {
                            selectedSpaceId = space.id
                            selectedListId = null
                        }.padding(horizontal = 8.dp, vertical = 6.dp).testTag("open-space-${space.name}").semantics { contentDescription = "Abrir espaco ${space.name}" }
                    ) {
                        Text(space.name, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = TaskFlowColors.Text)
                        val shared = space.ownerId != currentUser.id || space.members.size > 1
                        Row(Modifier.padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            ChipText(if (shared) "Compartilhado" else "Meu espaco", active = shared)
                        }
                        val listCount = lists.count { it.spaceId == space.id }
                        val taskCount = tasks.count { it.spaceId == space.id }
                        val selectedSuffix = if (selectedSpaceId == space.id) " - aberto" else ""
                        Text("$listCount listas - $taskCount tarefas - ${space.members.size} membros$selectedSuffix", color = TaskFlowColors.Muted, fontSize = 13.sp)
                    }
                    Row {
                        IconButton(onClick = { dialog = CrudDialogState(CrudKind.EditSpace, "Renomear espaco", space.name, space = space) }, modifier = Modifier.testTag("edit-space-${space.name}").semantics { contentDescription = "Renomear espaco ${space.name}" }) { Icon(Icons.Default.Edit, null, tint = TaskFlowColors.Muted) }
                        IconButton(onClick = { dialog = CrudDialogState(CrudKind.CreateList, "Nova lista", space = space) }, modifier = Modifier.testTag("create-list-${space.name}").semantics { contentDescription = "Criar lista em ${space.name}" }) { Icon(Icons.Default.PlaylistAdd, null, tint = TaskFlowColors.Blue) }
                        IconButton(onClick = {
                            if (tasks.any { it.spaceId == space.id }) message = "Exclua ou mova as tarefas antes de remover este espaco." else vm.repo.deleteSpace(space.id)
                        }, modifier = Modifier.testTag("delete-space-${space.name}").semantics { contentDescription = "Excluir espaco ${space.name}" }) { Icon(Icons.Default.Delete, null, tint = Color(0xFFEF4444)) }
                    }
                }
                orderedLists(space.id).forEach { list ->
                    val siblings = orderedLists(space.id)
                    val listIndex = siblings.indexOfFirst { it.id == list.id }
                    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f).clip(RoundedCornerShape(14.dp)).clickable {
                            selectedListId = list.id
                            selectedSpaceId = null
                        }.padding(horizontal = 8.dp, vertical = 6.dp).testTag("open-list-${list.name}").semantics { contentDescription = "Filtrar lista ${list.name}" }) {
                            Text(list.name, color = TaskFlowColors.Text)
                            val openCount = tasks.count { it.listId == list.id && !it.isCompleted }
                            val selectedSuffix = if (selectedListId == list.id) " - filtrando" else ""
                            Text("$openCount abertas$selectedSuffix", color = TaskFlowColors.Muted, fontSize = 13.sp)
                        }
                        IconButton(
                            onClick = { moveList(list, -1) },
                            enabled = listIndex > 0,
                            modifier = Modifier.testTag("move-list-up-${list.name}").semantics { contentDescription = "Mover lista ${list.name} para cima" }
                        ) { Icon(Icons.Default.KeyboardArrowUp, null, tint = if (listIndex > 0) TaskFlowColors.Muted else TaskFlowColors.Border) }
                        IconButton(
                            onClick = { moveList(list, 1) },
                            enabled = listIndex < siblings.lastIndex,
                            modifier = Modifier.testTag("move-list-down-${list.name}").semantics { contentDescription = "Mover lista ${list.name} para baixo" }
                        ) { Icon(Icons.Default.KeyboardArrowDown, null, tint = if (listIndex < siblings.lastIndex) TaskFlowColors.Muted else TaskFlowColors.Border) }
                        IconButton(onClick = { dialog = CrudDialogState(CrudKind.EditList, "Renomear lista", list.name, list = list) }, modifier = Modifier.testTag("edit-list-${list.name}").semantics { contentDescription = "Renomear lista ${list.name}" }) { Icon(Icons.Default.Edit, null, tint = TaskFlowColors.Muted) }
                        IconButton(onClick = {
                            if (tasks.any { it.listId == list.id }) message = "Exclua ou mova as tarefas antes de remover esta lista." else vm.repo.deleteList(list.id)
                        }, modifier = Modifier.testTag("delete-list-${list.name}").semantics { contentDescription = "Excluir lista ${list.name}" }) { Icon(Icons.Default.Delete, null, tint = Color(0xFFEF4444)) }
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
        }
        selectedSpace?.let { space ->
            item {
                SectionTitle("Tarefas em ${space.name}")
                val spaceLists = orderedLists(space.id)
                TaskFlowCard {
                    val ownerName = users.firstOrNull { it.id == space.ownerId }?.name ?: "Usuario local"
                    InfoRow("Proprietario", ownerName)
                    InfoRow("Acesso", if (space.ownerId == currentUser.id) "Meu espaco" else "Compartilhado comigo")
                    InfoRow("Listas", spaceLists.joinToString { it.name }.ifBlank { "Nenhuma lista" })
                    InfoRow("Tarefas abertas", selectedSpaceTasks.count { !it.isCompleted }.toString())
                }
                Spacer(Modifier.height(12.dp))
                if (selectedSpaceTasks.isEmpty()) {
                    TaskFlowCard {
                        Text("Nenhuma tarefa neste espaco.", fontWeight = FontWeight.Bold, color = TaskFlowColors.Text)
                        Text("Crie uma lista e uma tarefa para preencher este espaco.", color = TaskFlowColors.Muted, modifier = Modifier.padding(top = 4.dp))
                    }
                    Spacer(Modifier.height(12.dp))
                }
            }
            items(selectedSpaceTasks) { task ->
                val listName = lists.firstOrNull { it.id == task.listId }?.name ?: "Lista"
                TaskCard(task, listName, reminders.any { it.taskId == task.id && it.isActive }) { onDetail(task.id) }
                Spacer(Modifier.height(12.dp))
            }
        }
        selectedList?.let { list ->
            item {
                SectionTitle("Tarefas em ${list.name}")
                if (selectedListTasks.isEmpty()) {
                    TaskFlowCard {
                        Text("Nenhuma tarefa nesta lista.", fontWeight = FontWeight.Bold, color = TaskFlowColors.Text)
                        Text("Crie uma tarefa usando esta lista para ela aparecer aqui.", color = TaskFlowColors.Muted, modifier = Modifier.padding(top = 4.dp))
                    }
                    Spacer(Modifier.height(12.dp))
                }
            }
            items(selectedListTasks) { task ->
                TaskCard(task, list.name, reminders.any { it.taskId == task.id && it.isActive }) { onDetail(task.id) }
                Spacer(Modifier.height(12.dp))
            }
        }
    }
}

private enum class CrudKind { CreateSpace, EditSpace, CreateList, EditList }

private data class CrudDialogState(
    val kind: CrudKind,
    val title: String,
    val initialValue: String = "",
    val space: Space? = null,
    val list: TaskList? = null
)
