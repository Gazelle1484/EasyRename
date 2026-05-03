package com.example.easyrename.model

data class RenameRule(
    val id: String,
    val rawPattern: String,
    val prefix: String,
    val suffix: String,
    val hasWildcard: Boolean,
    val isUsed: Boolean = false,
)
