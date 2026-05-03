package com.example.easyrename.domain.usecase

import com.example.easyrename.model.RenameCandidate
import com.example.easyrename.model.RenameMode
import com.example.easyrename.model.RenameTargetFile

class ResolveRenameNameUseCase {

    operator fun invoke(
        sourceFile: RenameTargetFile,
        candidate: RenameCandidate,
        renameMode: RenameMode = RenameMode.Prefix,
        autoNumber: Int? = null,
    ): String {
        val originalName = sourceFile.displayName
        val extensionStart = originalName.lastIndexOf('.').takeIf { it > 0 }
        val baseName = extensionStart?.let { originalName.substring(0, it) } ?: originalName
        val extension = extensionStart?.let { originalName.substring(it) }.orEmpty()
        val candidatePattern = applyAutoNumber(candidate.rawPattern, autoNumber)

        if (!candidatePattern.contains('*')) {
            val resolvedBaseName = when (renameMode) {
                RenameMode.Prefix -> "${candidatePattern}_$baseName"
                RenameMode.Suffix -> "${baseName}_$candidatePattern"
                RenameMode.Replace -> removeExtension(candidatePattern)
            }
            return resolvedBaseName + extension
        }

        val resolvedBaseName = candidatePattern.replace("*", baseName)

        return resolvedBaseName + extension
    }

    private fun applyAutoNumber(pattern: String, autoNumber: Int?): String {
        if (autoNumber == null) return pattern

        val suffix = "-$autoNumber"
        val starIndex = pattern.indexOf('*')
        if (starIndex >= 0) {
            val insertIndex = if (starIndex > 0 && pattern[starIndex - 1] == '_') {
                starIndex - 1
            } else {
                starIndex
            }
            return pattern.substring(0, insertIndex) + suffix + pattern.substring(insertIndex)
        }

        val extensionStart = pattern.lastIndexOf('.').takeIf { it > 0 }
        if (extensionStart != null) {
            return pattern.substring(0, extensionStart) + suffix + pattern.substring(extensionStart)
        }

        return pattern + suffix
    }

    private fun removeExtension(fileName: String): String {
        val extensionStart = fileName.lastIndexOf('.').takeIf { it > 0 }
        return extensionStart?.let { fileName.substring(0, it) } ?: fileName
    }
}
