package com.example.easyrename.model

import android.net.Uri

data class RenameResult(
    val beforeName: String,
    val afterName: String,
    val success: Boolean,
    val errorMessage: String? = null,
    val errorType: RenameErrorType? = null,
    val afterUri: Uri? = null,
    val sourceFileId: String? = null,
)

enum class RenameErrorType {
    InvalidFileName,
    FileAlreadyExists,
    PermissionDenied,
    UnsupportedOperation,
    RenameFailed,
    FileNotFound,
    Unknown,
}
