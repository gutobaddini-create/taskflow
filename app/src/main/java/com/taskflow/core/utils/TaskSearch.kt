package com.taskflow.core.utils

import com.taskflow.domain.model.Attachment
import com.taskflow.domain.model.CustomField
import com.taskflow.domain.model.Task
import com.taskflow.domain.model.TaskLink
import com.taskflow.domain.model.TaskList
import com.taskflow.domain.model.User

object TaskSearch {
    fun matches(
        task: Task,
        query: String,
        users: List<User>,
        lists: List<TaskList>,
        attachments: List<Attachment>,
        links: List<TaskLink>,
        fields: List<CustomField>
    ): Boolean {
        val normalized = query.trim().lowercase()
        if (normalized.isBlank()) return true
        return index(task, users, lists, attachments, links, fields).contains(normalized)
    }

    fun index(
        task: Task,
        users: List<User>,
        lists: List<TaskList>,
        attachments: List<Attachment>,
        links: List<TaskLink>,
        fields: List<CustomField>
    ): String {
        val assignee = users.firstOrNull { it.id == task.assignedTo }?.name.orEmpty()
        val listName = lists.firstOrNull { it.id == task.listId }?.name.orEmpty()
        val taskAttachments = attachments.filter { it.taskId == task.id }.joinToString(" ") {
            "${it.fileName} ${it.originalFileName} ${it.fileType.name}"
        }
        val taskLinks = links.filter { it.taskId == task.id }.joinToString(" ") {
            "${it.title} ${it.url} ${it.description} ${it.category}"
        }
        val taskFields = fields.filter { it.taskId == task.id }.joinToString(" ") {
            "${it.fieldName} ${it.fieldType.name} ${it.fieldValue}"
        }
        val derivedTags = listOf(
            task.status.label,
            task.status.name,
            task.priority.label,
            task.priority.name,
            listName
        ).joinToString(" ")

        return listOf(
            task.title,
            task.description,
            assignee,
            derivedTags,
            taskAttachments,
            taskLinks,
            taskFields
        ).joinToString(" ").lowercase()
    }
}
