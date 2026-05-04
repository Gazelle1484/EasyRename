package com.example.easyrename.ui.matching

import com.example.easyrename.model.AppError
import com.example.easyrename.model.RenameCandidate
import com.example.easyrename.model.RenameResult
import com.example.easyrename.model.RenameTargetFile

data class RenameMatchingUiState(
    val targetFiles: List<RenameTargetFile> = emptyList(),
    val renameCandidates: List<RenameCandidate> = emptyList(),
    val selectedTargetFileId: String? = null,
    val selectedCandidateId: String? = null,
    val canExecuteRename: Boolean = false,
    val isExecuting: Boolean = false,
    val isAutoNumberingEnabled: Boolean = true,
    val selectedAutoNumber: Int? = null,
    val selectedPreviewText: String? = null,
    val lastResult: RenameResult? = null,
    val error: AppError? = null,
)
