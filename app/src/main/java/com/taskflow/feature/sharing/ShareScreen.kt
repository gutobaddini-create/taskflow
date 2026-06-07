package com.taskflow.feature.sharing

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Email
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.taskflow.core.design.ChipText
import com.taskflow.core.design.ChipTone
import com.taskflow.core.design.DesignTokens
import com.taskflow.core.design.FeedbackBanner
import com.taskflow.core.design.FeedbackKind
import com.taskflow.core.design.GradientButton
import com.taskflow.core.design.InfoRow
import com.taskflow.core.design.SectionTitle
import com.taskflow.core.design.Segmented
import com.taskflow.core.design.SmallAction
import com.taskflow.core.app.TaskFlowViewModel
import com.taskflow.core.design.TaskFlowCard
import com.taskflow.core.design.TopRow
import com.taskflow.core.design.LoadingFullScreen
import com.taskflow.core.design.TaskFlowColors
import com.taskflow.core.sharing.InviteLinks
import com.taskflow.domain.model.Invite
import com.taskflow.domain.model.UserPermission
import kotlinx.coroutines.launch
import java.time.format.DateTimeFormatter

@Composable
fun ShareScreen(vm: TaskFlowViewModel, onBack: () -> Unit) {
    val context = LocalContext.current
    val task = vm.selectedTask()
    val attachments by vm.attachments.collectAsState()
    val links by vm.links.collectAsState()
    val invites by vm.invites.collectAsState()
    val scope = rememberCoroutineScope()
    var permission by remember { mutableStateOf("Editar") }
    var message by remember { mutableStateOf<String?>(null) }
    var lastInviteUrl by remember { mutableStateOf<String?>(null) }

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
        val link = InviteLinks.urlForToken(invite.token)
        return "Voce foi convidado para participar da tarefa \"${task.title}\" no TaskFlow.\nPrazo: $due\nStatus: ${task.status.label}\nPermissao: ${invite.permission.label}\nLink: $link"
    }

    suspend fun createInviteAndText(): Pair<Invite, String> {
        val invite = Invite(taskId = task.id, createdBy = vm.currentUser().id, permission = selectedPermission())
        vm.createRemoteInvite(invite, task).getOrThrow()
        lastInviteUrl = InviteLinks.urlForToken(invite.token)
        return invite to buildInviteText(invite)
    }

    fun sendShare(packageName: String? = null) {
        scope.launch {
            runCatching {
                val (_, text) = createInviteAndText()
                val send = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_SUBJECT, "Convite TaskFlow: ${task.title}")
                    putExtra(Intent.EXTRA_TEXT, text)
                    packageName?.let(::setPackage)
                }
                try {
                    context.startActivity(Intent.createChooser(send, "Compartilhar tarefa"))
                    message = "Convite real gerado."
                } catch (_: ActivityNotFoundException) {
                    context.startActivity(Intent.createChooser(send.apply { setPackage(null) }, "Compartilhar tarefa"))
                    message = "App especifico indisponivel; usando seletor."
                }
            }.onFailure {
                message = "Nao foi possivel gerar link real. Verifique login e internet."
            }
        }
    }

    fun sendEmail() {
        scope.launch {
            runCatching {
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
            }.onFailure {
                message = "Nao foi possivel gerar link real. Verifique login e internet."
            }
        }
    }

    fun copyInvite() {
        scope.launch {
            runCatching {
                val (_, text) = createInviteAndText()
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.setPrimaryClip(ClipData.newPlainText("Convite TaskFlow", text))
                message = "Link copiado."
            }.onFailure {
                message = "Nao foi possivel gerar link real. Verifique login e internet."
            }
        }
    }

    LazyColumn(Modifier.fillMaxSize().statusBarsPadding().padding(DesignTokens.screenPadding), contentPadding = PaddingValues(bottom = DesignTokens.screenBottomPadding)) {
        item {
            TopRow("<", "Compartilhar", onBack)
            Text("Convide alguem para esta tarefa", color = TaskFlowColors.Muted, modifier = Modifier.padding(top = 8.dp))
            SectionTitle("Permissao")
            Segmented(listOf("Editar", "Comentar", "Ver"), permission) { permission = it }
            SectionTitle("Compartilhar por")
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SmallAction(Icons.AutoMirrored.Filled.Chat, "WhatsApp") { sendShare("com.whatsapp") }
                SmallAction(Icons.Default.Email, "E-mail") { sendEmail() }
                SmallAction(Icons.Default.ContentCopy, "Copiar") { copyInvite() }
            }
            FeedbackBanner(message, FeedbackKind.Success, Modifier.padding(top = 10.dp))
            SectionTitle("Previa da mensagem")
            TaskFlowCard {
                Text("Voce foi convidado para participar desta tarefa:", color = TaskFlowColors.Muted)
                Text(task.title, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = TaskFlowColors.Text, modifier = Modifier.padding(top = 8.dp))
                Text("Prazo: ${task.dueDate?.format(DateTimeFormatter.ofPattern("dd/MM HH:mm")) ?: "sem prazo"}", color = TaskFlowColors.Muted)
                Text("Status: ${task.status.label}", color = TaskFlowColors.Muted)
                Row(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ChipText("${attachments.count { it.taskId == task.id }} anexos", tone = ChipTone.Blue)
                    ChipText("${links.count { it.taskId == task.id }} links", tone = ChipTone.Success)
                }
                Text(lastInviteUrl ?: "O link HTTPS sera gerado ao enviar.", color = TaskFlowColors.Purple, modifier = Modifier.padding(top = 12.dp))
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
            GradientButton(
                "Enviar convite",
                onClick = {
                    copyInvite()
                },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
