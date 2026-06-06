package com.taskflow

import com.taskflow.data.repository.InMemoryTaskFlowRepository
import org.junit.Assert.assertEquals
import org.junit.Test

class RepositoryListOrderingTest {
    @Test
    fun createListAppendsInsideSpace() {
        val repo = InMemoryTaskFlowRepository()
        val space = repo.spaces.value.first()
        val before = repo.lists.value.filter { it.spaceId == space.id }.maxOf { it.order }

        repo.createList(space.id, "Depois")

        val created = repo.lists.value.first { it.spaceId == space.id && it.name == "Depois" }
        assertEquals(before + 1, created.order)
    }

    @Test
    fun updateListOrderChangesSortedOrder() {
        val repo = InMemoryTaskFlowRepository()
        val space = repo.spaces.value.first()
        repo.createList(space.id, "Depois")
        val first = repo.lists.value.filter { it.spaceId == space.id }.minBy { it.order }
        val second = repo.lists.value.first { it.spaceId == space.id && it.name == "Depois" }

        repo.updateList(first.copy(order = second.order))
        repo.updateList(second.copy(order = first.order))

        val orderedNames = repo.lists.value.filter { it.spaceId == space.id }.sortedBy { it.order }.map { it.name }
        assertEquals("Depois", orderedNames.first())
    }
}
