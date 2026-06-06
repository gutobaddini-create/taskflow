package com.taskflow

import com.taskflow.core.utils.isAllowedAttachment
import com.taskflow.core.utils.isValidUrl
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ValidatorsTest {
    @Test
    fun acceptsAllowedFileUnderLimit() {
        assertTrue(isAllowedAttachment("boleto.pdf", 1_200_000))
    }

    @Test
    fun rejectsLargeFile() {
        assertFalse(isAllowedAttachment("contrato.pdf", 21L * 1024L * 1024L))
    }

    @Test
    fun rejectsUnsupportedExtension() {
        assertFalse(isAllowedAttachment("arquivo.exe", 1200))
    }

    @Test
    fun acceptsHttpAndHttpsUrls() {
        assertTrue(isValidUrl("https://taskflow.local/convite/abc"))
        assertTrue(isValidUrl("http://example.com/path"))
    }

    @Test
    fun rejectsUrlsWithoutWebScheme() {
        assertFalse(isValidUrl("taskflow.local/convite/abc"))
        assertFalse(isValidUrl("ftp://example.com/file.pdf"))
    }
}
