package com.example.easyrename.domain.history

import android.util.Log

class RenameHistoryManager(
    private val maxSize: Int = DEFAULT_MAX_SIZE,
    private val logger: (String) -> Unit = { message -> Log.d(TAG_HISTORY, message) },
) {

    private val records = ArrayDeque<RenameHistoryRecord>()

    @Synchronized
    fun add(record: RenameHistoryRecord) {
        records.addFirst(record)
        logger(
            "add success beforeName=${record.beforeName} afterName=${record.afterName} historySize=${records.size}",
        )
        logger("beforeUri=${record.beforeUri}")
        logger("afterUri=${record.afterUri}")
        logger("renameMode=${record.renameMode} autoNumber=${record.autoNumber}")

        while (records.size > maxSize) {
            val removed = records.removeLast()
            logger(
                "trim oldest removed beforeName=${removed.beforeName} afterName=${removed.afterName}",
            )
        }
    }

    // Newest record first.
    @Synchronized
    fun getAll(): List<RenameHistoryRecord> = records.toList()

    @Synchronized
    fun getLatest(): RenameHistoryRecord? = records.firstOrNull()

    @Synchronized
    fun clear() {
        records.clear()
    }

    @Synchronized
    fun size(): Int = records.size

    companion object {
        const val DEFAULT_MAX_SIZE = 30
        private const val TAG_HISTORY = "EasyRenameHistory"
    }
}
