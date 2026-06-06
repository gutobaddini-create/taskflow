package com.taskflow

import com.taskflow.core.utils.TaskSearch
import com.taskflow.domain.model.CustomField
import com.taskflow.domain.model.CustomFieldType
import com.taskflow.domain.model.Task
import com.taskflow.domain.model.TaskLink
import com.taskflow.domain.model.TaskList
import com.taskflow.domain.model.TaskPriority
import com.taskflow.domain.model.TaskStatus
import com.taskflow.domain.model.User
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.system.measureTimeMillis

class TaskSearchTest {
    private val user = User(id = "ana", name = "Ana", email = "ana@taskflow.local")
    private val list = TaskList(id = "prazos", spaceId = "space", name = "Prazos")
    private val task = Task(
        id = "task-1",
        spaceId = "space",
        listId = list.id,
        title = "Enviar proposta",
        description = "Revisar contrato",
        status = TaskStatus.InProgress,
        priority = TaskPriority.High,
        createdBy = "manuel",
        assignedTo = user.id
    )

    @Test
    fun matchesLinkCategoryAsSearchTag() {
        val links = listOf(
            TaskLink(taskId = task.id, createdBy = "manuel", title = "Briefing", url = "https://example.com", category = "Cliente")
        )

        assertTrue(TaskSearch.matches(task, "cliente", listOf(user), listOf(list), emptyList(), links, emptyList()))
    }

    @Test
    fun matchesDerivedTaskTags() {
        assertTrue(TaskSearch.matches(task, "alta", listOf(user), listOf(list), emptyList(), emptyList(), emptyList()))
        assertTrue(TaskSearch.matches(task, "prazos", listOf(user), listOf(list), emptyList(), emptyList(), emptyList()))
        assertTrue(TaskSearch.matches(task, "andamento", listOf(user), listOf(list), emptyList(), emptyList(), emptyList()))
    }

    @Test
    fun matchesCustomFieldCategoryData() {
        val fields = listOf(
            CustomField(taskId = task.id, fieldName = "Categoria", fieldType = CustomFieldType.Text, fieldValue = "Jurídico", createdBy = "manuel")
        )

        assertTrue(TaskSearch.matches(task, "jurídico", listOf(user), listOf(list), emptyList(), emptyList(), fields))
    }

    @Test
    fun filtersLargeListQuickly() {
        val tasks = (0 until 5_000).map {
            task.copy(id = "task-$it", title = if (it == 4_999) "Alvará urgente" else "Rotina $it")
        }

        val elapsed = measureTimeMillis {
            val result = tasks.filter { TaskSearch.matches(it, "alvará", listOf(user), listOf(list), emptyList(), emptyList(), emptyList()) }
            assertEquals(listOf("task-4999"), result.map { it.id })
        }

        assertTrue("Search took ${elapsed}ms", elapsed < 1_000)
    }
}
