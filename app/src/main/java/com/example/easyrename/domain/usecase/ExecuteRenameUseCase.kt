package com.example.easyrename.domain.usecase

import android.os.SystemClock
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
        val start = SystemClock.elapsedRealtime()
        Log.d(
            TAG_PERF,
            "useCase start sourceFileId=${renamePair.sourceFile.id} beforeName=${renamePair.sourceFile.displayName} afterName=${renamePair.resolvedNewName}",
        )
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

        Log.d(TAG_PERF, "useCase duplicate precheck skipped")

        Log.d(
            LOG_TAG,
            "ExecuteRenameUseCase rename directoryUri=${renamePair.directoryUri} fileUri=${renamePair.sourceFile.uri} beforeName=${renamePair.sourceFile.displayName} afterName=${renamePair.resolvedNewName}",
        )
        val result = storageRepository.renameFile(
            directoryUri = renamePair.directoryUri,
            fileUri = renamePair.sourceFile.uri,
            newName = renamePair.resolvedNewName,
            expectedBeforeName = renamePair.sourceFile.displayName,
        )
        val resultWithSource = result.copy(sourceFileId = renamePair.sourceFile.id)
        Log.d(
            TAG_PERF,
            "useCase end elapsedMs=${SystemClock.elapsedRealtime() - start} path=${resultWithSource.renamePath} success=${resultWithSource.success} errorType=${resultWithSource.errorType} sourceFileId=${resultWithSource.sourceFileId} beforeName=${resultWithSource.beforeName} afterName=${resultWithSource.afterName} afterUri=${resultWithSource.afterUri}",
        )
        return resultWithSource
    }

    private companion object {
        const val LOG_TAG = "EasyRename"
        const val TAG_PERF = "EasyRenamePerf"
        const val ERROR_INVALID_FILE_NAME = "Invalid file name."
    }
}
