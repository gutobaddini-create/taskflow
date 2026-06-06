package com.taskflow.core.design

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

object DesignTokens {
    val cardRadius = 24.dp
    val compactRadius = 16.dp
    val pillRadius = 50.dp
    val screenPadding = 24.dp
    val cardPadding = 18.dp
    val itemGap = 12.dp
    val controlHeight = 54.dp
}

object TaskFlowColors {
    val OffWhite = Color(0xFFF7F8FC)
    val Surface = Color.White
    val Text = Color(0xFF07132F)
    val Muted = Color(0xFF667085)
    val Blue = Color(0xFF2563FF)
    val Purple = Color(0xFF7C3AED)
    val Border = Color(0xFFE5E7EB)
    val Success = Color(0xFF22C55E)
    val Danger = Color(0xFFEF4444)
    val Warning = Color(0xFFF59E0B)
}

data class TaskFlowPalette(
    val background: Color,
    val surface: Color,
    val text: Color,
    val muted: Color,
    val border: Color,
    val primary: Color,
    val secondary: Color,
    val success: Color,
    val danger: Color,
    val warning: Color
)

private val LightTaskFlowPalette = TaskFlowPalette(
    background = TaskFlowColors.OffWhite,
    surface = TaskFlowColors.Surface,
    text = TaskFlowColors.Text,
    muted = TaskFlowColors.Muted,
    border = TaskFlowColors.Border,
    primary = TaskFlowColors.Blue,
    secondary = TaskFlowColors.Purple,
    success = TaskFlowColors.Success,
    danger = TaskFlowColors.Danger,
    warning = TaskFlowColors.Warning
)

private val DarkTaskFlowPalette = TaskFlowPalette(
    background = Color(0xFF0B1020),
    surface = Color(0xFF141A2E),
    text = Color(0xFFF8FAFC),
    muted = Color(0xFFB7C0D1),
    border = Color(0xFF27324A),
    primary = Color(0xFF7AA2FF),
    secondary = Color(0xFFA78BFA),
    success = Color(0xFF4ADE80),
    danger = Color(0xFFF87171),
    warning = Color(0xFFFBBF24)
)

private val LocalTaskFlowPalette = staticCompositionLocalOf { LightTaskFlowPalette }

object TaskFlowThemeValues {
    val colors: TaskFlowPalette
        @Composable get() = LocalTaskFlowPalette.current
}

val TaskFlowGradient = Brush.horizontalGradient(listOf(TaskFlowColors.Blue, TaskFlowColors.Purple))

private val TaskFlowType = Typography(
    titleLarge = androidx.compose.ui.text.TextStyle(
        color = TaskFlowColors.Text,
        fontSize = 24.sp,
        fontWeight = FontWeight.Bold,
        lineHeight = 30.sp
    ),
    titleMedium = androidx.compose.ui.text.TextStyle(
        color = TaskFlowColors.Text,
        fontSize = 18.sp,
        fontWeight = FontWeight.Bold,
        lineHeight = 24.sp
    ),
    bodyLarge = androidx.compose.ui.text.TextStyle(
        color = TaskFlowColors.Text,
        fontSize = 16.sp,
        lineHeight = 22.sp
    ),
    bodyMedium = androidx.compose.ui.text.TextStyle(
        color = TaskFlowColors.Muted,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    labelLarge = androidx.compose.ui.text.TextStyle(
        fontSize = 15.sp,
        fontWeight = FontWeight.SemiBold,
        lineHeight = 20.sp
    )
)

@Composable
fun TaskFlowTheme(darkTheme: Boolean = false, content: @Composable () -> Unit) {
    val palette = if (darkTheme) DarkTaskFlowPalette else LightTaskFlowPalette
    val scheme = if (darkTheme) {
        darkColorScheme(
            primary = palette.primary,
            secondary = palette.secondary,
            background = palette.background,
            surface = palette.surface,
            error = palette.danger,
            onPrimary = Color.White,
            onSecondary = Color.White,
            onBackground = palette.text,
            onSurface = palette.text
        )
    } else {
        lightColorScheme(
            primary = palette.primary,
            secondary = palette.secondary,
            background = palette.background,
            surface = palette.surface,
            error = palette.danger,
            onPrimary = Color.White,
            onSecondary = Color.White,
            onBackground = palette.text,
            onSurface = palette.text
        )
    }
    CompositionLocalProvider(LocalTaskFlowPalette provides palette) {
        MaterialTheme(
            colorScheme = scheme,
            typography = TaskFlowType,
            content = content
        )
    }
}

@Composable
fun TaskFlowButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    Button(
        onClick = onClick,
        modifier = modifier.height(DesignTokens.controlHeight),
        enabled = enabled,
        shape = RoundedCornerShape(DesignTokens.pillRadius),
        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
        contentPadding = PaddingValues()
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .background(if (enabled) TaskFlowGradient else Brush.linearGradient(listOf(TaskFlowColors.Border, TaskFlowColors.Border))),
            contentAlignment = Alignment.Center
        ) {
            Text(text, color = Color.White, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun TaskFlowOutlinedButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(DesignTokens.controlHeight),
        enabled = enabled,
        shape = RoundedCornerShape(DesignTokens.pillRadius)
    ) {
        Text(text, color = if (enabled) TaskFlowColors.Blue else TaskFlowColors.Muted, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun TaskFlowCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(DesignTokens.cardRadius),
        colors = CardDefaults.cardColors(containerColor = TaskFlowColors.Surface),
        elevation = CardDefaults.cardElevation(3.dp)
    ) {
        Column(Modifier.padding(DesignTokens.cardPadding), content = content)
    }
}

@Composable
fun TaskCard(title: String, subtitle: String, priorityColor: Color, modifier: Modifier = Modifier, onClick: () -> Unit = {}) {
    TaskFlowCard(modifier.clickable(onClick = onClick)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.width(5.dp).height(58.dp).clip(RoundedCornerShape(DesignTokens.pillRadius)).background(priorityColor))
            Spacer(Modifier.width(DesignTokens.itemGap))
            Box(Modifier.size(32.dp).border(2.dp, TaskFlowColors.Border, CircleShape).clip(CircleShape))
            Spacer(Modifier.width(DesignTokens.itemGap))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(subtitle, style = MaterialTheme.typography.bodyMedium)
            }
            PriorityChip("Alta", priorityColor)
        }
    }
}

