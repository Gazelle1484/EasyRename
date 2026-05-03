package com.example.easyrename.domain.usecase

import com.example.easyrename.model.RenameCandidate
import com.example.easyrename.model.RenameMode
import com.example.easyrename.model.RenameTargetFile

class ResolveRenameNameUseCase {

    operator fun invoke(
        sourceFile: RenameTargetFile,
        candidate: RenameCandidate,
        renameMode: RenameMode = RenameMode.Prefix,
    ): String {
        val originalName = sourceFile.displayName
        val extensionStart = originalName.lastIndexOf('.').takeIf { it > 0 }
        val baseName = extensionStart?.let { originalName.substring(0, it) } ?: originalName
        val extension = extensionStart?.let { originalName.substring(it) }.orEmpty()

        if (!candidate.rawPattern.contains('*')) {
            val resolvedBaseName = when (renameMode) {
                RenameMode.Prefix -> "${candidate.rawPattern}_$baseName"
                RenameMode.Suffix -> "${baseName}_${candidate.rawPattern}"
                RenameMode.Replace -> removeExtension(candidate.rawPattern)
            }
            return resolvedBaseName + extension
        }

        val resolvedBaseName = candidate.rawPattern.replace("*", baseName)

        return resolvedBaseName + extension
    }

    private fun removeExtension(fileName: String): String {
        val extensionStart = fileName.lastIndexOf('.').takeIf { it > 0 }
        return extensionStart?.let { fileName.substring(0, it) } ?: fileName
    }
}
