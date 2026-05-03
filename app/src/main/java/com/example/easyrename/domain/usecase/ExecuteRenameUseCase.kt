package com.example.easyrename.domain.usecase

import android.util.Log
import com.example.easyrename.data.repository.StorageRepository
import com.example.easyrename.model.RenamePair
import com.example.easyrename.model.RenameErrorType
import com.example.easyrename.model.RenameResult

class ExecuteRenameUseCase(
    private val storageRepository: StorageRepository,
    private val validateRenameUseCase: ValidateRenameUseCase,
) {

    operator fun invoke(renamePair: RenamePair): RenameResult {
        if (!validateRenameUseCase(renamePair)) {
            Log.d(LOG_TAG, "ExecuteRenameUseCase invalidName beforeName=${renamePair.sourceFile.displayName} afterName=${renamePair.resolvedNewName}")
            return RenameResult(
                beforeName = renamePair.sourceFile.displayName,
                afterName = renamePair.resolvedNewName,
                success = false,
                errorMessage = ERROR_INVALID_FILE_NAME,
                errorType = RenameErrorType.InvalidFileName,
            )
        }

        if (storageRepository.existsInSameDirectory(renamePair.directoryUri, renamePair.resolvedNewName)) {
            Log.d(LOG_TAG, "ExecuteRenameUseCase fileAlreadyExists directoryUri=${renamePair.directoryUri} afterName=${renamePair.resolvedNewName}")
            return RenameResult(
                beforeName = renamePair.sourceFile.displayName,
                afterName = renamePair.resolvedNewName,
                success = false,
                errorMessage = ERROR_FILE_ALREADY_EXISTS,
                errorType = RenameErrorType.FileAlreadyExists,
            )
        }

        Log.d(
            LOG_TAG,
            "ExecuteRenameUseCase rename directoryUri=${renamePair.directoryUri} fileUri=${renamePair.sourceFile.uri} beforeName=${renamePair.sourceFile.displayName} afterName=${renamePair.resolvedNewName}",
        )
        return storageRepository.renameFile(
            directoryUri = renamePair.directoryUri,
            fileUri = renamePair.sourceFile.uri,
            newName = renamePair.resolvedNewName,
        )
    }

    private companion object {
        const val LOG_TAG = "EasyRename"
        const val ERROR_INVALID_FILE_NAME = "Invalid file name."
        const val ERROR_FILE_ALREADY_EXISTS = "A file with the same name already exists."
    }
}
