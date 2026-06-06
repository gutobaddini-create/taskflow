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
    fun deleteTask(taskId: String)
    fun createSpace(name: String)
    fun updateSpace(space: Space)
    fun deleteSpace(spaceId: String)
    fun createList(spaceId: String, name: String)
    fun updateList(list: TaskList)
    fun deleteList(listId: String)
    fun saveReminder(reminder: Reminder)
    fun addAttachment(attachment: Attachment)
    fun deleteAttachment(attachmentId: String)
    fun addLink(link: TaskLink)
    fun updateLink(link: TaskLink)
    fun deleteLink(linkId: String)
    fun addCustomField(field: CustomField)
    fun updateCustomField(field: CustomField)
    fun deleteCustomField(fieldId: String)
    fun addChecklistItem(item: ChecklistItem)
    fun updateChecklistItem(item: ChecklistItem)
    fun toggleChecklistItem(itemId: String)
    fun deleteChecklistItem(itemId: String)
    fun addComment(comment: Comment)
    fun createInvite(invite: Invite)
    fun acceptInvite(token: String, userId: String)
    fun declineInvite(token: String)
}
