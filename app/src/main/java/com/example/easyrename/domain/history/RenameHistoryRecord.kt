package com.example.easyrename.domain.history

import com.example.easyrename.model.RenameMode

data class RenameHistoryRecord(
    val id: String,
    val timestampMillis: Long,
    val beforeName: String,
    val afterName: String,
    val beforeUri: String,
    val afterUri: String,
    val directoryUri: String?,
    val renameMode: RenameMode,
    val sourceFileIdBefore: String?,
    val sourceFileIdAfter: String?,
    val candidateRawPattern: String?,
    val autoNumber: Int?,
)
