package com.example.easyrename.data.repository

import android.net.Uri
import android.util.Log
import com.example.easyrename.data.saf.SafDocumentDataSource
import com.example.easyrename.model.RenameResult
import com.example.easyrename.model.RenameTargetFile

class StorageRepositoryImpl(
    private val safDocumentDataSource: SafDocumentDataSource,
) : StorageRepository {

    override fun loadFilesInDirectory(directoryUri: Uri): List<RenameTargetFile> {
        return safDocumentDataSource.loadFilesInDirectory(directoryUri)
    }

    override fun readTextFromUri(uri: Uri): String {
        return safDocumentDataSource.readTextFromUri(uri)
    }

    override fun renameFile(directoryUri: Uri, fileUri: Uri, newName: String): RenameResult {
        Log.d(LOG_TAG, "StorageRepository.renameFile directoryUri=$directoryUri fileUri=$fileUri newName=$newName")
        return safDocumentDataSource.renameFile(directoryUri, fileUri, newName)
    }

    override fun existsInSameDirectory(directoryUri: Uri, fileName: String): Boolean {
        return safDocumentDataSource.existsInSameDirectory(directoryUri, fileName)
    }

    override fun takePersistablePermission(uri: Uri, flags: Int): Boolean {
        return safDocumentDataSource.takePersistablePermission(uri, flags)
    }

    private companion object {
        const val LOG_TAG = "EasyRename"
    }
}
