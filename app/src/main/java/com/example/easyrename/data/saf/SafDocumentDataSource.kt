package com.example.easyrename.data.saf

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import com.example.easyrename.model.RenameErrorType
import com.example.easyrename.model.RenameResult
import com.example.easyrename.model.RenameTargetFile
import java.nio.ByteBuffer
import java.nio.charset.CharacterCodingException
import java.nio.charset.Charset
import java.nio.charset.CodingErrorAction
import java.nio.charset.StandardCharsets

class SafDocumentDataSource(
    private val context: Context,
) {

    fun loadFilesInDirectory(directoryUri: Uri): List<RenameTargetFile> {
        val directory = DocumentFile.fromTreeUri(context, directoryUri) ?: return emptyList()

        return directory
            .listFiles()
            .filter { it.isFile }
            .map { file ->
                RenameTargetFile(
                    id = file.uri.toString(),
                    displayName = file.name ?: file.uri.lastPathSegment.orEmpty(),
                    uri = file.uri,
                    size = file.length(),
                    lastModified = file.lastModified(),
                )
            }
    }

    fun readTextFromUri(uri: Uri): String {
        val bytes = context.contentResolver.openInputStream(uri)?.use { inputStream ->
            inputStream.readBytes()
        } ?: throw IllegalArgumentException("Unable to open input stream: $uri")

        return decodeText(bytes)
    }

    fun renameFile(directoryUri: Uri, fileUri: Uri, newName: String): RenameResult {
        Log.d(LOG_TAG, "Saf.renameFile start directoryUri=$directoryUri fileUri=$fileUri afterName=$newName")
        return try {
            val singleDocumentFile = DocumentFile.fromSingleUri(context, fileUri)
            val beforeName = singleDocumentFile?.name ?: fileUri.lastPathSegment.orEmpty()

            Log.d(
                LOG_TAG,
                "Saf.fromSingleUri result=${singleDocumentFile != null} exists=${singleDocumentFile?.exists()} canWrite=${singleDocumentFile?.canWrite()} isFile=${singleDocumentFile?.isFile} name=${singleDocumentFile?.name}",
            )

            val directory = DocumentFile.fromTreeUri(context, directoryUri)
            Log.d(LOG_TAG, "Saf.fromTreeUri result=${directory != null} directoryUri=$directoryUri")

            if (directory == null) {
                return RenameResult(
                    beforeName = beforeName,
                    afterName = newName,
                    success = false,
                    errorMessage = "FileNotFound: Directory DocumentFile could not be resolved.",
                    errorType = RenameErrorType.FileNotFound,
                )
            }

            val directoryFiles = directory.listFiles().filter { it.isFile }
            val targetFile = directoryFiles.firstOrNull { it.uri == fileUri }
                ?: directoryFiles.firstOrNull { it.name == beforeName }

            if (targetFile == null) {
                Log.e(LOG_TAG, "Saf.renameFile FileNotFound directoryUri=$directoryUri fileUri=$fileUri beforeName=$beforeName")
                return RenameResult(
                    beforeName = beforeName,
                    afterName = newName,
                    success = false,
                    errorMessage = "FileNotFound: Target file could not be resolved from the selected directory.",
                    errorType = RenameErrorType.FileNotFound,
                )
            }

            Log.d(
                LOG_TAG,
                "Saf.target exists=${targetFile.exists()} canWrite=${targetFile.canWrite()} isFile=${targetFile.isFile} beforeName=${targetFile.name} afterName=$newName uri=${targetFile.uri}",
            )

            val duplicateExists = directoryFiles.any { file ->
                file.uri != targetFile.uri && file.name == newName
            }
            if (duplicateExists) {
                return RenameResult(
                    beforeName = targetFile.name ?: beforeName,
                    afterName = newName,
                    success = false,
                    errorMessage = "A file with the same name already exists.",
                    errorType = RenameErrorType.FileAlreadyExists,
                )
            }

            val success = renameToSafely(targetFile, newName, targetFile.name ?: beforeName)
            Log.d(LOG_TAG, "Saf.renameTo result=${success.success} afterUri=${success.afterUri}")
            success
        } catch (exception: SecurityException) {
            Log.e(LOG_TAG, "Saf.renameFile SecurityException message=${exception.message}", exception)
            RenameResult(
                beforeName = fileUri.lastPathSegment.orEmpty(),
                afterName = newName,
                success = false,
                errorMessage = "SecurityException: ファイルまたはディレクトリへのアクセス権限がありません。",
                errorType = RenameErrorType.PermissionDenied,
            )
        } catch (exception: IllegalArgumentException) {
            Log.e(LOG_TAG, "Saf.renameFile IllegalArgumentException message=${exception.message}", exception)
            RenameResult(
                beforeName = fileUri.lastPathSegment.orEmpty(),
                afterName = newName,
                success = false,
                errorMessage = "IllegalArgumentException: ${exception.message ?: "Invalid rename request."}",
                errorType = RenameErrorType.RenameFailed,
            )
        } catch (exception: Exception) {
            Log.e(LOG_TAG, "Saf.renameFile exceptionClass=${exception::class.java.simpleName} message=${exception.message}", exception)
            RenameResult(
                beforeName = fileUri.lastPathSegment.orEmpty(),
                afterName = newName,
                success = false,
                errorMessage = "${exception::class.java.simpleName}: ${exception.message ?: "Unknown error."}",
                errorType = RenameErrorType.Unknown,
            )
        }
    }

    fun existsInSameDirectory(directoryUri: Uri, fileName: String): Boolean {
        val directory = DocumentFile.fromTreeUri(context, directoryUri) ?: return false

        return directory
            .listFiles()
            .any { it.isFile && it.name == fileName }
    }

    fun takePersistablePermission(uri: Uri, flags: Int): Boolean {
        return try {
            context.contentResolver.takePersistableUriPermission(uri, flags)
            true
        } catch (exception: Exception) {
            false
        }
    }

    private fun decodeText(bytes: ByteArray): String {
        return try {
            StandardCharsets.UTF_8
                .newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT)
                .decode(ByteBuffer.wrap(bytes))
                .toString()
        } catch (exception: CharacterCodingException) {
            String(bytes, Charset.forName(SHIFT_JIS))
        }
    }

    private companion object {
        const val LOG_TAG = "EasyRename"
        const val SHIFT_JIS = "Shift_JIS"
    }

    private fun renameToSafely(targetFile: DocumentFile, newName: String, beforeName: String): RenameResult {
        return try {
            val renameSuccess = targetFile.renameTo(newName)
            RenameResult(
                beforeName = beforeName,
                afterName = newName,
                success = renameSuccess,
                errorMessage = if (renameSuccess) null else "RenameFailed: DocumentFile.renameTo returned false.",
                errorType = if (renameSuccess) null else RenameErrorType.RenameFailed,
                afterUri = if (renameSuccess) targetFile.uri else null,
            )
        } catch (exception: UnsupportedOperationException) {
            Log.e(LOG_TAG, "Saf.renameTo UnsupportedOperationException message=${exception.message}", exception)
            RenameResult(
                beforeName = beforeName,
                afterName = newName,
                success = false,
                errorMessage = "UnsupportedOperationException: この保存場所ではリネーム操作がサポートされていません。",
                errorType = RenameErrorType.UnsupportedOperation,
            )
        } catch (exception: SecurityException) {
            Log.e(LOG_TAG, "Saf.renameTo SecurityException message=${exception.message}", exception)
            RenameResult(
                beforeName = beforeName,
                afterName = newName,
                success = false,
                errorMessage = "SecurityException: ファイルまたはディレクトリへのアクセス権限がありません。",
                errorType = RenameErrorType.PermissionDenied,
            )
        } catch (exception: IllegalArgumentException) {
            Log.e(LOG_TAG, "Saf.renameTo IllegalArgumentException message=${exception.message}", exception)
            RenameResult(
                beforeName = beforeName,
                afterName = newName,
                success = false,
                errorMessage = "IllegalArgumentException: ${exception.message ?: "Invalid rename request."}",
                errorType = RenameErrorType.RenameFailed,
            )
        } catch (exception: Exception) {
            Log.e(LOG_TAG, "Saf.renameTo exceptionClass=${exception::class.java.simpleName} message=${exception.message}", exception)
            RenameResult(
                beforeName = beforeName,
                afterName = newName,
                success = false,
                errorMessage = "${exception::class.java.simpleName}: ${exception.message ?: "Unknown error."}",
                errorType = RenameErrorType.Unknown,
            )
        }
    }
}
