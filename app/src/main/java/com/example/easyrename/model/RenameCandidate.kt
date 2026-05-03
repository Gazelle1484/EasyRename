package com.example.easyrename.model

data class RenameCandidate(
    val id: String,
    val ruleId: String,
    val displayName: String,
    val rawPattern: String,
    val isSelected: Boolean = false,
    val isUsed: Boolean = false,
)
