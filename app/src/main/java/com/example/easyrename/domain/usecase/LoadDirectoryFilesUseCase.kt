package com.example.easyrename.domain.usecase

import android.net.Uri
import com.example.easyrename.data.repository.StorageRepository
import com.example.easyrename.model.RenameTargetFile

class LoadDirectoryFilesUseCase(
    private val storageRepository: StorageRepository,
) {

    operator fun invoke(directoryUri: Uri): List<RenameTargetFile> {
        return storageRepository.loadFilesInDirectory(directoryUri)
    }
}
