package com.taskflow.feature.people

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.taskflow.core.design.InfoRow
import com.taskflow.core.design.SectionTitle
import com.taskflow.TaskFlowViewModel
import com.taskflow.core.design.TaskFlowCard
import com.taskflow.core.design.TaskFlowColors
import com.taskflow.domain.model.Invite
import com.taskflow.domain.model.UserPermission

@Composable
fun PeopleScreen(vm: TaskFlowViewModel) {
    val users by vm.users.collectAsState()
    val invites by vm.invites.collectAsState()
    val preferences by vm.preferences.collectAsState()

    LazyColumn(Modifier.fillMaxSize().statusBarsPadding().padding(24.dp), contentPadding = PaddingValues(bottom = 120.dp)) {
        item {
            Text("Pessoas", fontSize = 30.sp, fontWeight = FontWeight.Bold, color = TaskFlowColors.Text)
            Text("Convites e participantes", color = TaskFlowColors.Muted)
            Spacer(Modifier.height(16.dp))
        }
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
            TaskFlowCard {
                InfoRow("Permissao", it.permission.label)
                InfoRow("Token", it.token.take(8))
                InfoRow("Status", if (it.acceptedBy == null) "Pendente" else "Aceito")
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}
