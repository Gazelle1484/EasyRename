package com.example.easyrename.viewmodel

import android.net.Uri
import android.os.SystemClock
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.easyrename.domain.history.RenameHistoryManager
import com.example.easyrename.domain.history.RenameHistoryRecord
import com.example.easyrename.domain.usecase.ExecuteRenameUseCase
import com.example.easyrename.domain.usecase.ResolveRenameNameUseCase
import com.example.easyrename.domain.usecase.UndoRenameUseCase
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
    private val undoRenameUseCase: UndoRenameUseCase,
    private val directoryUri: Uri?,
    private val renameMode: RenameMode,
    initialTargetFiles: List<RenameTargetFile> = emptyList(),
    initialRenameCandidates: List<RenameCandidate> = emptyList(),
    private val renameHistoryManager: RenameHistoryManager,
) : ViewModel() {

    private val autoNumberCounters: MutableMap<String, Int> = mutableMapOf()
    private val _uiState = MutableStateFlow(
        RenameMatchingUiState(
            targetFiles = initialTargetFiles.sortedBy { it.displayName.lowercase() },
            renameCandidates = initialRenameCandidates.sortedBy { it.displayName.lowercase() },
            canUndo = renameHistoryManager.getLatest() != null,
            renameHistoryCount = renameHistoryManager.size(),
        ),
    )
    val uiState: StateFlow<RenameMatchingUiState> = _uiState.asStateFlow()

    init {
        Log.d(
            TAG_HISTORY,
            "RenameMatchingViewModel initialized historySize=${renameHistoryManager.size()} canUndo=${renameHistoryManager.getLatest() != null}",
        )
    }

    fun toggleAutoNumbering() {
        if (_uiState.value.isExecuting) return

        _uiState.update { state ->
            val enabled = !state.isAutoNumberingEnabled
            Log.d(LOG_TAG, "RenameMatchingViewModel.toggleAutoNumbering enabled=$enabled")
            val updatedState = state.copy(isAutoNumberingEnabled = enabled, error = null)
            updatedState.withSelectedPreview()
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
            updatedState.withSelectedPreview()
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
            updatedState.withSelectedPreview()
        }
    }

    fun getNextAutoNumberForSelectedCandidate(): Int? {
        val state = _uiState.value
        if (!state.isAutoNumberingEnabled) return null

        val selectedCandidate = state.renameCandidates.firstOrNull { it.id == state.selectedCandidateId }
            ?: return null

        return autoNumberCounters[selectedCandidate.rawPattern] ?: INITIAL_AUTO_NUMBER
    }

    fun setNextAutoNumberForSelectedCandidate(number: Int) {
        if (number < INITIAL_AUTO_NUMBER) return

        _uiState.update { state ->
            if (!state.isAutoNumberingEnabled || state.isExecuting) return@update state

            val selectedCandidate = state.renameCandidates.firstOrNull { it.id == state.selectedCandidateId }
                ?: return@update state

            val currentAutoNumber = autoNumberCounters[selectedCandidate.rawPattern] ?: INITIAL_AUTO_NUMBER
            Log.d(
                LOG_TAG,
                "RenameMatchingViewModel.setNextAutoNumber currentAutoNumberBeforeDialog=$currentAutoNumber requestedAutoNumber=$number selectedCandidate.rawPattern=${selectedCandidate.rawPattern}",
            )
            autoNumberCounters[selectedCandidate.rawPattern] = number
            val updatedState = state.copy(error = null)
            val previewState = updatedState.withSelectedPreview()
            Log.d(
                LOG_TAG,
                "RenameMatchingViewModel.setNextAutoNumber selectedPreviewText=${previewState.selectedPreviewText} selectedAutoNumber=${previewState.selectedAutoNumber}",
            )
            previewState
        }
    }

    fun onUndoClicked() {
        if (_uiState.value.isExecuting || _uiState.value.isUndoExecuting) {
            Log.d(TAG_HISTORY, "undo ignored because rename or undo is already executing")
            return
        }

        val latest = renameHistoryManager.getLatest()
        if (latest == null) {
            Log.d(TAG_HISTORY, "undo clicked but history is empty")
            updateUndoState()
            return
        }

        Log.d(
            TAG_HISTORY,
            "undo start beforeName=${latest.beforeName} afterName=${latest.afterName} historySize=${renameHistoryManager.size()}",
        )
        Log.d(
            TAG_HISTORY,
            "undo start record.beforeName=${latest.beforeName} record.afterName=${latest.afterName}",
        )
        Log.d(
            TAG_HISTORY,
            "undo sourceUriFromHistoryAfterUri=${latest.afterUri}",
        )
        Log.d(
            TAG_HISTORY,
            "undo clicked latest beforeName=${latest.beforeName} afterName=${latest.afterName} beforeUri=${latest.beforeUri} afterUri=${latest.afterUri} renameMode=${latest.renameMode} autoNumber=${latest.autoNumber}",
        )

        viewModelScope.launch {
            _uiState.update { state ->
                state.copy(isUndoExecuting = true, error = null)
            }

            runCatching {
                withContext(Dispatchers.IO) {
                    undoRenameUseCase(latest)
                }
            }.onSuccess { result ->
                if (result.success) {
                    val removed = renameHistoryManager.removeLatest()
                    Log.d(
                        TAG_HISTORY,
                        "undo success restoredName=${result.afterName} resultAfterUri=${result.afterUri} historySize=${renameHistoryManager.size()} removedMatchesLatest=${removed?.id == latest.id}",
                    )
                    refreshAfterUndo(latest, result)
                } else {
                    Log.d(
                        TAG_HISTORY,
                        "undo failed sourceName=${latest.afterName} targetName=${latest.beforeName} errorType=${result.errorType} errorMessage=${result.errorMessage}",
                    )
                    _uiState.update { state ->
                        state.copy(
                            isUndoExecuting = false,
                            canUndo = renameHistoryManager.getLatest() != null,
                            renameHistoryCount = renameHistoryManager.size(),
                            lastUndoResult = result,
                            error = toAppError(result),
                        )
                    }
                    updateUndoState()
                }
            }.onFailure { throwable ->
                Log.e(LOG_TAG, "RenameMatchingViewModel.onUndoClicked exceptionClass=${throwable::class.java.simpleName} message=${throwable.message}", throwable)
                Log.d(
                    TAG_HISTORY,
                    "undo failed sourceName=${latest.afterName} targetName=${latest.beforeName} errorType=${RenameErrorType.Unknown} errorMessage=${throwable.message}",
                )
                _uiState.update { state ->
                    state.copy(
                        isUndoExecuting = false,
                        error = AppError.Unknown(
                            detailMessage = throwable.message ?: "Failed to undo rename.",
                            throwable = throwable,
                        ),
                    )
                }
                updateUndoState()
            }
        }
    }

    fun executeSelectedRename() {
        val totalStart = SystemClock.elapsedRealtime()
        Log.d(TAG_PERF, "viewModel rename start renameMode=$renameMode")
        Log.d(
            TAG_HISTORY,
            "executeSelectedRename requested historySizeBefore=${renameHistoryManager.size()}",
        )
        if (_uiState.value.isExecuting) {
            Log.d(TAG_PERF, "viewModel rename ignored because already executing")
            Log.d(TAG_HISTORY, "skip add because rename is already executing")
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
                Log.d(
                    TAG_HISTORY,
                    "skip add because selection is missing selectedFile=${selectedFile != null} selectedCandidate=${selectedCandidate != null} directoryUri=$selectedDirectoryUri",
                )
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
                    TAG_HISTORY,
                    "rename result received success=${result.success} beforeName=${result.beforeName} afterName=${result.afterName} afterUri=${result.afterUri} errorType=${result.errorType}",
                )
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
                if (result.success) {
                    addRenameHistory(
                        result = result,
                        selectedFile = selectedFile,
                        selectedCandidate = selectedCandidate,
                        autoNumber = autoNumber,
                    )
                } else {
                    Log.d(
                        TAG_HISTORY,
                        "skip add because rename failed beforeName=${result.beforeName} afterName=${result.afterName} errorType=${result.errorType}",
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
                Log.d(
                    TAG_HISTORY,
                    "skip add because rename threw exceptionClass=${throwable::class.java.simpleName} message=${throwable.message}",
                )
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
                    lastUndoResult = null,
                    selectedPreviewText = null,
                    canUndo = renameHistoryManager.getLatest() != null,
                    renameHistoryCount = renameHistoryManager.size(),
                    error = toAppError(result),
                )
            }

            val updatedFiles = state.targetFiles.map { file ->
                if (file.id == state.selectedTargetFileId) {
                    val updatedUri = result.afterUri ?: file.uri
                    Log.d(
                        TAG_STATE_SYNC,
                        "matching applyRenameResult sourceFileId=${result.sourceFileId}",
                    )
                    Log.d(
                        TAG_STATE_SYNC,
                        "matching before displayName=${file.displayName} uri=${file.uri} id=${file.id}",
                    )
                    val updatedFile = file.copy(
                        id = updatedUri.toString(),
                        displayName = result.afterName,
                        uri = updatedUri,
                        isSelected = false,
                        isRenamed = true,
                    )
                    Log.d(
                        TAG_STATE_SYNC,
                        "matching after displayName=${updatedFile.displayName} uri=${updatedFile.uri} id=${updatedFile.id} isRenamed=${updatedFile.isRenamed}",
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
                selectedAutoNumber = null,
                canUndo = renameHistoryManager.getLatest() != null,
                renameHistoryCount = renameHistoryManager.size(),
                isExecuting = false,
                lastResult = result,
                lastUndoResult = null,
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

    private fun refreshAfterUndo(record: RenameHistoryRecord, result: RenameResult) {
        val stateUpdateStart = SystemClock.elapsedRealtime()
        Log.d(
            TAG_PERF,
            "matching undo state update start success=${result.success} path=${result.renamePath} sourceFileId=${result.sourceFileId} beforeName=${result.beforeName} afterName=${result.afterName} afterUri=${result.afterUri}",
        )
        _uiState.update { state ->
            val updatedFiles = state.targetFiles.map { file ->
                val shouldUpdate = file.id == record.sourceFileIdAfter ||
                    file.id == record.afterUri ||
                    file.uri.toString() == record.afterUri ||
                    file.displayName == record.afterName

                if (shouldUpdate) {
                    val updatedUri = result.afterUri ?: Uri.parse(record.beforeUri)
                    Log.d(
                        TAG_STATE_SYNC,
                        "matching applyUndo before displayName=${file.displayName} uri=${file.uri} id=${file.id}",
                    )
                    val updatedFile = file.copy(
                        id = updatedUri.toString(),
                        displayName = record.beforeName,
                        uri = updatedUri,
                        isSelected = false,
                        isRenamed = false,
                    )
                    Log.d(
                        TAG_STATE_SYNC,
                        "matching applyUndo after displayName=${updatedFile.displayName} uri=${updatedFile.uri} id=${updatedFile.id} isRenamed=${updatedFile.isRenamed}",
                    )
                    Log.d(
                        LOG_TAG,
                        "RenameMatchingViewModel.undoUpdateTargetFile beforeName=${record.beforeName} afterName=${record.afterName} resultAfterUri=${result.afterUri} updatedUri=${updatedFile.uri}",
                    )
                    updatedFile
                } else {
                    file.copy(isSelected = false)
                }
            }.sortedBy { it.displayName.lowercase() }

            state.copy(
                targetFiles = updatedFiles,
                selectedTargetFileId = null,
                selectedCandidateId = null,
                canExecuteRename = false,
                selectedPreviewText = null,
                selectedAutoNumber = null,
                canUndo = renameHistoryManager.getLatest() != null,
                renameHistoryCount = renameHistoryManager.size(),
                isUndoExecuting = false,
                lastUndoResult = result,
                error = null,
            )
        }
        Log.d(
            TAG_PERF,
            "matching undo state update end elapsedMs=${SystemClock.elapsedRealtime() - stateUpdateStart} path=${result.renamePath} success=${result.success} errorType=${result.errorType} sourceFileId=${result.sourceFileId} afterUri=${result.afterUri}",
        )
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

    private fun RenameMatchingUiState.withSelectedPreview(): RenameMatchingUiState {
        return copy(
            selectedAutoNumber = getSelectedAutoNumber(this),
            selectedPreviewText = buildSelectedPreviewText(this),
        )
    }

    private fun addRenameHistory(
        result: RenameResult,
        selectedFile: RenameTargetFile,
        selectedCandidate: RenameCandidate,
        autoNumber: Int?,
    ) {
        val timestampMillis = System.currentTimeMillis()
        val afterUri = result.afterUri ?: selectedFile.uri
        Log.d(
            TAG_HISTORY,
            "history add afterUri source=${if (result.afterUri != null) "RenameResult.afterUri" else "selectedFile.uri"} afterUri=$afterUri",
        )
        Log.d(
            TAG_HISTORY,
            "history add beforeUri=${selectedFile.uri} afterUri=$afterUri renamePath=${result.renamePath}",
        )
        val record = RenameHistoryRecord(
            id = "$timestampMillis-${afterUri}",
            timestampMillis = timestampMillis,
            beforeName = result.beforeName,
            afterName = result.afterName,
            beforeUri = selectedFile.uri.toString(),
            afterUri = afterUri.toString(),
            directoryUri = directoryUri?.toString(),
            renameMode = renameMode,
            sourceFileIdBefore = selectedFile.id,
            sourceFileIdAfter = result.afterUri?.toString() ?: result.sourceFileId,
            candidateRawPattern = selectedCandidate.rawPattern,
            autoNumber = autoNumber,
        )
        renameHistoryManager.add(record)
        Log.d(
            TAG_HISTORY,
            "history add stored beforeUri=${record.beforeUri} afterUri=${record.afterUri} renameResultAfterUri=${result.afterUri} matchesRenameResult=${result.afterUri?.toString() == record.afterUri}",
        )
    }

    private fun updateUndoState() {
        _uiState.update { state ->
            state.copy(
                canUndo = renameHistoryManager.getLatest() != null,
                renameHistoryCount = renameHistoryManager.size(),
            )
        }
        Log.d(
            TAG_HISTORY,
            "undo state updated canUndo=${renameHistoryManager.getLatest() != null} historySize=${renameHistoryManager.size()}",
        )
    }

    private fun getSelectedAutoNumber(state: RenameMatchingUiState): Int? {
        if (!state.isAutoNumberingEnabled) return null

        val selectedCandidate = state.renameCandidates.firstOrNull { it.id == state.selectedCandidateId }
            ?: return null

        return autoNumberCounters[selectedCandidate.rawPattern] ?: INITIAL_AUTO_NUMBER
    }

    private companion object {
        const val LOG_TAG = "EasyRename"
        const val TAG_HISTORY = "EasyRenameHistory"
        const val TAG_PERF = "EasyRenamePerf"
        const val TAG_STATE_SYNC = "EasyRenameStateSync"
        const val INITIAL_AUTO_NUMBER = 1
    }
}
