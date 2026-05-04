package com.example.easyrename.viewmodel

import android.net.Uri
import android.os.SystemClock
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class RenameMatchingViewModel(
    private val resolveRenameNameUseCase: ResolveRenameNameUseCase,
    private val executeRenameUseCase: ExecuteRenameUseCase,
    private val directoryUri: Uri?,
    private val renameMode: RenameMode,
    initialTargetFiles: List<RenameTargetFile> = emptyList(),
    initialRenameCandidates: List<RenameCandidate> = emptyList(),
) : ViewModel() {

    private val autoNumberCounters: MutableMap<String, Int> = mutableMapOf()
    private val _uiState = MutableStateFlow(
        RenameMatchingUiState(
            targetFiles = initialTargetFiles.sortedBy { it.displayName.lowercase() },
            renameCandidates = initialRenameCandidates.sortedBy { it.displayName.lowercase() },
        ),
    )
    val uiState: StateFlow<RenameMatchingUiState> = _uiState.asStateFlow()

    fun toggleAutoNumbering() {
        if (_uiState.value.isExecuting) return

        _uiState.update { state ->
            val enabled = !state.isAutoNumberingEnabled
            Log.d(LOG_TAG, "RenameMatchingViewModel.toggleAutoNumbering enabled=$enabled")
            val updatedState = state.copy(isAutoNumberingEnabled = enabled, error = null)
            updatedState.copy(selectedPreviewText = buildSelectedPreviewText(updatedState))
        }
    }

    fun selectTargetFile(fileId: String) {
        if (_uiState.value.isExecuting) return

        _uiState.update { state ->
            val updatedFiles = state.targetFiles.map { file ->
                file.copy(isSelected = file.id == fileId)
            }
            val selectedTargetFileId = updatedFiles.firstOrNull { it.isSelected }?.id

            val updatedState = state.copy(
                targetFiles = updatedFiles,
                selectedTargetFileId = selectedTargetFileId,
                canExecuteRename = canExecuteRename(
                    selectedTargetFileId = selectedTargetFileId,
                    selectedCandidateId = state.selectedCandidateId,
                ),
                error = null,
            )
            updatedState.copy(selectedPreviewText = buildSelectedPreviewText(updatedState))
        }
    }

    fun selectRenameCandidate(candidateId: String) {
        if (_uiState.value.isExecuting) return

        _uiState.update { state ->
            val updatedCandidates = state.renameCandidates.map { candidate ->
                candidate.copy(isSelected = candidate.id == candidateId)
            }
            val selectedCandidateId = updatedCandidates.firstOrNull { it.isSelected }?.id

            val updatedState = state.copy(
                renameCandidates = updatedCandidates,
                selectedCandidateId = selectedCandidateId,
                canExecuteRename = canExecuteRename(
                    selectedTargetFileId = state.selectedTargetFileId,
                    selectedCandidateId = selectedCandidateId,
                ),
                error = null,
            )
            updatedState.copy(selectedPreviewText = buildSelectedPreviewText(updatedState))
        }
    }

    fun executeSelectedRename() {
        val totalStart = SystemClock.elapsedRealtime()
        Log.d(TAG_PERF, "viewModel rename start renameMode=$renameMode")
        if (_uiState.value.isExecuting) {
            Log.d(TAG_PERF, "viewModel rename ignored because already executing")
            return
        }

        viewModelScope.launch {
            Log.d(TAG_PERF, "viewModel coroutine start renameMode=$renameMode")
            _uiState.update { state ->
                state.copy(isExecuting = true, error = null)
            }

            val currentState = _uiState.value
            val selectedFile = currentState.targetFiles.firstOrNull { it.id == currentState.selectedTargetFileId }
            val selectedCandidate = currentState.renameCandidates.firstOrNull { it.id == currentState.selectedCandidateId }
            val selectedDirectoryUri = directoryUri
            val isAutoNumberingEnabled = currentState.isAutoNumberingEnabled

            if (selectedFile == null || selectedCandidate == null || selectedDirectoryUri == null) {
                Log.e(LOG_TAG, "RenameMatchingViewModel.executeSelectedRename missingSelection directoryUri=$selectedDirectoryUri")
                _uiState.update { state ->
                    state.copy(
                        isExecuting = false,
                        canExecuteRename = false,
                        error = AppError.Unknown("Target file, rename candidate, or directory is not selected."),
                    )
                }
                return@launch
            }

            val autoNumberKey = selectedCandidate.rawPattern
            val autoNumber = if (isAutoNumberingEnabled) {
                autoNumberCounters[autoNumberKey] ?: INITIAL_AUTO_NUMBER
            } else {
                null
            }
            val ioStart = SystemClock.elapsedRealtime()
            runCatching {
                Log.d(TAG_PERF, "viewModel withContext(IO) start")
                withContext(Dispatchers.IO) {
                    val resolvedNewName = resolveRenameNameUseCase(
                        sourceFile = selectedFile,
                        candidate = selectedCandidate,
                        renameMode = renameMode,
                        autoNumber = autoNumber,
                    )
                    Log.d(
                        LOG_TAG,
                        "RenameMatchingViewModel.executeSelectedRename isAutoNumberingEnabled=$isAutoNumberingEnabled selectedRenameMode=$renameMode directoryUri=$selectedDirectoryUri fileUri=${selectedFile.uri} sourceFile.displayName=${selectedFile.displayName} candidate.rawPattern=${selectedCandidate.rawPattern} candidate.displayName=${selectedCandidate.displayName} autoNumber=$autoNumber resolvedNewName=$resolvedNewName",
                    )
                    val renamePair = RenamePair(
                        sourceFile = selectedFile,
                        renameCandidate = selectedCandidate,
                        resolvedNewName = resolvedNewName,
                        directoryUri = selectedDirectoryUri,
                    )

                    executeRenameUseCase(renamePair)
                }
            }.onSuccess { result ->
                Log.d(TAG_PERF, "viewModel withContext(IO) end elapsedMs=${SystemClock.elapsedRealtime() - ioStart}")
                Log.d(
                    LOG_TAG,
                    "RenameMatchingViewModel.renameResult success=${result.success} path=${result.renamePath} sourceFileId=${result.sourceFileId} beforeName=${result.beforeName} afterName=${result.afterName} afterUri=${result.afterUri} errorType=${result.errorType} errorMessage=${result.errorMessage}",
                )
                if (result.success && autoNumber != null) {
                    val counterAfter = autoNumber + 1
                    autoNumberCounters[autoNumberKey] = counterAfter
                    Log.d(
                        LOG_TAG,
                        "RenameMatchingViewModel.autoNumberCounter success=true candidate.rawPattern=$autoNumberKey counterBefore=$autoNumber counterAfter=$counterAfter",
                    )
                } else if (autoNumber != null) {
                    Log.d(
                        LOG_TAG,
                        "RenameMatchingViewModel.autoNumberCounter success=false candidate.rawPattern=$autoNumberKey counterBefore=$autoNumber counterAfter=$autoNumber",
                    )
                }
                val stateUpdateStart = SystemClock.elapsedRealtime()
                refreshAfterRename(result)
                Log.d(TAG_PERF, "viewModel state update after IO elapsedMs=${SystemClock.elapsedRealtime() - stateUpdateStart}")
                Log.d(
                    TAG_PERF,
                    "rename total elapsedMs=${SystemClock.elapsedRealtime() - totalStart} path=${result.renamePath} success=${result.success} errorType=${result.errorType} beforeName=${result.beforeName} afterName=${result.afterName} sourceFileId=${result.sourceFileId} afterUri=${result.afterUri}",
                )
            }.onFailure { throwable ->
                Log.d(TAG_PERF, "viewModel withContext(IO) end elapsedMs=${SystemClock.elapsedRealtime() - ioStart} failure=${throwable::class.java.simpleName}")
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
    }

    fun refreshAfterRename(result: RenameResult? = _uiState.value.lastResult) {
        val stateUpdateStart = SystemClock.elapsedRealtime()
        Log.d(
            TAG_PERF,
            "matching state update start success=${result?.success} path=${result?.renamePath} sourceFileId=${result?.sourceFileId} beforeName=${result?.beforeName} afterName=${result?.afterName} afterUri=${result?.afterUri}",
        )
        _uiState.update { state ->
            if (result == null) {
                return@update state.copy(isExecuting = false)
            }

            if (!result.success) {
                return@update state.copy(
                    isExecuting = false,
                    lastResult = result,
                    selectedPreviewText = null,
                    error = toAppError(result),
                )
            }

            val updatedFiles = state.targetFiles.map { file ->
                if (file.id == state.selectedTargetFileId) {
                    val updatedUri = result.afterUri ?: file.uri
                    val updatedFile = file.copy(
                        id = updatedUri.toString(),
                        displayName = result.afterName,
                        uri = updatedUri,
                        isSelected = false,
                        isRenamed = true,
                    )
                    Log.d(
                        LOG_TAG,
                        "RenameMatchingViewModel.updateTargetFile sourceFileId=${result.sourceFileId} beforeName=${result.beforeName} afterName=${result.afterName} afterUri=${result.afterUri} updatedUri=${updatedFile.uri}",
                    )
                    updatedFile
                } else {
                    file.copy(isSelected = false)
                }
            }.sortedBy { it.displayName.lowercase() }
            val updatedCandidates = state.renameCandidates.map { candidate ->
                if (candidate.id == state.selectedCandidateId) {
                    candidate.copy(
                        isSelected = false,
                        isUsed = if (state.isAutoNumberingEnabled) candidate.isUsed else true,
                    )
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
                selectedPreviewText = null,
                isExecuting = false,
                lastResult = result,
                error = null,
            )
        }
        Log.d(
            TAG_PERF,
            "matching state update end elapsedMs=${SystemClock.elapsedRealtime() - stateUpdateStart} path=${result?.renamePath} success=${result?.success} errorType=${result?.errorType} sourceFileId=${result?.sourceFileId} afterUri=${result?.afterUri}",
        )
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

    private fun buildSelectedPreviewText(state: RenameMatchingUiState): String? {
        val selectedFile = state.targetFiles.firstOrNull { it.id == state.selectedTargetFileId }
        val selectedCandidate = state.renameCandidates.firstOrNull { it.id == state.selectedCandidateId }
        if (selectedFile == null || selectedCandidate == null) return null

        val previewAutoNumber = if (state.isAutoNumberingEnabled) {
            autoNumberCounters[selectedCandidate.rawPattern] ?: INITIAL_AUTO_NUMBER
        } else {
            null
        }
        val resolvedNewName = resolveRenameNameUseCase(
            sourceFile = selectedFile,
            candidate = selectedCandidate,
            renameMode = renameMode,
            autoNumber = previewAutoNumber,
        )
        val previewText = "選択中：${selectedFile.displayName} -> $resolvedNewName"
        Log.d(
            LOG_TAG,
            "RenameMatchingViewModel.preview selectedTargetFile.displayName=${selectedFile.displayName} selectedCandidate.displayName=${selectedCandidate.displayName} selectedPreviewText=$previewText isAutoNumberingEnabled=${state.isAutoNumberingEnabled} previewAutoNumber=$previewAutoNumber displayNamePreserveCase=true",
        )
        return previewText
    }

    private companion object {
        const val LOG_TAG = "EasyRename"
        const val TAG_PERF = "EasyRenamePerf"
        const val INITIAL_AUTO_NUMBER = 1
    }
}
