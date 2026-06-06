package com.taskflow.feature.settings

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
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.taskflow.core.design.InfoRow
import com.taskflow.core.design.SectionTitle
import com.taskflow.core.design.Segmented
import com.taskflow.core.app.TaskFlowViewModel
import com.taskflow.core.design.TaskFlowCard
import com.taskflow.core.design.TaskFlowColors

@Composable
fun SettingsScreen(vm: TaskFlowViewModel, onLogout: () -> Unit) {
    val preferences by vm.preferences.collectAsState()
    val pendingOperations by vm.pendingOperations.collectAsState()

    LazyColumn(Modifier.fillMaxSize().statusBarsPadding().padding(24.dp), contentPadding = PaddingValues(bottom = 120.dp)) {
        item {
            Text("Ajustes", fontSize = 30.sp, fontWeight = FontWeight.Bold, color = TaskFlowColors.Text)
            Spacer(Modifier.height(16.dp))
            TaskFlowCard {
                InfoRow("Meu perfil", "Manuel")
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 7.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Notificacoes", color = TaskFlowColors.Muted)
                    Switch(checked = preferences.notificationsEnabled, onCheckedChange = vm::setNotificationsEnabled)
                }
                Text("Tema", color = TaskFlowColors.Muted, modifier = Modifier.padding(top = 10.dp))
                Segmented(listOf("Claro", "Escuro futuro"), preferences.theme) { vm.setTheme(it) }
                InfoRow("Filtro inicial", preferences.homeFilter)
                InfoRow("Backups e sincronizacao", "Local-first")
                InfoRow(
                    "Pendencias de sincronizacao",
                    if (pendingOperations.isEmpty()) "Nenhuma" else "${pendingOperations.size} operacoes aguardando Firebase"
                )
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
            TextButton(onClick = onLogout, modifier = Modifier.fillMaxWidth()) {
                Text("Sair da conta", color = Color(0xFFEF4444))
            }
        }
    }
}
