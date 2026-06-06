package com.taskflow

import com.taskflow.data.repository.InMemoryTaskFlowRepository
import com.taskflow.domain.model.User
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalAuthRepositoryTest {
    @Test
    fun saveUserAddsLocalAccountAndReplacesByEmail() {
        val repo = InMemoryTaskFlowRepository()
        val user = User(name = "Guto", email = "guto@taskflow.local")

        repo.saveUser(user)
        repo.saveUser(user.copy(name = "Guto Atualizado"))

        val saved = repo.users.value.filter { it.email == "guto@taskflow.local" }
        assertEquals(1, saved.size)
        assertEquals("Guto Atualizado", saved.first().name)
        assertTrue(repo.users.value.any { it.id == user.id })
    }
}
