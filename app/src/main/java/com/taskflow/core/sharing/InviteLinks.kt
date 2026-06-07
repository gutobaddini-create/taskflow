package com.taskflow.core.sharing

import android.content.Intent

object InviteLinks {
    const val HostingHost = "gen-lang-client-0780081219.web.app"
    const val HostingBaseUrl = "https://$HostingHost"
    private const val InvitePath = "invite"

    fun urlForToken(token: String): String = "$HostingBaseUrl/$InvitePath/$token"

    fun tokenFromUrl(value: String?): String? {
        val raw = value?.trim()?.takeIf { it.isNotBlank() } ?: return null
        val token = when {
            raw.startsWith("$HostingBaseUrl/$InvitePath/") -> raw.substringAfter("$HostingBaseUrl/$InvitePath/").substringBefore("?").substringBefore("#")
            raw.startsWith("taskflow://invite/") -> raw.substringAfter("taskflow://invite/").substringBefore("?").substringBefore("#")
            else -> null
        }
        return token?.takeIf { it.isNotBlank() }
    }

    fun tokenFromIntent(intent: Intent): String? {
        val data = intent.data ?: return null
        val uri = data.toString()
        return tokenFromUrl(uri)
    }
}
