package com.example.easyrename.ui.home

import android.net.Uri
import com.example.easyrename.model.AppError
import com.example.easyrename.model.RenameCandidate
import com.example.easyrename.model.RenameTargetFile

data class HomeUiState(
    val selectedDirectoryUri: Uri? = null,
    val selectedCsvUri: Uri? = null,
    val selectedDirectoryName: String? = null,
    val selectedCsvFileName: String? = null,
    val targetFiles: List<RenameTargetFile> = emptyList(),
    val renameCandidates: List<RenameCandidate> = emptyList(),
    val targetFileCount: Int = 0,
    val renameCandidateCount: Int = 0,
    val isReadyToStartMatching: Boolean = false,
    val isLoading: Boolean = false,
    val error: AppError? = null,
)
