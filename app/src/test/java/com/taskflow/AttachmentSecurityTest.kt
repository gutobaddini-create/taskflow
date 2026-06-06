package com.taskflow

import com.taskflow.core.security.AttachmentSecurity
import com.taskflow.data.repository.InMemoryTaskFlowRepository
import com.taskflow.domain.model.Attachment
import com.taskflow.domain.model.AttachmentType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AttachmentSecurityTest {
    @Test
    fun sanitizerRemovesPublicWebUrls() {
        val attachment = Attachment(
            id = "att-1",
            taskId = "task",
            uploadedBy = "user",
            fileName = "contrato.pdf",
            originalFileName = "contrato.pdf",
            fileType = AttachmentType.Pdf,
            mimeType = "application/pdf",
            fileSize = 42,
            storagePath = "https://cdn.example.com/contrato.pdf",
            secureUrl = "https://cdn.example.com/signed/contrato.pdf",
            thumbnailUrl = "http://cdn.example.com/thumb.png"
        )

        val safe = AttachmentSecurity.withoutPublicPermanentUrls(attachment)

        assertFalse(AttachmentSecurity.isPublicWebUrl(safe.storagePath))
        assertEquals("local/attachments/att-1/contrato.pdf", safe.storagePath)
        assertNull(safe.secureUrl)
        assertNull(safe.thumbnailUrl)
    }

    @Test
    fun sanitizerKeepsLocalContentUri() {
        val attachment = Attachment(
            taskId = "task",
            uploadedBy = "user",
            fileName = "foto.jpg",
            originalFileName = "foto.jpg",
            fileType = AttachmentType.Image,
            mimeType = "image/jpeg",
            fileSize = 42,
            storagePath = "content://media/picker/foto"
        )

        val safe = AttachmentSecurity.withoutPublicPermanentUrls(attachment)

        assertEquals("content://media/picker/foto", safe.storagePath)
    }

    @Test
    fun repositoryPersistsOnlySanitizedAttachmentUrls() {
        val repo = InMemoryTaskFlowRepository()
        val task = repo.tasks.value.first()
        val user = repo.users.value.first()

        repo.addAttachment(
            Attachment(
                taskId = task.id,
                uploadedBy = user.id,
                fileName = "publico.pdf",
                originalFileName = "publico.pdf",
                fileType = AttachmentType.Pdf,
                mimeType = "application/pdf",
                fileSize = 42,
                storagePath = "https://files.example.com/publico.pdf",
                secureUrl = "https://files.example.com/publico.pdf"
            )
        )

        val saved = repo.attachments.value.first { it.fileName == "publico.pdf" }
        assertFalse(AttachmentSecurity.isPublicWebUrl(saved.storagePath))
        assertNull(saved.secureUrl)
        assertTrue(repo.activity.value.any { it.taskId == task.id && it.action == "Anexo adicionado: publico.pdf" })
    }
}
