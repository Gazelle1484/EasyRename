package com.example.easyrename.model

enum class RenameMode(
    val displayName: String,
) {
    Prefix("先頭に追加"),
    Suffix("末尾に追加"),
    Replace("置き換え"),
}
