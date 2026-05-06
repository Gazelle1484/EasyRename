package com.example.easyrename.viewmodel

import android.net.Uri
import android.os.SystemClock
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.easyrename.data.preferences.LastUsedSetStore
import com.example.easyrename.domain.usecase.GenerateRenameCandidateUseCase
import com.example.easyrename.domain.usecase.LoadDirectoryFilesUseCase
import com.example.easyrename.domain.usecase.LoadRenameRulesFromCsvUseCase
import com.example.easyrename.domain.usecase.TakePersistablePermissionUseCase
import com.example.easyrename.model.AppError
import com.example.easyrename.model.LastUsedSet
import com.example.easyrename.model.RenameMode
import com.example.easyrename.model.RenameResult
import com.example.easyrename.ui.home.HomeUiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class HomeViewModel(
    private val loadDirectoryFilesUseCase: LoadDirectoryFilesUseCase,
    private val loadRenameRulesFromCsvUseCase: LoadRenameRulesFromCsvUseCase,
    private val generateRenameCandidateUseCase: GenerateRenameCandidateUseCase,
    private val takePersistablePermissionUseCase: TakePersistablePermissionUseCase,
    private val lastUsedSetStore: LastUsedSetStore,
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        HomeUiState().let { state ->
            val lastUsedSet = lastUsedSetStore.load()
            state.copy(
                lastUsedSet = lastUsedSet,
                isLastUsedSetAvailable = lastUsedSet != null,
            )
        },
    )
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    fun onDirectorySelected(uri: Uri) {
        _uiState.update { state ->
            state.copy(isLoading = true, hasMatchingPreparationFailed = false, error = null)
        }

        runCatching {
            takePersistablePermissionUseCase.forDirectory(uri)
            loadSortedDirectoryFiles(uri)
        }.onSuccess { files ->
            _uiState.update { state ->
                state.copy(
                    selectedDirectoryUri = uri,
                    selectedDirectoryName = resolveDisplayName(uri),
                    targetFiles = files,
                    targetFileCount = files.size,
                    isReadyToStartMatching = state.selectedCsvFileName != null,
                    isLoading = false,
                    hasMatchingPreparationFailed = false,
                    isStartMatchingActionArmed = state.selectedCsvUri != null,
                    error = null,
                )
            }
        }.onFailure { throwable ->
            _uiState.update { state ->
                state.copy(
                    isLoading = false,
                    error = AppError.Unknown(
                        detailMessage = throwable.message ?: "Failed to load directory files.",
                        throwable = throwable,
                    ),
                )
            }
        }
    }

    fun refreshSelectedDirectoryFiles() {
        val directoryUri = _uiState.value.selectedDirectoryUri ?: return

        runCatching {
            loadSortedDirectoryFiles(directoryUri)
        }.onSuccess { files ->
            _uiState.update { state ->
                state.copy(
                    targetFiles = files,
                    targetFileCount = files.size,
                    error = null,
                )
            }
        }.onFailure { throwable ->
            _uiState.update { state ->
                state.copy(
                    error = AppError.Unknown(
                        detailMessage = throwable.message ?: "Failed to refresh directory files.",
                        throwable = throwable,
                    ),
                )
            }
        }
    }

    fun applyRenameResult(result: RenameResult) {
        if (!result.success) return

        val start = SystemClock.elapsedRealtime()
        Log.d(
            TAG_PERF,
            "home applyRenameResult start sourceFileId=${result.sourceFileId} beforeName=${result.beforeName} afterName=${result.afterName} afterUri=${result.afterUri}",
        )
        _uiState.update { state ->
            val updatedFiles = state.targetFiles.map { file ->
                val shouldUpdate = file.id == result.sourceFileId ||
                    (result.sourceFileId == null && file.displayName == result.beforeName)

                if (shouldUpdate) {
                    val updatedUri = result.afterUri ?: file.uri
                    Log.d(
                        TAG_STATE_SYNC,
                        "home applyRenameResult sourceFileId=${result.sourceFileId}",
                    )
                    Log.d(
                        TAG_STATE_SYNC,
                        "home before displayName=${file.displayName} uri=${file.uri} id=${file.id}",
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
                        "home after displayName=${updatedFile.displayName} uri=${updatedFile.uri} id=${updatedFile.id} isRenamed=${updatedFile.isRenamed}",
                    )
                    Log.d(
                        LOG_TAG,
                        "HomeViewModel.applyRenameResult sourceFileId=${result.sourceFileId} beforeName=${result.beforeName} afterName=${result.afterName} afterUri=${result.afterUri} updatedUri=${updatedFile.uri}",
                    )
                    updatedFile
                } else {
                    file.copy(isSelected = false)
                }
            }.sortedBy { it.displayName.lowercase() }

            state.copy(targetFiles = updatedFiles)
        }
        Log.d(
            TAG_PERF,
            "home applyRenameResult end elapsedMs=${SystemClock.elapsedRealtime() - start} sourceFileId=${result.sourceFileId} afterUri=${result.afterUri}",
        )
    }

    fun applyUndoRenameResult(result: RenameResult) {
        if (!result.success) return

        val start = SystemClock.elapsedRealtime()
        Log.d(
            TAG_PERF,
            "home applyUndoRenameResult start sourceFileId=${result.sourceFileId} beforeName=${result.beforeName} afterName=${result.afterName} afterUri=${result.afterUri}",
        )
        _uiState.update { state ->
            val updatedFiles = state.targetFiles.map { file ->
                val shouldUpdate = file.id == result.sourceFileId ||
                    (result.sourceFileId == null && file.displayName == result.beforeName)

                if (shouldUpdate) {
                    val updatedUri = result.afterUri ?: file.uri
                    Log.d(
                        TAG_STATE_SYNC,
                        "home applyUndo before displayName=${file.displayName} uri=${file.uri} id=${file.id}",
                    )
                    val updatedFile = file.copy(
                        id = updatedUri.toString(),
                        displayName = result.afterName,
                        uri = updatedUri,
                        isSelected = false,
                        isRenamed = false,
                    )
                    Log.d(
                        TAG_STATE_SYNC,
                        "home applyUndo after displayName=${updatedFile.displayName} uri=${updatedFile.uri} id=${updatedFile.id} isRenamed=${updatedFile.isRenamed}",
                    )
                    Log.d(
                        LOG_TAG,
                        "HomeViewModel.applyUndoRenameResult sourceFileId=${result.sourceFileId} beforeName=${result.beforeName} afterName=${result.afterName} afterUri=${result.afterUri} updatedUri=${updatedFile.uri}",
                    )
                    updatedFile
                } else {
                    file.copy(isSelected = false)
                }
            }.sortedBy { it.displayName.lowercase() }

            state.copy(targetFiles = updatedFiles)
        }
        Log.d(
            TAG_PERF,
            "home applyUndoRenameResult end elapsedMs=${SystemClock.elapsedRealtime() - start} sourceFileId=${result.sourceFileId} afterUri=${result.afterUri}",
        )
    }

    fun onCsvSelected(uri: Uri) {
        _uiState.update { state ->
            state.copy(isLoading = true, hasMatchingPreparationFailed = false, error = null)
        }

        runCatching {
            takePersistablePermissionUseCase.forCsv(uri)
            val rules = loadRenameRulesFromCsvUseCase(uri)
            generateRenameCandidateUseCase(rules)
        }.onSuccess { candidates ->
            val sortedCandidates = candidates.sortedBy { it.displayName.lowercase() }
            _uiState.update { state ->
                state.copy(
                    selectedCsvUri = uri,
                    selectedCsvFileName = resolveDisplayName(uri),
                    renameCandidates = sortedCandidates,
                    renameCandidateCount = sortedCandidates.size,
                    isReadyToStartMatching = state.selectedDirectoryName != null,
                    isLoading = false,
                    hasMatchingPreparationFailed = false,
                    isStartMatchingActionArmed = state.selectedDirectoryUri != null,
                    error = null,
                )
            }
        }.onFailure { throwable ->
            _uiState.update { state ->
                state.copy(
                    isLoading = false,
                    error = AppError.Unknown(
                        detailMessage = throwable.message ?: "Failed to load CSV rules.",
                        throwable = throwable,
                    ),
                )
            }
        }
    }

    fun selectLastUsedSet() {
        val lastUsedSet = _uiState.value.lastUsedSet ?: return
        val directoryUri = runCatching { Uri.parse(lastUsedSet.directoryUriString) }.getOrNull()
        val csvUri = runCatching { Uri.parse(lastUsedSet.csvUriString) }.getOrNull()

        if (directoryUri == null || csvUri == null) {
            _uiState.update { state ->
                state.copy(
                    error = AppError.Unknown("Saved directory or CSV URI is invalid."),
                    isReadyToStartMatching = false,
                    hasMatchingPreparationFailed = true,
                )
            }
            return
        }

        Log.d(
            LOG_TAG,
            "HomeViewModel.selectLastUsedSet directoryUri=$directoryUri csvUri=$csvUri",
        )
        _uiState.update { state ->
            val updatedState = state.copy(
                selectedDirectoryUri = directoryUri,
                selectedCsvUri = csvUri,
                selectedDirectoryName = lastUsedSet.directoryDisplayName,
                selectedCsvFileName = lastUsedSet.csvDisplayName,
                targetFiles = emptyList(),
                renameCandidates = emptyList(),
                targetFileCount = 0,
                renameCandidateCount = 0,
                isReadyToStartMatching = true,
                hasMatchingPreparationFailed = false,
                isStartMatchingActionArmed = true,
                selectionRevision = state.selectionRevision + 1,
                error = null,
            )
            Log.d(
                LOG_TAG,
                "HomeViewModel.selectLastUsedSet updated selectedDirectoryUri=${updatedState.selectedDirectoryUri} selectedCsvUri=${updatedState.selectedCsvUri} selectedDirectoryName=${updatedState.selectedDirectoryName} selectedCsvFileName=${updatedState.selectedCsvFileName} isReadyToStartMatching=${updatedState.isReadyToStartMatching} isLoading=${updatedState.isLoading} targetFileCount=${updatedState.targetFileCount} renameCandidateCount=${updatedState.renameCandidateCount} selectionRevision=${updatedState.selectionRevision}",
            )
            updatedState
        }
    }

    fun prepareMatchingData(onPrepared: () -> Unit) {
        if (_uiState.value.isLoading) return

        val currentState = _uiState.value
        val directoryUri = currentState.selectedDirectoryUri
        val csvUri = currentState.selectedCsvUri
        val directoryName = currentState.selectedDirectoryName
        val csvName = currentState.selectedCsvFileName

        if (directoryUri == null || csvUri == null || directoryName == null || csvName == null) {
            _uiState.update { state ->
                state.copy(
                    isReadyToStartMatching = false,
                    hasMatchingPreparationFailed = true,
                    error = AppError.Unknown("Directory and CSV must be selected before matching."),
                )
            }
            return
        }

        viewModelScope.launch {
            Log.d(LOG_TAG, "HomeViewModel.prepareMatchingData start directoryUri=$directoryUri csvUri=$csvUri")
            _uiState.update { state ->
                state.copy(isLoading = true, hasMatchingPreparationFailed = false, error = null)
            }

            runCatching {
                withContext(Dispatchers.IO) {
                    takePersistablePermissionUseCase.forDirectory(directoryUri)
                    takePersistablePermissionUseCase.forCsv(csvUri)
                    val files = loadSortedDirectoryFiles(directoryUri)
                    Log.d(LOG_TAG, "HomeViewModel.prepareMatchingData loadDirectory success fileCount=${files.size}")
                    val rules = loadRenameRulesFromCsvUseCase(csvUri)
                    val candidates = generateRenameCandidateUseCase(rules).sortedBy { it.displayName.lowercase() }
                    Log.d(LOG_TAG, "HomeViewModel.prepareMatchingData loadCsv success candidateCount=${candidates.size}")
                    PreparedMatchingData(files, candidates)
                }
            }.onSuccess { preparedData ->
                val lastUsedSet = LastUsedSet(
                    directoryUriString = directoryUri.toString(),
                    directoryDisplayName = directoryName,
                    csvUriString = csvUri.toString(),
                    csvDisplayName = csvName,
                )
                lastUsedSetStore.save(lastUsedSet)
                _uiState.update { state ->
                    state.copy(
                        targetFiles = preparedData.targetFiles,
                        renameCandidates = preparedData.renameCandidates,
                        targetFileCount = preparedData.targetFiles.size,
                        renameCandidateCount = preparedData.renameCandidates.size,
                        lastUsedSet = lastUsedSet,
                        isLastUsedSetAvailable = true,
                        isReadyToStartMatching = true,
                        isLoading = false,
                        hasMatchingPreparationFailed = false,
                        isStartMatchingActionArmed = true,
                        error = null,
                    )
                }
                onPrepared()
            }.onFailure { throwable ->
                Log.e(LOG_TAG, "HomeViewModel.prepareMatchingData failed message=${throwable.message}", throwable)
                _uiState.update { state ->
                    state.copy(
                        isLoading = false,
                        hasMatchingPreparationFailed = true,
                        error = AppError.Unknown(
                            detailMessage = throwable.message ?: "Failed to prepare matching data.",
                            throwable = throwable,
                        ),
                    )
                }
            }
        }
    }

    fun onRenameModeSelected(mode: RenameMode) {
        _uiState.update { state ->
            state.copy(renameMode = mode)
        }
    }

    fun resetStartMatchingAction() {
        _uiState.update { state ->
            state.copy(
                isStartMatchingActionArmed = false,
                hasMatchingPreparationFailed = false,
            )
        }
    }

    fun clearError() {
        _uiState.update { state ->
            state.copy(error = null)
        }
    }

    private fun resolveDisplayName(uri: Uri): String {
        return uri.lastPathSegment?.substringAfterLast('/') ?: uri.toString()
    }

    private fun loadSortedDirectoryFiles(uri: Uri) =
        loadDirectoryFilesUseCase(uri).sortedBy { it.displayName.lowercase() }

    private data class PreparedMatchingData(
        val targetFiles: List<com.example.easyrename.model.RenameTargetFile>,
        val renameCandidates: List<com.example.easyrename.model.RenameCandidate>,
    )

    private companion object {
        const val LOG_TAG = "EasyRename"
        const val TAG_PERF = "EasyRenamePerf"
        const val TAG_STATE_SYNC = "EasyRenameStateSync"
    }
}
