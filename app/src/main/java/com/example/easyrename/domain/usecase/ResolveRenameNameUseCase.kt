package com.example.easyrename.domain.usecase

import com.example.easyrename.model.RenameCandidate
import com.example.easyrename.model.RenameTargetFile

class ResolveRenameNameUseCase {

    operator fun invoke(sourceFile: RenameTargetFile, candidate: RenameCandidate): String {
        if (!candidate.rawPattern.contains('*')) {
            return candidate.rawPattern
        }

        val originalName = sourceFile.displayName
        val extensionStart = originalName.lastIndexOf('.').takeIf { it > 0 }
        val baseName = extensionStart?.let { originalName.substring(0, it) } ?: originalName
        val extension = extensionStart?.let { originalName.substring(it) }.orEmpty()
        val resolvedBaseName = candidate.rawPattern.replace("*", baseName)

        return resolvedBaseName + extension
    }
}
