package com.example.easyrename.viewmodel

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import com.example.easyrename.domain.usecase.ExecuteRenameUseCase
import com.example.easyrename.domain.usecase.ResolveRenameNameUseCase
import com.example.easyrename.model.AppError
import com.example.easyrename.model.RenameCandidate
import com.example.easyrename.model.RenameErrorType
import com.example.easyrename.model.RenameMode
import com.example.easyrename.model.RenamePair
import com.example.easyrename.model.RenameResult
import com.example.easyrename.model.RenameTargetFile
import com.example.easyrename.ui.matching.RenameMatchingUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class RenameMatchingViewModel(
    private val resolveRenameNameUseCase: ResolveRenameNameUseCase,
    private val executeRenameUseCase: ExecuteRenameUseCase,
    private val directoryUri: Uri?,
    private val renameMode: RenameMode,
    initialTargetFiles: List<RenameTargetFile> = emptyList(),
    initialRenameCandidates: List<RenameCandidate> = emptyList(),
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        RenameMatchingUiState(
            targetFiles = initialTargetFiles.sortedBy { it.displayName.lowercase() },
            renameCandidates = initialRenameCandidates.sortedBy { it.displayName.lowercase() },
        ),
    )
    val uiState: StateFlow<RenameMatchingUiState> = _uiState.asStateFlow()

    fun selectTargetFile(fileId: String) {
        _uiState.update { state ->
            val updatedFiles = state.targetFiles.map { file ->
                file.copy(isSelected = file.id == fileId)
            }
            val selectedTargetFileId = updatedFiles.firstOrNull { it.isSelected }?.id

            state.copy(
                targetFiles = updatedFiles,
                selectedTargetFileId = selectedTargetFileId,
                canExecuteRename = canExecuteRename(
                    selectedTargetFileId = selectedTargetFileId,
                    selectedCandidateId = state.selectedCandidateId,
                ),
                error = null,
            )
        }
    }

    fun selectRenameCandidate(candidateId: String) {
        _uiState.update { state ->
            val updatedCandidates = state.renameCandidates.map { candidate ->
                candidate.copy(isSelected = candidate.id == candidateId)
            }
            val selectedCandidateId = updatedCandidates.firstOrNull { it.isSelected }?.id

            state.copy(
                renameCandidates = updatedCandidates,
                selectedCandidateId = selectedCandidateId,
                canExecuteRename = canExecuteRename(
                    selectedTargetFileId = state.selectedTargetFileId,
                    selectedCandidateId = selectedCandidateId,
                ),
                error = null,
            )
        }
    }

    fun executeSelectedRename() {
        _uiState.update { state ->
            state.copy(isExecuting = true, error = null)
        }

        val currentState = _uiState.value
        val selectedFile = currentState.targetFiles.firstOrNull { it.id == currentState.selectedTargetFileId }
        val selectedCandidate = currentState.renameCandidates.firstOrNull { it.id == currentState.selectedCandidateId }
        val selectedDirectoryUri = directoryUri

        if (selectedFile == null || selectedCandidate == null || selectedDirectoryUri == null) {
            Log.e(LOG_TAG, "RenameMatchingViewModel.executeSelectedRename missingSelection directoryUri=$selectedDirectoryUri")
            _uiState.update { state ->
                state.copy(
                    isExecuting = false,
                    canExecuteRename = false,
                    error = AppError.Unknown("Target file, rename candidate, or directory is not selected."),
                )
            }
            return
        }

        runCatching {
            val resolvedNewName = resolveRenameNameUseCase(selectedFile, selectedCandidate, renameMode)
            Log.d(
                LOG_TAG,
                "RenameMatchingViewModel.executeSelectedRename selectedRenameMode=$renameMode directoryUri=$selectedDirectoryUri fileUri=${selectedFile.uri} sourceFile.displayName=${selectedFile.displayName} candidate.rawPattern=${selectedCandidate.rawPattern} candidate.displayName=${selectedCandidate.displayName} resolvedNewName=$resolvedNewName",
            )
            val renamePair = RenamePair(
                sourceFile = selectedFile,
                renameCandidate = selectedCandidate,
                resolvedNewName = resolvedNewName,
                directoryUri = selectedDirectoryUri,
            )

            executeRenameUseCase(renamePair)
        }.onSuccess { result ->
            Log.d(
                LOG_TAG,
                "RenameMatchingViewModel.renameResult success=${result.success} beforeName=${result.beforeName} afterName=${result.afterName} errorType=${result.errorType} errorMessage=${result.errorMessage}",
            )
            refreshAfterRename(result)
        }.onFailure { throwable ->
            Log.e(LOG_TAG, "RenameMatchingViewModel.executeSelectedRename exceptionClass=${throwable::class.java.simpleName} message=${throwable.message}", throwable)
            _uiState.update { state ->
                state.copy(
                    isExecuting = false,
                    error = AppError.Unknown(
                        detailMessage = throwable.message ?: "Failed to execute rename.",
                        throwable = throwable,
                    ),
                )
            }
        }
    }

    fun refreshAfterRename(result: RenameResult? = _uiState.value.lastResult) {
        _uiState.update { state ->
            if (result == null) {
                return@update state.copy(isExecuting = false)
            }

            if (!result.success) {
                return@update state.copy(
                    isExecuting = false,
                    lastResult = result,
                    error = toAppError(result),
                )
            }

            val updatedFiles = state.targetFiles.map { file ->
                if (file.id == state.selectedTargetFileId) {
                    file.copy(isSelected = false, isRenamed = true)
                } else {
                    file.copy(isSelected = false)
                }
            }
            val updatedCandidates = state.renameCandidates.map { candidate ->
                if (candidate.id == state.selectedCandidateId) {
                    candidate.copy(isSelected = false, isUsed = true)
                } else {
                    candidate.copy(isSelected = false)
                }
            }

            state.copy(
                targetFiles = updatedFiles,
                renameCandidates = updatedCandidates,
                selectedTargetFileId = null,
                selectedCandidateId = null,
                canExecuteRename = false,
                isExecuting = false,
                lastResult = result,
                error = null,
            )
        }
    }

    private fun toAppError(result: RenameResult): AppError {
        return when (result.errorType) {
            RenameErrorType.InvalidFileName -> AppError.InvalidFileName
            RenameErrorType.FileAlreadyExists -> AppError.FileAlreadyExists
            RenameErrorType.PermissionDenied -> AppError.PermissionDenied
            RenameErrorType.UnsupportedOperation -> AppError.UnsupportedOperation
            RenameErrorType.FileNotFound -> AppError.RenameFailed
            RenameErrorType.RenameFailed -> AppError.RenameFailed
            RenameErrorType.Unknown -> AppError.Unknown(result.errorMessage ?: "不明なエラーが発生しました。")
            null -> toAppError(result.errorMessage)
        }
    }

    private fun toAppError(errorMessage: String?): AppError {
        return when {
            errorMessage?.contains("UnsupportedOperationException", ignoreCase = true) == true -> AppError.UnsupportedOperation
            errorMessage?.contains("not supported", ignoreCase = true) == true -> AppError.UnsupportedOperation
            errorMessage?.contains("Invalid file name", ignoreCase = true) == true -> AppError.InvalidFileName
            errorMessage?.contains("same name", ignoreCase = true) == true -> AppError.FileAlreadyExists
            errorMessage?.contains("already exists", ignoreCase = true) == true -> AppError.FileAlreadyExists
            else -> AppError.RenameFailed
        }
    }

    private fun canExecuteRename(
        selectedTargetFileId: String?,
        selectedCandidateId: String?,
    ): Boolean {
        return selectedTargetFileId != null && selectedCandidateId != null
    }

    private companion object {
        const val LOG_TAG = "EasyRename"
    }
}
