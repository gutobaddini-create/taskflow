package com.taskflow.core.utils

import com.taskflow.domain.model.AttachmentType
import java.net.URI

fun isValidUrl(value: String): Boolean = runCatching {
    val uri = URI(value.trim())
    uri.scheme in setOf("http", "https") && !uri.host.isNullOrBlank()
}.getOrDefault(false)

fun isAllowedAttachment(fileName: String, sizeBytes: Long): Boolean {
    val ext = fileName.substringAfterLast('.', "").lowercase()
    return sizeBytes <= 20L * 1024L * 1024L && ext in setOf("jpg", "jpeg", "png", "pdf", "doc", "docx", "xls", "xlsx", "txt")
}

fun attachmentType(fileName: String): AttachmentType = when (fileName.substringAfterLast('.', "").lowercase()) {
    "jpg", "jpeg", "png" -> AttachmentType.Image
    "pdf" -> AttachmentType.Pdf
    "doc", "docx" -> AttachmentType.Document
    "xls", "xlsx" -> AttachmentType.Spreadsheet
    "txt" -> AttachmentType.Text
    else -> AttachmentType.Other
}
