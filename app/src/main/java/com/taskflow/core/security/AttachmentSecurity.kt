package com.taskflow.core.security

import com.taskflow.domain.model.Attachment
import java.net.URI

object AttachmentSecurity {
    fun withoutPublicPermanentUrls(attachment: Attachment): Attachment {
        return attachment.copy(
            storagePath = privateStoragePath(attachment),
            secureUrl = attachment.secureUrl?.takeUnless(::isPublicWebUrl),
            thumbnailUrl = attachment.thumbnailUrl?.takeUnless(::isPublicWebUrl)
        )
    }

    fun isPublicWebUrl(value: String): Boolean {
        val uri = runCatching { URI(value) }.getOrNull() ?: return false
        return uri.scheme.equals("http", ignoreCase = true) || uri.scheme.equals("https", ignoreCase = true)
    }

    private fun privateStoragePath(attachment: Attachment): String {
        return if (isPublicWebUrl(attachment.storagePath)) {
            "local/attachments/${attachment.id}/${attachment.fileName}"
        } else {
            attachment.storagePath
        }
    }
}
