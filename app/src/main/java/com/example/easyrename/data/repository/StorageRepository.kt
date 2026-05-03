package com.example.easyrename.data.repository

import android.net.Uri
import com.example.easyrename.model.RenameResult
import com.example.easyrename.model.RenameTargetFile

interface StorageRepository {

    fun loadFilesInDirectory(directoryUri: Uri): List<RenameTargetFile>

    fun readTextFromUri(uri: Uri): String

    fun renameFile(directoryUri: Uri, fileUri: Uri, newName: String): RenameResult

    fun existsInSameDirectory(directoryUri: Uri, fileName: String): Boolean

    fun takePersistablePermission(uri: Uri, flags: Int): Boolean
}
