package com.example.easyrename.data.saf

import android.content.Context
import android.net.Uri
import android.os.SystemClock
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import com.example.easyrename.model.RenameErrorType
import com.example.easyrename.model.RenamePath
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
        val start = SystemClock.elapsedRealtime()
        Log.d(LOG_TAG, "Saf.renameFile start directoryUri=$directoryUri fileUri=$fileUri afterName=$newName")
        val fastPathResult = trySingleUriFastPath(fileUri, newName)
        if (fastPathResult.success) {
            Log.d(
                TAG_PERF,
                "saf renameFile end elapsedMs=${SystemClock.elapsedRealtime() - start} path=${fastPathResult.renamePath} success=true errorType=null beforeName=${fastPathResult.beforeName} afterName=${fastPathResult.afterName} afterUri=${fastPathResult.afterUri}",
            )
            return fastPathResult
        }

        val fallbackStart = SystemClock.elapsedRealtime()
        Log.d(
            TAG_PERF,
            "treeUri fallback start reason=${fastPathResult.errorType ?: fastPathResult.errorMessage} beforeName=${fastPathResult.beforeName} afterName=$newName",
        )
        return try {
            val resolveStart = SystemClock.elapsedRealtime()
            Log.d(TAG_PERF, "saf resolve target start directoryUri=$directoryUri fileUri=$fileUri afterName=$newName")
            val singleDocumentFile = DocumentFile.fromSingleUri(context, fileUri)
            val beforeName = singleDocumentFile?.name ?: fileUri.lastPathSegment.orEmpty()

            Log.d(
                LOG_TAG,
                "Saf.fromSingleUri result=${singleDocumentFile != null} exists=${singleDocumentFile?.exists()} canWrite=${singleDocumentFile?.canWrite()} isFile=${singleDocumentFile?.isFile} name=${singleDocumentFile?.name}",
            )

            val directory = DocumentFile.fromTreeUri(context, directoryUri)
            Log.d(LOG_TAG, "Saf.fromTreeUri result=${directory != null} directoryUri=$directoryUri")

            if (directory == null) {
                Log.d(
                    TAG_PERF,
                    "treeUri fallback failed reason=FileNotFound elapsedMs=${SystemClock.elapsedRealtime() - fallbackStart} beforeName=$beforeName afterName=$newName",
                )
                Log.d(
                    TAG_PERF,
                    "saf renameFile end elapsedMs=${SystemClock.elapsedRealtime() - start} path=${RenamePath.TreeUriFallback} success=false errorType=${RenameErrorType.FileNotFound} beforeName=$beforeName afterName=$newName afterUri=null",
                )
                return RenameResult(
                    beforeName = beforeName,
                    afterName = newName,
                    success = false,
                    errorMessage = "FileNotFound: Directory DocumentFile could not be resolved.",
                    errorType = RenameErrorType.FileNotFound,
                    renamePath = RenamePath.TreeUriFallback,
                )
            }

            val directoryFiles = directory.listFiles().filter { it.isFile }
            val targetFile = directoryFiles.firstOrNull { it.uri == fileUri }
                ?: directoryFiles.firstOrNull { it.name == beforeName }
            val duplicateFile = directoryFiles.firstOrNull { file ->
                file.name == newName
            }
            Log.d(
                TAG_PERF,
                "saf resolve target end elapsedMs=${SystemClock.elapsedRealtime() - resolveStart} beforeName=$beforeName afterName=$newName targetFound=${targetFile != null}",
            )

            if (targetFile == null) {
                Log.e(LOG_TAG, "Saf.renameFile FileNotFound directoryUri=$directoryUri fileUri=$fileUri beforeName=$beforeName")
                Log.d(
                    TAG_PERF,
                    "treeUri fallback failed reason=FileNotFound elapsedMs=${SystemClock.elapsedRealtime() - fallbackStart} beforeName=$beforeName afterName=$newName",
                )
                Log.d(
                    TAG_PERF,
                    "saf renameFile end elapsedMs=${SystemClock.elapsedRealtime() - start} path=${RenamePath.TreeUriFallback} success=false errorType=${RenameErrorType.FileNotFound} beforeName=$beforeName afterName=$newName afterUri=null",
                )
                return RenameResult(
                    beforeName = beforeName,
                    afterName = newName,
                    success = false,
                    errorMessage = "FileNotFound: Target file could not be resolved from the selected directory.",
                    errorType = RenameErrorType.FileNotFound,
                    renamePath = RenamePath.TreeUriFallback,
                )
            }

            Log.d(
                LOG_TAG,
                "Saf.target beforeName=${targetFile.name} afterName=$newName uri=${targetFile.uri}",
            )

            if (duplicateFile != null && duplicateFile.uri != targetFile.uri) {
                Log.d(
                    TAG_PERF,
                    "treeUri fallback failed reason=FileAlreadyExists elapsedMs=${SystemClock.elapsedRealtime() - fallbackStart} beforeName=${targetFile.name ?: beforeName} afterName=$newName",
                )
                Log.d(
                    TAG_PERF,
                    "saf renameFile end elapsedMs=${SystemClock.elapsedRealtime() - start} path=${RenamePath.TreeUriFallback} success=false errorType=${RenameErrorType.FileAlreadyExists} beforeName=${targetFile.name ?: beforeName} afterName=$newName afterUri=null",
                )
                return RenameResult(
                    beforeName = targetFile.name ?: beforeName,
                    afterName = newName,
                    success = false,
                    errorMessage = "A file with the same name already exists.",
                    errorType = RenameErrorType.FileAlreadyExists,
                    renamePath = RenamePath.TreeUriFallback,
                )
            }

            val success = renameToSafely(targetFile, newName, targetFile.name ?: beforeName)
            Log.d(LOG_TAG, "Saf.renameTo result=${success.success} afterUri=${success.afterUri}")
            Log.d(
                TAG_PERF,
                "saf renameFile end elapsedMs=${SystemClock.elapsedRealtime() - start} path=${success.renamePath} success=${success.success} errorType=${success.errorType} beforeName=${success.beforeName} afterName=${success.afterName} afterUri=${success.afterUri}",
            )
            Log.d(
                TAG_PERF,
                "treeUri fallback success elapsedMs=${SystemClock.elapsedRealtime() - fallbackStart} success=${success.success} errorType=${success.errorType} beforeName=${success.beforeName} afterName=${success.afterName} afterUri=${success.afterUri}",
            )
            success
        } catch (exception: SecurityException) {
            Log.e(LOG_TAG, "Saf.renameFile SecurityException message=${exception.message}", exception)
            Log.d(
                TAG_PERF,
                "treeUri fallback failed reason=SecurityException elapsedMs=${SystemClock.elapsedRealtime() - fallbackStart} afterName=$newName",
            )
            RenameResult(
                beforeName = fileUri.lastPathSegment.orEmpty(),
                afterName = newName,
                success = false,
                errorMessage = "SecurityException: ファイルまたはディレクトリへのアクセス権限がありません。",
                errorType = RenameErrorType.PermissionDenied,
                renamePath = RenamePath.TreeUriFallback,
            )
        } catch (exception: IllegalArgumentException) {
            Log.e(LOG_TAG, "Saf.renameFile IllegalArgumentException message=${exception.message}", exception)
            Log.d(
                TAG_PERF,
                "treeUri fallback failed reason=IllegalArgumentException elapsedMs=${SystemClock.elapsedRealtime() - fallbackStart} afterName=$newName",
            )
            RenameResult(
                beforeName = fileUri.lastPathSegment.orEmpty(),
                afterName = newName,
                success = false,
                errorMessage = "IllegalArgumentException: ${exception.message ?: "Invalid rename request."}",
                errorType = RenameErrorType.RenameFailed,
                renamePath = RenamePath.TreeUriFallback,
            )
        } catch (exception: Exception) {
            Log.e(LOG_TAG, "Saf.renameFile exceptionClass=${exception::class.java.simpleName} message=${exception.message}", exception)
            Log.d(
                TAG_PERF,
                "treeUri fallback failed reason=${exception::class.java.simpleName} elapsedMs=${SystemClock.elapsedRealtime() - fallbackStart} afterName=$newName",
            )
            RenameResult(
                beforeName = fileUri.lastPathSegment.orEmpty(),
                afterName = newName,
                success = false,
                errorMessage = "${exception::class.java.simpleName}: ${exception.message ?: "Unknown error."}",
                errorType = RenameErrorType.Unknown,
                renamePath = RenamePath.TreeUriFallback,
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
        const val TAG_PERF = "EasyRenamePerf"
        const val SHIFT_JIS = "Shift_JIS"
    }

    private fun trySingleUriFastPath(fileUri: Uri, newName: String): RenameResult {
        val start = SystemClock.elapsedRealtime()
        Log.d(TAG_PERF, "singleUri fast path start fileUri=$fileUri afterName=$newName")
        val singleFile = try {
            DocumentFile.fromSingleUri(context, fileUri)
        } catch (exception: Exception) {
            Log.d(
                TAG_PERF,
                "singleUri fast path failed reason=${exception::class.java.simpleName} elapsedMs=${SystemClock.elapsedRealtime() - start}",
            )
            return RenameResult(
                beforeName = fileUri.lastPathSegment.orEmpty(),
                afterName = newName,
                success = false,
                errorMessage = "${exception::class.java.simpleName}: ${exception.message ?: "Single URI could not be resolved."}",
                errorType = RenameErrorType.Unknown,
                renamePath = RenamePath.SingleUri,
            )
        }

        if (singleFile == null) {
            Log.d(
                TAG_PERF,
                "singleUri fast path failed reason=DocumentFileNull elapsedMs=${SystemClock.elapsedRealtime() - start}",
            )
            return RenameResult(
                beforeName = fileUri.lastPathSegment.orEmpty(),
                afterName = newName,
                success = false,
                errorMessage = "FileNotFound: Single URI DocumentFile could not be resolved.",
                errorType = RenameErrorType.FileNotFound,
                renamePath = RenamePath.SingleUri,
            )
        }

        val beforeName = singleFile.name ?: fileUri.lastPathSegment.orEmpty()
        val result = renameToSafely(singleFile, newName, beforeName, RenamePath.SingleUri)
        if (result.success) {
            Log.d(
                TAG_PERF,
                "singleUri fast path success elapsedMs=${SystemClock.elapsedRealtime() - start} beforeName=${result.beforeName} afterName=${result.afterName} afterUri=${result.afterUri}",
            )
        } else {
            Log.d(
                TAG_PERF,
                "singleUri fast path failed reason=${result.errorType ?: result.errorMessage} elapsedMs=${SystemClock.elapsedRealtime() - start} beforeName=${result.beforeName} afterName=${result.afterName}",
            )
        }
        return result
    }

    private fun renameToSafely(
        targetFile: DocumentFile,
        newName: String,
        beforeName: String,
        renamePath: RenamePath = RenamePath.TreeUriFallback,
    ): RenameResult {
        return try {
            val start = SystemClock.elapsedRealtime()
            Log.d(TAG_PERF, "saf renameTo start beforeName=$beforeName afterName=$newName")
            val renameSuccess = targetFile.renameTo(newName)
            Log.d(
                TAG_PERF,
                "saf renameTo end success=$renameSuccess elapsedMs=${SystemClock.elapsedRealtime() - start} beforeName=$beforeName afterName=$newName afterUri=${if (renameSuccess) targetFile.uri else null}",
            )
            RenameResult(
                beforeName = beforeName,
                afterName = newName,
                success = renameSuccess,
                errorMessage = if (renameSuccess) null else "RenameFailed: DocumentFile.renameTo returned false.",
                errorType = if (renameSuccess) null else RenameErrorType.RenameFailed,
                afterUri = if (renameSuccess) targetFile.uri else null,
                renamePath = renamePath,
            )
        } catch (exception: UnsupportedOperationException) {
            Log.e(LOG_TAG, "Saf.renameTo UnsupportedOperationException message=${exception.message}", exception)
            RenameResult(
                beforeName = beforeName,
                afterName = newName,
                success = false,
                errorMessage = "UnsupportedOperationException: この保存場所ではリネーム操作がサポートされていません。",
                errorType = RenameErrorType.UnsupportedOperation,
                renamePath = renamePath,
            )
        } catch (exception: SecurityException) {
            Log.e(LOG_TAG, "Saf.renameTo SecurityException message=${exception.message}", exception)
            RenameResult(
                beforeName = beforeName,
                afterName = newName,
                success = false,
                errorMessage = "SecurityException: ファイルまたはディレクトリへのアクセス権限がありません。",
                errorType = RenameErrorType.PermissionDenied,
                renamePath = renamePath,
            )
        } catch (exception: IllegalArgumentException) {
            Log.e(LOG_TAG, "Saf.renameTo IllegalArgumentException message=${exception.message}", exception)
            RenameResult(
                beforeName = beforeName,
                afterName = newName,
                success = false,
                errorMessage = "IllegalArgumentException: ${exception.message ?: "Invalid rename request."}",
                errorType = RenameErrorType.RenameFailed,
                renamePath = renamePath,
            )
        } catch (exception: Exception) {
            Log.e(LOG_TAG, "Saf.renameTo exceptionClass=${exception::class.java.simpleName} message=${exception.message}", exception)
            RenameResult(
                beforeName = beforeName,
                afterName = newName,
                success = false,
                errorMessage = "${exception::class.java.simpleName}: ${exception.message ?: "Unknown error."}",
                errorType = RenameErrorType.Unknown,
                renamePath = renamePath,
            )
        }
    }
}
