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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.taskflow.core.design.GradientButton
import com.taskflow.core.design.InfoRow
import com.taskflow.core.design.TaskFlowCard
import com.taskflow.core.app.TaskFlowViewModel
import com.taskflow.core.design.TaskFlowColors
import com.taskflow.domain.model.now
import java.time.format.DateTimeFormatter

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
            Text("Convite TaskFlow", fontSize = 30.sp, fontWeight = FontWeight.Bold, color = TaskFlowColors.Text)
            Text("Revise os detalhes antes de entrar na tarefa.", color = TaskFlowColors.Muted, modifier = Modifier.padding(top = 8.dp))
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
                        Text("O token nao foi encontrado neste dispositivo.", color = TaskFlowColors.Muted, modifier = Modifier.padding(top = 6.dp))
                    }
                    Spacer(Modifier.height(20.dp))
                    GradientButton("Voltar", onCancel, Modifier.fillMaxWidth())
                }
                expired -> {
                    TaskFlowCard {
                        Text("Convite expirado.", color = Color(0xFFEF4444), fontWeight = FontWeight.Bold)
                        Text(task.title, color = TaskFlowColors.Text, modifier = Modifier.padding(top = 8.dp))
                    }
                    Spacer(Modifier.height(20.dp))
                    GradientButton("Voltar", onCancel, Modifier.fillMaxWidth())
                }
                invite.acceptedBy != null -> {
                    TaskFlowCard {
                        Text("Convite ja aceito.", color = TaskFlowColors.Purple, fontWeight = FontWeight.Bold)
                        InfoRow("Tarefa", task.title)
                        InfoRow("Permissao", invite.permission.label)
                    }
                    Spacer(Modifier.height(20.dp))
                    GradientButton("Abrir TaskFlow", onDone, Modifier.fillMaxWidth())
                }
                else -> {
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
                    }, Modifier.fillMaxWidth()) { Text("Recusar", color = Color(0xFFEF4444)) }
                }
            }
        }
    }
}
