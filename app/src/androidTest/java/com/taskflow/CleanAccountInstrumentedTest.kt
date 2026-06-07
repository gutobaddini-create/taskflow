package com.taskflow

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.taskflow.data.local.LocalPersistencePlan
import com.taskflow.data.local.TaskFlowDatabase
import com.taskflow.data.repository.LocalTaskFlowRepository
import com.taskflow.domain.model.User
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CleanAccountInstrumentedTest {
    @Test
    fun newLocalAccountStartsWithoutDemoTasks() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        context.deleteDatabase(LocalPersistencePlan.databaseName)
        val dao = TaskFlowDatabase.get(context).dao()
        val repo = LocalTaskFlowRepository(dao, context, CoroutineScope(SupervisorJob() + Dispatchers.Main))
        val user = User(id = "clean-user", name = "Conta limpa", email = "limpa@taskflow.local")

        repo.saveUser(user)
        delay(600)

        assertEquals(0, dao.taskCount())
        assertEquals(1, dao.ownedSpaceCount(user.id))
        assertTrue(repo.spaces.value.all { it.ownerId == user.id })
        assertTrue(repo.lists.value.all { list -> repo.spaces.value.any { it.id == list.spaceId && it.ownerId == user.id } })
    }
}
