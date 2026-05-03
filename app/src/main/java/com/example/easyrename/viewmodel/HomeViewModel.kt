package com.example.easyrename.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import com.example.easyrename.domain.usecase.GenerateRenameCandidateUseCase
import com.example.easyrename.domain.usecase.LoadDirectoryFilesUseCase
import com.example.easyrename.domain.usecase.LoadRenameRulesFromCsvUseCase
import com.example.easyrename.domain.usecase.TakePersistablePermissionUseCase
import com.example.easyrename.model.AppError
import com.example.easyrename.model.RenameMode
import com.example.easyrename.ui.home.HomeUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class HomeViewModel(
    private val loadDirectoryFilesUseCase: LoadDirectoryFilesUseCase,
    private val loadRenameRulesFromCsvUseCase: LoadRenameRulesFromCsvUseCase,
    private val generateRenameCandidateUseCase: GenerateRenameCandidateUseCase,
    private val takePersistablePermissionUseCase: TakePersistablePermissionUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    fun onDirectorySelected(uri: Uri) {
        _uiState.update { state ->
            state.copy(isLoading = true, error = null)
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

    fun onCsvSelected(uri: Uri) {
        _uiState.update { state ->
            state.copy(isLoading = true, error = null)
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

    fun onRenameModeSelected(mode: RenameMode) {
        _uiState.update { state ->
            state.copy(renameMode = mode)
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
}
