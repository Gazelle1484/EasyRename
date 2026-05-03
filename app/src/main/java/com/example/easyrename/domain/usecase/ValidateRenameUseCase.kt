package com.example.easyrename.domain.usecase

import com.example.easyrename.domain.validator.FileNameValidator
import com.example.easyrename.model.RenamePair

class ValidateRenameUseCase(
    private val fileNameValidator: FileNameValidator = FileNameValidator(),
) {

    operator fun invoke(renamePair: RenamePair): Boolean {
        return fileNameValidator.isValid(renamePair.resolvedNewName)
    }
}
