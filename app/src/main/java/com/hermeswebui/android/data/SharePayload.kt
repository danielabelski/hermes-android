package com.hermeswebui.android.data

import android.net.Uri

data class SharePayload(
    val sharedText: String? = null,
    val fileUris: List<Uri> = emptyList()
)
