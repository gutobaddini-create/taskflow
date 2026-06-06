package com.taskflow.feature.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.taskflow.core.design.GradientButton
import com.taskflow.core.design.Segmented
import com.taskflow.core.app.TaskFlowViewModel
import com.taskflow.core.design.FeedbackBanner
import com.taskflow.core.design.FeedbackKind
import com.taskflow.core.design.TaskFlowColors

@Composable
fun OnboardingScreen(vm: TaskFlowViewModel, onStart: () -> Unit) {
    val users by vm.users.collectAsState()
    var mode by remember { mutableStateOf("Criar conta") }
    var name by remember(users) { mutableStateOf(users.firstOrNull()?.name ?: "Ana") }
    var email by remember(users) { mutableStateOf(users.firstOrNull()?.email ?: "ana@taskflow.local") }
    var message by remember { mutableStateOf("") }

    Column(
        Modifier.fillMaxSize().padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Default.NotificationsActive, null, tint = TaskFlowColors.Purple, modifier = Modifier.size(86.dp))
        Spacer(Modifier.height(26.dp))
        Text("TaskFlow", fontSize = 38.sp, fontWeight = FontWeight.Bold, color = TaskFlowColors.Text)
        Text(
            "Organize tarefas, lembretes e materiais em um fluxo unico.",
            color = TaskFlowColors.Muted,
            modifier = Modifier.padding(top = 12.dp),
            lineHeight = 22.sp
        )
        Spacer(Modifier.height(36.dp))
        Segmented(listOf("Criar conta", "Entrar"), mode) {
            mode = it
            message = ""
        }
        Spacer(Modifier.height(16.dp))
        if (mode == "Criar conta") {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Nome") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(Modifier.height(10.dp))
        }
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("E-mail local") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        FeedbackBanner(message.takeIf { it.isNotBlank() }, FeedbackKind.Error, Modifier.padding(top = 12.dp))
        Spacer(Modifier.height(24.dp))
        GradientButton(
            "Comecar",
            onClick = {
                val ok = if (mode == "Criar conta") vm.registerLocal(name, email) else vm.loginLocal(email)
                if (ok) {
                    onStart()
                } else {
                    message = if (mode == "Criar conta") "Informe nome e e-mail validos." else "Usuario local nao encontrado."
                }
            },
            modifier = Modifier.fillMaxWidth()
        )
        TextButton(
            onClick = {
                mode = if (mode == "Criar conta") "Entrar" else "Criar conta"
                message = ""
            }
        ) {
            Text(if (mode == "Criar conta") "Ja tenho uma conta" else "Criar nova conta local")
        }
    }
}
