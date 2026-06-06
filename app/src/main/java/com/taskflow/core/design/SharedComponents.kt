package com.taskflow.core.design

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.taskflow.domain.model.Task
import com.taskflow.domain.model.TaskPriority
import com.taskflow.domain.model.TaskStatus
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Composable
fun TaskCard(task: Task, listName: String, hasReminder: Boolean, onClick: () -> Unit) {
    TaskFlowCard(Modifier.touchFeedback(onClick = onClick)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.width(5.dp).height(66.dp).clip(RoundedCornerShape(20.dp)).background(priorityColor(task.priority)))
            Spacer(Modifier.width(14.dp))
            Box(Modifier.size(34.dp).border(2.dp, TaskFlowColors.Border, CircleShape).clip(CircleShape))
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(task.title, fontSize = 21.sp, fontWeight = FontWeight.Bold, color = TaskFlowColors.Text, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text("${task.dueDate?.format(DateTimeFormatter.ofPattern("HH:mm")) ?: "--:--"}  -  $listName", color = TaskFlowColors.Muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            PriorityPill(task.priority)
            if (hasReminder) Icon(Icons.Default.Notifications, null, tint = TaskFlowColors.Muted, modifier = Modifier.padding(start = 8.dp))
        }
    }
}

@Composable
fun NextReminderCard(title: String, date: LocalDateTime) {
    TaskFlowCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconBubble(Icons.Default.Event, TaskFlowColors.Purple.copy(.14f), TaskFlowColors.Purple)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text("Proximo lembrete", color = TaskFlowColors.Purple, fontWeight = FontWeight.Bold)
                Text(title, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = TaskFlowColors.Text, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy - HH:mm")), color = TaskFlowColors.Muted)
            }
            Icon(Icons.Default.ChevronRight, null, tint = TaskFlowColors.Text)
        }
    }
}

@Composable
fun Segmented(options: List<String>, selected: String, onSelect: (String) -> Unit) {
    SegmentedControl(options, selected, onSelect)
}

@Composable
fun GradientButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    TaskFlowButton(text, onClick, modifier, enabled)
}

@Composable
fun IconTile(icon: ImageVector, bg: Color = Color.White, tint: Color = TaskFlowColors.Text) =
    Box(Modifier.size(48.dp).clip(RoundedCornerShape(16.dp)).background(bg), contentAlignment = Alignment.Center) {
        Icon(icon, null, tint = tint)
    }

@Composable
fun IconBubble(icon: ImageVector, bg: Color = TaskFlowColors.Blue.copy(.12f), tint: Color = TaskFlowColors.Blue) =
    Box(Modifier.size(42.dp).clip(CircleShape).background(bg), contentAlignment = Alignment.Center) {
        Icon(icon, null, tint = tint)
    }

@Composable
fun SectionTitle(text: String) =
    Text(text, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TaskFlowColors.Text, modifier = Modifier.padding(top = 22.dp, bottom = 10.dp))

@Composable
fun ScreenTitle(text: String, modifier: Modifier = Modifier) =
    Text(
        text,
        fontSize = 28.sp,
        fontWeight = FontWeight.Bold,
        color = TaskFlowColors.Text,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier
    )

@Composable
fun ChipText(text: String, active: Boolean = true, modifier: Modifier = Modifier, tone: ChipTone = ChipTone.Purple) {
    val color = if (active) chipToneColor(tone) else TaskFlowColors.Muted
    Text(
        text,
        color = color,
        fontWeight = FontWeight.SemiBold,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(if (active) color.copy(.10f) else TaskFlowColors.Border.copy(.45f))
            .padding(horizontal = 12.dp, vertical = 8.dp)
    )
}

enum class ChipTone { Purple, Blue, Success, Warning, Danger }

private fun chipToneColor(tone: ChipTone) = when (tone) {
    ChipTone.Purple -> TaskFlowColors.Purple
    ChipTone.Blue -> TaskFlowColors.Blue
    ChipTone.Success -> TaskFlowColors.Success
    ChipTone.Warning -> TaskFlowColors.Warning
    ChipTone.Danger -> TaskFlowColors.Danger
}

@Composable
fun PriorityPill(priority: TaskPriority) =
    Text(
        priority.label,
        color = priorityColor(priority),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(priorityColor(priority).copy(.12f))
            .padding(horizontal = 12.dp, vertical = 8.dp)
    )

@Composable
fun StatusPill(status: TaskStatus) =
    Text(
        status.label,
        color = TaskFlowColors.Blue,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(TaskFlowColors.Blue.copy(.10f))
            .padding(horizontal = 12.dp, vertical = 8.dp)
    )

@Composable
fun SmallAction(icon: ImageVector, label: String, modifier: Modifier = Modifier, onClick: () -> Unit = {}) =
    OutlinedButton(onClick = onClick, modifier = modifier, shape = RoundedCornerShape(18.dp), contentPadding = PaddingValues(10.dp)) {
        Icon(icon, null)
        Spacer(Modifier.width(6.dp))
        Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }

@Composable
fun TopRow(action: String, title: String, onAction: () -> Unit) =
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        TextButton(onClick = onAction) { Text(action) }
        Spacer(Modifier.weight(1f))
        Text(title, fontWeight = FontWeight.Bold, color = TaskFlowColors.Text)
        Spacer(Modifier.weight(1f))
        Spacer(Modifier.width(76.dp))
    }

@Composable
fun InfoRow(label: String, value: String) =
    Row(Modifier.fillMaxWidth().padding(vertical = 7.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = TaskFlowColors.Muted)
        Text(value, color = TaskFlowColors.Text, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }

@Composable
fun MaterialRow(icon: ImageVector, title: String, subtitle: String) =
    TaskFlowCard(Modifier.padding(bottom = 10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconBubble(icon, TaskFlowColors.Purple.copy(.10f), TaskFlowColors.Purple)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold, color = TaskFlowColors.Text, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(subtitle, color = TaskFlowColors.Muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Icon(Icons.Default.MoreVert, null, tint = TaskFlowColors.Muted)
        }
    }

enum class FeedbackKind { Info, Success, Warning, Error }

@Composable
fun FeedbackBanner(message: String?, kind: FeedbackKind, modifier: Modifier = Modifier) {
    AnimatedVisibility(
        visible = !message.isNullOrBlank(),
        enter = fadeIn() + expandVertically(),
        exit = fadeOut()
    ) {
        val color = when (kind) {
            FeedbackKind.Info -> TaskFlowColors.Blue
            FeedbackKind.Success -> TaskFlowColors.Success
            FeedbackKind.Warning -> TaskFlowColors.Warning
            FeedbackKind.Error -> TaskFlowColors.Danger
        }
        val icon = when (kind) {
            FeedbackKind.Error -> Icons.Default.ErrorOutline
            FeedbackKind.Success -> Icons.Default.CheckCircle
            else -> Icons.Default.Info
        }
        Row(
            modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(color.copy(.10f))
                .border(1.dp, color.copy(.24f), RoundedCornerShape(18.dp))
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, null, tint = color, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(10.dp))
            Text(message ?: "", color = TaskFlowColors.Text, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
    }
}

fun priorityColor(priority: TaskPriority) = when (priority) {
    TaskPriority.High -> TaskFlowColors.Danger
    TaskPriority.Medium -> TaskFlowColors.Blue
    TaskPriority.Low -> TaskFlowColors.Success
}
