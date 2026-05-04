package com.example.easyrename.data.preferences

import android.content.Context
import android.util.Log
import com.example.easyrename.model.LastUsedSet

class LastUsedSetStore(context: Context) {

    private val preferences = context.applicationContext.getSharedPreferences(
        PREFERENCES_NAME,
        Context.MODE_PRIVATE,
    )

    fun save(lastUsedSet: LastUsedSet) {
        runCatching {
            preferences.edit()
                .putString(KEY_DIRECTORY_URI, lastUsedSet.directoryUriString)
                .putString(KEY_DIRECTORY_DISPLAY_NAME, lastUsedSet.directoryDisplayName)
                .putString(KEY_CSV_URI, lastUsedSet.csvUriString)
                .putString(KEY_CSV_DISPLAY_NAME, lastUsedSet.csvDisplayName)
                .apply()
            Log.d(
                LOG_TAG,
                "LastUsedSetStore.save directoryName=${lastUsedSet.directoryDisplayName} csvName=${lastUsedSet.csvDisplayName}",
            )
        }.onFailure { throwable ->
            Log.w(LOG_TAG, "LastUsedSetStore.save failed message=${throwable.message}", throwable)
        }
    }

    fun load(): LastUsedSet? {
        return runCatching {
            val directoryUri = preferences.getString(KEY_DIRECTORY_URI, null)
            val directoryDisplayName = preferences.getString(KEY_DIRECTORY_DISPLAY_NAME, null)
            val csvUri = preferences.getString(KEY_CSV_URI, null)
            val csvDisplayName = preferences.getString(KEY_CSV_DISPLAY_NAME, null)

            if (
                directoryUri.isNullOrBlank() ||
                directoryDisplayName.isNullOrBlank() ||
                csvUri.isNullOrBlank() ||
                csvDisplayName.isNullOrBlank()
            ) {
                Log.d(LOG_TAG, "LastUsedSetStore.load null")
                return@runCatching null
            }

            LastUsedSet(
                directoryUriString = directoryUri,
                directoryDisplayName = directoryDisplayName,
                csvUriString = csvUri,
                csvDisplayName = csvDisplayName,
            ).also {
                Log.d(
                    LOG_TAG,
                    "LastUsedSetStore.load success directoryName=${it.directoryDisplayName} csvName=${it.csvDisplayName}",
                )
            }
        }.onFailure { throwable ->
            Log.w(LOG_TAG, "LastUsedSetStore.load failed message=${throwable.message}", throwable)
        }.getOrNull()
    }

    fun clear() {
        runCatching {
            preferences.edit().clear().apply()
        }.onFailure { throwable ->
            Log.w(LOG_TAG, "LastUsedSetStore.clear failed message=${throwable.message}", throwable)
        }
    }

    private companion object {
        const val LOG_TAG = "EasyRename"
        const val PREFERENCES_NAME = "last_used_set"
        const val KEY_DIRECTORY_URI = "directory_uri"
        const val KEY_DIRECTORY_DISPLAY_NAME = "directory_display_name"
        const val KEY_CSV_URI = "csv_uri"
        const val KEY_CSV_DISPLAY_NAME = "csv_display_name"
    }
}