@Composable
fun ReminderCard(title: String, dateLabel: String, modifier: Modifier = Modifier) {
    TaskFlowCard(modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            DesignIconBubble(Icons.Default.Schedule, TaskFlowColors.Purple.copy(alpha = .12f), TaskFlowColors.Purple)
            Spacer(Modifier.width(DesignTokens.itemGap))
            Column(Modifier.weight(1f)) {
                Text("Proximo lembrete", color = TaskFlowColors.Purple, fontWeight = FontWeight.Bold)
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(dateLabel, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
fun AttachmentCard(title: String, subtitle: String, modifier: Modifier = Modifier) {
    TaskFlowCard(modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            DesignIconBubble(Icons.Default.Inbox, TaskFlowColors.Purple.copy(alpha = .12f), TaskFlowColors.Purple)
            Spacer(Modifier.width(DesignTokens.itemGap))
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold, color = TaskFlowColors.Text)
                Text(subtitle, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
fun LinkCard(title: String, url: String, modifier: Modifier = Modifier) {
    AttachmentCard(title = title, subtitle = url, modifier = modifier)
}

@Composable
fun CustomFieldRow(label: String, value: String, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth().padding(vertical = 7.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = TaskFlowColors.Muted)
        Text(value, color = TaskFlowColors.Text, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun StatusPill(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        color = TaskFlowColors.Blue,
        fontWeight = FontWeight.SemiBold,
        modifier = modifier.clip(RoundedCornerShape(DesignTokens.pillRadius)).background(TaskFlowColors.Blue.copy(alpha = .10f)).padding(horizontal = 12.dp, vertical = 8.dp)
    )
}

@Composable
fun PriorityChip(text: String, color: Color, modifier: Modifier = Modifier) {
    Text(
        text,
        color = color,
        fontWeight = FontWeight.SemiBold,
        modifier = modifier.clip(RoundedCornerShape(DesignTokens.pillRadius)).background(color.copy(alpha = .12f)).padding(horizontal = 12.dp, vertical = 8.dp)
    )
}

@Composable
fun SegmentedControl(options: List<String>, selected: String, onSelect: (String) -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(TaskFlowColors.Surface).border(1.dp, TaskFlowColors.Border, RoundedCornerShape(22.dp)).padding(4.dp)
    ) {
        options.forEach { option ->
            val active = option == selected
            Box(
                Modifier.weight(1f).clip(RoundedCornerShape(18.dp)).background(if (active) TaskFlowGradient else Brush.linearGradient(listOf(Color.Transparent, Color.Transparent))).clickable { onSelect(option) }.padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(option, color = if (active) Color.White else TaskFlowColors.Text, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
fun ToggleSwitch(checked: Boolean, onCheckedChange: (Boolean) -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    Switch(checked = checked, onCheckedChange = onCheckedChange, modifier = modifier, enabled = enabled)
}

@Composable
fun BottomNavigationBar(items: List<NavigationItem>, selectedRoute: String, onSelect: (String) -> Unit, modifier: Modifier = Modifier) {
    NavigationBar(
        modifier.clip(RoundedCornerShape(26.dp)),
        containerColor = TaskFlowColors.Surface
    ) {
        items.forEach { item ->
            NavigationBarItem(
                selected = selectedRoute == item.route,
                onClick = { onSelect(item.route) },
                icon = { Icon(item.icon, null) },
                label = { Text(item.label) }
            )
        }
    }
}

data class NavigationItem(val route: String, val label: String, val icon: ImageVector)

@Composable
fun FloatingAddButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    androidx.compose.material3.FloatingActionButton(
        onClick = onClick,
        containerColor = TaskFlowColors.Purple,
        contentColor = Color.White,
        modifier = modifier.size(64.dp)
    ) {
        Icon(Icons.Default.Add, null)
    }
}

@Composable
fun EmptyState(title: String, message: String, modifier: Modifier = Modifier) {
    StateMessage(Icons.Default.Inbox, title, message, modifier)
}

@Composable
fun LoadingState(label: String, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth().padding(DesignTokens.screenPadding), horizontalAlignment = Alignment.CenterHorizontally) {
        CircularProgressIndicator(color = TaskFlowColors.Purple)
        Spacer(Modifier.height(12.dp))
        Text(label, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
fun LoadingFullScreen(label: String) {
    Box(Modifier.fillMaxSize().background(TaskFlowThemeValues.colors.background), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = TaskFlowThemeValues.colors.secondary)
            Spacer(Modifier.height(12.dp))
            Text(label, color = TaskFlowThemeValues.colors.muted)
        }
    }
}

@Composable
fun ErrorState(title: String, message: String, modifier: Modifier = Modifier) {
    StateMessage(Icons.Default.ErrorOutline, title, message, modifier, iconColor = TaskFlowColors.Danger)
}

@Composable
private fun StateMessage(icon: ImageVector, title: String, message: String, modifier: Modifier = Modifier, iconColor: Color = TaskFlowColors.Muted) {
    Column(modifier.fillMaxWidth().padding(DesignTokens.screenPadding), horizontalAlignment = Alignment.CenterHorizontally) {
        DesignIconBubble(icon, iconColor.copy(alpha = .10f), iconColor)
        Spacer(Modifier.height(12.dp))
        Text(title, style = MaterialTheme.typography.titleMedium)
        Text(message, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun DesignIconBubble(icon: ImageVector, bg: Color, tint: Color) {
    Box(Modifier.size(42.dp).clip(CircleShape).background(bg), contentAlignment = Alignment.Center) {
        Icon(icon, null, tint = tint)
    }
}

@Preview(showBackground = true)
@Composable
private fun TaskFlowComponentsPreview() {
    TaskFlowTheme {
        TaskFlowPreviewContent()
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0B1020)
@Composable
private fun TaskFlowComponentsDarkPreview() {
    TaskFlowTheme(darkTheme = true) {
        TaskFlowPreviewContent()
    }
}

@Composable
private fun TaskFlowPreviewContent() {
    val colors = TaskFlowThemeValues.colors
    Surface(color = colors.background) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            TaskFlowButton("Salvar", {})
            TaskFlowOutlinedButton("Cancelar", {})
            SegmentedControl(listOf("Hoje", "Semana"), "Hoje", {})
            TaskCard("Enviar contrato", "Hoje - Juridico", colors.danger)
                ReminderCard("Renovar documento", "06/06/2026 - 09:00")
                AttachmentCard("Comprovante.pdf", "PDF - 240 KB")
                CustomFieldRow("Processo", "12345")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatusPill("Em andamento")
                PriorityChip("Alta", colors.danger)
            }
            EmptyState("Sem tarefas", "Crie a primeira tarefa para comecar.")
            ErrorState("Erro ao carregar", "Tente novamente em alguns instantes.")
            BottomNavigationBar(
                items = listOf(NavigationItem("home", "Hoje", Icons.Default.Home), NavigationItem("agenda", "Agenda", Icons.Default.Schedule)),
                selectedRoute = "home",
                onSelect = {}
            )
        }
    }
}
