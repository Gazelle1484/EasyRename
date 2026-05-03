package com.example.easyrename.util

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns

object UriDisplayNameUtil {

    fun getDisplayName(context: Context, uri: Uri): String {
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
            ?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val columnIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (columnIndex >= 0) {
                        val displayName = cursor.getString(columnIndex)
                        if (!displayName.isNullOrBlank()) {
                            return displayName
                        }
                    }
                }
            }

        return uri.lastPathSegment?.substringAfterLast('/') ?: uri.toString()
    }
}
