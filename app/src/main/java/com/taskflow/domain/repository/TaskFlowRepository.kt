package com.taskflow.domain.repository

import com.taskflow.domain.model.*
import kotlinx.coroutines.flow.StateFlow

interface TaskFlowRepository {
    val users: StateFlow<List<User>>
    val spaces: StateFlow<List<Space>>
    val lists: StateFlow<List<TaskList>>
    val tasks: StateFlow<List<Task>>
    val reminders: StateFlow<List<Reminder>>
    val attachments: StateFlow<List<Attachment>>
    val links: StateFlow<List<TaskLink>>
    val customFields: StateFlow<List<CustomField>>
    val checklist: StateFlow<List<ChecklistItem>>
    val comments: StateFlow<List<Comment>>
    val invites: StateFlow<List<Invite>>
    val activity: StateFlow<List<ActivityLog>>

    fun createTask(task: Task)
    fun updateTask(task: Task)
    fun completeTask(taskId: String)
    fun createSpace(name: String)
    fun createList(spaceId: String, name: String)
    fun saveReminder(reminder: Reminder)
    fun addAttachment(attachment: Attachment)
    fun addLink(link: TaskLink)
    fun addCustomField(field: CustomField)
    fun addChecklistItem(item: ChecklistItem)
    fun toggleChecklistItem(itemId: String)
    fun addComment(comment: Comment)
    fun createInvite(invite: Invite)
}
