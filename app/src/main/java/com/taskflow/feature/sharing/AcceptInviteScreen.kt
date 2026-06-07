package com.taskflow.feature.sharing

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.taskflow.core.app.TaskFlowViewModel
import com.taskflow.core.design.DesignTokens
import com.taskflow.core.design.GradientButton
import com.taskflow.core.design.InfoRow
import com.taskflow.core.design.ScreenTitle
import com.taskflow.core.design.TaskFlowCard
import com.taskflow.core.design.TaskFlowColors
import com.taskflow.data.remote.RemoteInviteLink
import com.taskflow.domain.model.now
import kotlinx.coroutines.launch
import java.time.format.DateTimeFormatter

@Composable
fun AcceptInviteScreen(vm: TaskFlowViewModel, token: String?, onDone: () -> Unit, onCancel: () -> Unit) {
    val invites by vm.invites.collectAsState()
    val tasks by vm.tasks.collectAsState()
    val users by vm.users.collectAsState()
    val preferences by vm.preferences.collectAsState()
    val invite = invites.firstOrNull { it.token == token }
    val task = invite?.let { value -> tasks.firstOrNull { it.id == value.taskId } }
    val currentUser = users.firstOrNull { it.id == preferences.currentUserId } ?: vm.currentUser()
    val expired = invite?.expiresAt?.let { it < now() } == true
    val scope = rememberCoroutineScope()
    var remoteInvite by remember(token) { mutableStateOf<RemoteInviteLink?>(null) }
    var loadingRemote by remember(token) { mutableStateOf(false) }
    var errorMessage by remember(token) { mutableStateOf<String?>(null) }

    LaunchedEffect(token, invite) {
        if (!token.isNullOrBlank() && invite == null) {
            loadingRemote = true
            errorMessage = null
            val result = vm.resolveRemoteInvite(token)
            loadingRemote = false
            result
                .onSuccess { remoteInvite = it }
                .onFailure { errorMessage = "Nao foi possivel carregar o convite remoto." }
        }
    }

    LazyColumn(Modifier.fillMaxSize().statusBarsPadding().padding(DesignTokens.screenPadding), contentPadding = PaddingValues(bottom = DesignTokens.screenBottomPadding)) {
        item {
            ScreenTitle("Convite TaskFlow")
            Spacer(Modifier.height(22.dp))
            when {
                token.isNullOrBlank() -> {
                    TaskFlowCard { Text("Link de convite sem token.", color = TaskFlowColors.Danger, fontWeight = FontWeight.Bold) }
                    Spacer(Modifier.height(20.dp))
                    GradientButton("Voltar", onCancel, Modifier.fillMaxWidth())
                }
                invite != null && task == null -> {
                    TaskFlowCard {
                        Text("Convite invalido.", color = TaskFlowColors.Danger, fontWeight = FontWeight.Bold)
                        Text("A tarefa deste convite nao esta disponivel neste aparelho.", color = TaskFlowColors.Muted, modifier = Modifier.padding(top = 6.dp))
                    }
                    Spacer(Modifier.height(20.dp))
                    GradientButton("Voltar", onCancel, Modifier.fillMaxWidth())
                }
                preferences.currentUserId.isBlank() -> {
                    vm.pendingInviteToken = token
                    TaskFlowCard {
                        Text("Entre para aceitar", color = TaskFlowColors.Text, fontWeight = FontWeight.Bold)
                        Text("Depois do login ou cadastro, o convite sera aberto automaticamente.", color = TaskFlowColors.Muted, modifier = Modifier.padding(top = 6.dp))
                    }
                    Spacer(Modifier.height(20.dp))
                    GradientButton("Entrar ou criar conta", onCancel, Modifier.fillMaxWidth())
                }
                invite == null && loadingRemote -> {
                    TaskFlowCard { Text("Carregando convite...", color = TaskFlowColors.Muted, fontWeight = FontWeight.Bold) }
                }
                invite == null && remoteInvite == null -> {
                    TaskFlowCard {
                        Text("Convite invalido.", color = TaskFlowColors.Danger, fontWeight = FontWeight.Bold)
                        Text(errorMessage ?: "O link nao foi encontrado ou expirou.", color = TaskFlowColors.Muted, modifier = Modifier.padding(top = 6.dp))
                    }
                    Spacer(Modifier.height(20.dp))
                    GradientButton("Voltar", onCancel, Modifier.fillMaxWidth())
                }
                invite == null && remoteInvite != null -> {
                    val remote = remoteInvite!!
                    TaskFlowCard {
                        Text(remote.task.title, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = TaskFlowColors.Text)
                        Text(remote.task.description.ifBlank { "Sem descricao." }, color = TaskFlowColors.Muted, modifier = Modifier.padding(top = 8.dp))
                        InfoRow("Permissao", remote.permission.label)
                        InfoRow("Criador", remote.createdBy.take(8))
                    }
                    Spacer(Modifier.height(16.dp))
                    GradientButton("Aceitar convite", {
                        scope.launch {
                            vm.acceptRemoteInvite(remote.token)
                                .onSuccess { onDone() }
                                .onFailure { errorMessage = it.message ?: "Nao foi possivel aceitar o convite." }
                        }
                    }, Modifier.fillMaxWidth())
                    errorMessage?.let {
                        Spacer(Modifier.height(10.dp))
                        Text(it, color = TaskFlowColors.Danger)
                    }
                    TextButton(onCancel, Modifier.fillMaxWidth()) { Text("Recusar", color = TaskFlowColors.Danger) }
                }
                expired -> {
                    val localTask = task!!
                    TaskFlowCard {
                        Text("Convite expirado.", color = TaskFlowColors.Danger, fontWeight = FontWeight.Bold)
                        Text(localTask.title, color = TaskFlowColors.Text, modifier = Modifier.padding(top = 8.dp))
                    }
                    Spacer(Modifier.height(20.dp))
                    GradientButton("Voltar", onCancel, Modifier.fillMaxWidth())
                }
                invite!!.acceptedBy != null -> {
                    val localTask = task!!
                    val localInvite = invite
                    TaskFlowCard {
                        Text("Convite ja aceito.", color = TaskFlowColors.Purple, fontWeight = FontWeight.Bold)
                        InfoRow("Tarefa", localTask.title)
                        InfoRow("Permissao", localInvite.permission.label)
                    }
                    Spacer(Modifier.height(20.dp))
                    GradientButton("Abrir TaskFlow", onDone, Modifier.fillMaxWidth())
                }
                else -> {
                    if (task == null) {
                        TaskFlowCard { Text("A tarefa deste convite nao esta disponivel.", color = TaskFlowColors.Danger, fontWeight = FontWeight.Bold) }
                        Spacer(Modifier.height(20.dp))
                        GradientButton("Voltar", onCancel, Modifier.fillMaxWidth())
                        return@item
                    }
                    TaskFlowCard {
                        Text(task.title, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = TaskFlowColors.Text)
                        Text(task.description.ifBlank { "Sem descricao." }, color = TaskFlowColors.Muted, modifier = Modifier.padding(top = 8.dp))
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
                    }, Modifier.fillMaxWidth()) { Text("Recusar", color = TaskFlowColors.Danger) }
                }
            }
        }
    }
}
