package com.example.easyrename.model

import android.net.Uri

data class RenameTargetFile(
    val id: String,
    val displayName: String,
    val uri: Uri,
    val size: Long?,
    val lastModified: Long?,
    val isSelected: Boolean = false,
    val isRenamed: Boolean = false,
)
