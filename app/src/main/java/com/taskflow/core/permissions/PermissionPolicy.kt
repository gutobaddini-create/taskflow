package com.taskflow.core.permissions

import com.taskflow.domain.model.Invite
import com.taskflow.domain.model.Task
import com.taskflow.domain.model.UserPermission
import com.taskflow.domain.model.now

object PermissionPolicy {
    val notificationPermission = "android.permission.POST_NOTIFICATIONS"
    val cameraPermission = "android.permission.CAMERA"

    fun acceptedPermission(taskId: String, userId: String, invites: List<Invite>): UserPermission? =
        invites
            .filter { it.taskId == taskId && it.acceptedBy == userId && isInviteActive(it) }
            .maxByOrNull { permissionRank(it.permission) }
            ?.permission

    fun isInviteActive(invite: Invite, referenceTime: Long = now()): Boolean =
        invite.expiresAt?.let { it > referenceTime } != false

    fun canViewTask(task: Task, userId: String, permission: UserPermission?): Boolean =
        task.createdBy == userId ||
            task.assignedTo == userId ||
            permission != null

    fun canEditTask(task: Task, userId: String, permission: UserPermission?): Boolean =
        task.createdBy == userId || task.assignedTo == userId || permission == UserPermission.Owner

    fun canCommentOnTask(task: Task, userId: String, permission: UserPermission?): Boolean =
        canEditTask(task, userId, permission) ||
            permission == UserPermission.Participant ||
            permission == UserPermission.Responsible

    fun canViewMaterial(task: Task, userId: String, permission: UserPermission?): Boolean =
        canViewTask(task, userId, permission)

    fun canManageMaterial(task: Task, userId: String, permission: UserPermission?): Boolean =
        canEditTask(task, userId, permission)

    private fun permissionRank(permission: UserPermission): Int =
        when (permission) {
            UserPermission.Viewer -> 0
            UserPermission.Participant -> 1
            UserPermission.Responsible -> 2
            UserPermission.Owner -> 3
        }
}
