package com.example.easyrename.domain.usecase

import android.content.Intent
import android.net.Uri
import com.example.easyrename.data.repository.StorageRepository

class TakePersistablePermissionUseCase(
    private val storageRepository: StorageRepository,
) {

    fun forDirectory(uri: Uri): Boolean {
        return storageRepository.takePersistablePermission(
            uri = uri,
            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
        )
    }

    fun forCsv(uri: Uri): Boolean {
        return storageRepository.takePersistablePermission(
            uri = uri,
            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION,
        )
    }
}
