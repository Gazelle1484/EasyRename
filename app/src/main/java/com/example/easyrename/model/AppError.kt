package com.example.easyrename.model

sealed class AppError(
    val type: ErrorType,
    val message: String? = null,
    val cause: Throwable? = null,
) {
    data object CsvReadFailed : AppError(ErrorType.CsvReadFailed)
    data object DirectoryReadFailed : AppError(ErrorType.DirectoryReadFailed)
    data object PermissionDenied : AppError(ErrorType.PermissionDenied)
    data object InvalidFileName : AppError(ErrorType.InvalidFileName)
    data object FileAlreadyExists : AppError(ErrorType.FileAlreadyExists)
    data object UnsupportedOperation : AppError(ErrorType.UnsupportedOperation)
    data object RenameFailed : AppError(ErrorType.RenameFailed)
    data class Unknown(
        val detailMessage: String,
        val throwable: Throwable? = null,
    ) : AppError(ErrorType.Unknown, detailMessage, throwable)
}

enum class ErrorType {
    CsvReadFailed,
    DirectoryReadFailed,
    PermissionDenied,
    InvalidFileName,
    FileAlreadyExists,
    UnsupportedOperation,
    RenameFailed,
    Unknown,
}
