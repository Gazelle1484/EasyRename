package com.example.easyrename.model

import android.net.Uri

data class RenamePair(
    val sourceFile: RenameTargetFile,
    val renameCandidate: RenameCandidate,
    val resolvedNewName: String,
    val directoryUri: Uri,
)
