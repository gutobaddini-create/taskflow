package com.taskflow.core.utils

import android.util.Patterns
import com.taskflow.domain.model.AttachmentType

fun isValidUrl(value: String): Boolean = Patterns.WEB_URL.matcher(value).matches() && (value.startsWith("http://") || value.startsWith("https://"))

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
