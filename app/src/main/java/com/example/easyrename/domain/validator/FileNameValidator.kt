package com.example.easyrename.domain.validator

class FileNameValidator {

    fun isValid(fileName: String): Boolean {
        if (fileName.isBlank()) {
            return false
        }

        if (fileName == "." || fileName == "..") {
            return false
        }

        return fileName.none { char ->
            char.code < CONTROL_CHAR_LIMIT || char in INVALID_FILE_NAME_CHARS
        }
    }

    private companion object {
        const val CONTROL_CHAR_LIMIT = 32
        val INVALID_FILE_NAME_CHARS = setOf('\\', '/', ':', '*', '?', '"', '<', '>', '|')
    }
}
