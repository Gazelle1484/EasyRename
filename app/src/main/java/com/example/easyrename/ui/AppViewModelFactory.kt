package com.example.easyrename.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.easyrename.data.csv.CsvRuleParser
import com.example.easyrename.data.preferences.LastUsedSetStore
import com.example.easyrename.data.repository.StorageRepositoryImpl
import com.example.easyrename.data.saf.SafDocumentDataSource
import com.example.easyrename.domain.history.RenameHistoryManager
import com.example.easyrename.domain.usecase.ExecuteRenameUseCase
import com.example.easyrename.domain.usecase.GenerateRenameCandidateUseCase
import com.example.easyrename.domain.usecase.LoadDirectoryFilesUseCase
import com.example.easyrename.domain.usecase.LoadRenameRulesFromCsvUseCase
import com.example.easyrename.domain.usecase.ResolveRenameNameUseCase
import com.example.easyrename.domain.usecase.TakePersistablePermissionUseCase
import com.example.easyrename.domain.usecase.UndoRenameUseCase
import com.example.easyrename.domain.usecase.ValidateRenameUseCase
import com.example.easyrename.model.RenameMode
import com.example.easyrename.viewmodel.HomeViewModel
import com.example.easyrename.viewmodel.RenameMatchingViewModel

class AppViewModelFactory(
    context: Context,
    private val homeViewModelProvider: (() -> HomeViewModel)? = null,
) : ViewModelProvider.Factory {

    private val applicationContext = context.applicationContext

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val dataSource = SafDocumentDataSource(applicationContext)
        val repository = StorageRepositoryImpl(dataSource)
        val validateRenameUseCase = ValidateRenameUseCase()

        return when {
            modelClass.isAssignableFrom(HomeViewModel::class.java) -> HomeViewModel(
                loadDirectoryFilesUseCase = LoadDirectoryFilesUseCase(repository),
                loadRenameRulesFromCsvUseCase = LoadRenameRulesFromCsvUseCase(
                    storageRepository = repository,
                    csvRuleParser = CsvRuleParser(),
                ),
                generateRenameCandidateUseCase = GenerateRenameCandidateUseCase(),
                takePersistablePermissionUseCase = TakePersistablePermissionUseCase(repository),
                lastUsedSetStore = LastUsedSetStore(applicationContext),
            ) as T

            modelClass.isAssignableFrom(RenameMatchingViewModel::class.java) -> {
                val homeState = homeViewModelProvider?.invoke()?.uiState?.value
                RenameMatchingViewModel(
                    resolveRenameNameUseCase = ResolveRenameNameUseCase(),
                    executeRenameUseCase = ExecuteRenameUseCase(
                        storageRepository = repository,
                        validateRenameUseCase = validateRenameUseCase,
                    ),
                    undoRenameUseCase = UndoRenameUseCase(repository),
                    directoryUri = homeState?.selectedDirectoryUri,
                    renameMode = homeState?.renameMode ?: RenameMode.Prefix,
                    initialTargetFiles = homeState?.targetFiles.orEmpty(),
                    initialRenameCandidates = homeState?.renameCandidates.orEmpty(),
                    renameHistoryManager = renameHistoryManager,
                ) as T
            }

            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }

    private companion object {
        val renameHistoryManager = RenameHistoryManager()
    }
}
