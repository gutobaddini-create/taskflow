package com.taskflow.core.permissions

import com.taskflow.domain.model.Invite
import com.taskflow.domain.model.Task
import com.taskflow.domain.model.UserPermission

object PermissionPolicy {
    val notificationPermission = "android.permission.POST_NOTIFICATIONS"
    val cameraPermission = "android.permission.CAMERA"

    fun acceptedPermission(taskId: String, userId: String, invites: List<Invite>): UserPermission? =
        invites.firstOrNull { it.taskId == taskId && it.acceptedBy == userId }?.permission

    fun canEditTask(task: Task, userId: String, permission: UserPermission?): Boolean =
        task.createdBy == userId || task.assignedTo == userId || permission == UserPermission.Owner

    fun canCommentOnTask(task: Task, userId: String, permission: UserPermission?): Boolean =
        canEditTask(task, userId, permission) ||
            permission == UserPermission.Participant ||
            permission == UserPermission.Responsible
}
