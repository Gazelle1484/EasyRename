package com.example.easyrename.model

data class RenameResult(
    val beforeName: String,
    val afterName: String,
    val success: Boolean,
    val errorMessage: String? = null,
    val errorType: RenameErrorType? = null,
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
