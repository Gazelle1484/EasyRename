package com.example.easyrename.domain.usecase

import android.net.Uri
import android.os.SystemClock
import android.util.Log
import com.example.easyrename.data.repository.StorageRepository
import com.example.easyrename.domain.history.RenameHistoryRecord
import com.example.easyrename.model.RenameErrorType
import com.example.easyrename.model.RenameResult

class UndoRenameUseCase(
    private val storageRepository: StorageRepository,
) {

    operator fun invoke(record: RenameHistoryRecord): RenameResult {
        val start = SystemClock.elapsedRealtime()
        val directoryUri = record.directoryUri?.let(Uri::parse)
        val sourceUri = Uri.parse(record.afterUri)
        val targetName = record.beforeName

        Log.d(
            TAG_HISTORY,
            "undo sourceName=${record.afterName} targetName=$targetName",
        )
        Log.d(
            TAG_HISTORY,
            "undo sourceUri=$sourceUri targetUriName=$targetName",
        )

        if (directoryUri == null) {
            Log.d(
                TAG_HISTORY,
                "undo failed sourceName=${record.afterName} targetName=$targetName errorType=${RenameErrorType.FileNotFound} errorMessage=Directory URI is missing.",
            )
            return RenameResult(
                beforeName = record.afterName,
                afterName = targetName,
                success = false,
                errorMessage = "FileNotFound: Directory URI is missing.",
                errorType = RenameErrorType.FileNotFound,
                sourceFileId = record.sourceFileIdAfter ?: record.afterUri,
            )
        }

        if (storageRepository.existsInSameDirectory(directoryUri, targetName)) {
            Log.d(
                TAG_HISTORY,
                "undo failed sourceName=${record.afterName} targetName=$targetName errorType=${RenameErrorType.FileAlreadyExists} errorMessage=A file with the same name already exists.",
            )
            return RenameResult(
                beforeName = record.afterName,
                afterName = targetName,
                success = false,
                errorMessage = "A file with the same name already exists.",
                errorType = RenameErrorType.FileAlreadyExists,
                sourceFileId = record.sourceFileIdAfter ?: record.afterUri,
            )
        }

        val result = storageRepository.renameFile(
            directoryUri = directoryUri,
            fileUri = sourceUri,
            newName = targetName,
            expectedBeforeName = record.afterName,
        ).copy(sourceFileId = record.sourceFileIdAfter ?: record.afterUri)

        Log.d(
            TAG_HISTORY,
            "undo result success=${result.success} sourceName=${record.afterName} targetName=$targetName resultAfterUri=${result.afterUri} errorType=${result.errorType} elapsedMs=${SystemClock.elapsedRealtime() - start}",
        )
        return result
    }

    private companion object {
        const val TAG_HISTORY = "EasyRenameHistory"
    }
}
