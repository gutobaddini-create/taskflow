package com.taskflow

import com.taskflow.data.repository.InMemoryTaskFlowRepository
import com.taskflow.domain.model.Task
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RepositoryTaskCreationTest {
    @Test
    fun createTaskRejectsMissingList() {
        val repo = InMemoryTaskFlowRepository()
        val beforeTasks = repo.tasks.value.size
        val beforeActivity = repo.activity.value.size

        repo.createTask(
            Task(
                spaceId = repo.spaces.value.first().id,
                listId = "missing-list",
                title = "Tarefa sem lista",
                createdBy = repo.users.value.first().id
            )
        )

        assertEquals(beforeTasks, repo.tasks.value.size)
        assertEquals(beforeActivity, repo.activity.value.size)
        assertFalse(repo.tasks.value.any { it.title == "Tarefa sem lista" })
    }

    @Test
    fun createTaskRejectsListFromAnotherSpace() {
        val repo = InMemoryTaskFlowRepository()
        val list = repo.lists.value.first()
        val otherSpace = repo.spaces.value.first { it.id != list.spaceId }
        val beforeTasks = repo.tasks.value.size

        repo.createTask(
            Task(
                spaceId = otherSpace.id,
                listId = list.id,
                title = "Tarefa em espaco incorreto",
                createdBy = repo.users.value.first().id
            )
        )

        assertEquals(beforeTasks, repo.tasks.value.size)
        assertFalse(repo.tasks.value.any { it.title == "Tarefa em espaco incorreto" })
    }

    @Test
    fun createTaskAcceptsValidList() {
        val repo = InMemoryTaskFlowRepository()
        val list = repo.lists.value.first()
        val task = Task(
            spaceId = list.spaceId,
            listId = list.id,
            title = "Tarefa com lista valida",
            createdBy = repo.users.value.first().id
        )

        repo.createTask(task)

        assertTrue(repo.tasks.value.any { it.id == task.id })
        assertTrue(repo.activity.value.any { it.taskId == task.id && it.action == "Tarefa criada" })
    }
}
