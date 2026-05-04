package com.example.easyrename.domain.history

import com.example.easyrename.model.RenameMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RenameHistoryManagerTest {

    @Test
    fun add_increasesHistoryAndLatestReturnsNewest() {
        val manager = RenameHistoryManager(logger = {})
        val first = record(1)
        val second = record(2)

        manager.add(first)
        manager.add(second)

        assertEquals(2, manager.size())
        assertEquals(second, manager.getLatest())
        assertEquals(listOf(second, first), manager.getAll())
    }

    @Test
    fun add_trimsOldestWhenMaxSizeExceeded() {
        val manager = RenameHistoryManager(maxSize = 30, logger = {})

        (1..31).forEach { index ->
            manager.add(record(index))
        }

        val all = manager.getAll()
        assertEquals(30, all.size)
        assertEquals("before-31.txt", all.first().beforeName)
        assertEquals("before-2.txt", all.last().beforeName)
    }

    @Test
    fun clear_removesAllHistory() {
        val manager = RenameHistoryManager(logger = {})
        manager.add(record(1))

        manager.clear()

        assertEquals(0, manager.size())
        assertNull(manager.getLatest())
        assertEquals(emptyList<RenameHistoryRecord>(), manager.getAll())
    }

    @Test
    fun getAll_returnsSnapshot() {
        val manager = RenameHistoryManager(logger = {})
        manager.add(record(1))
        val snapshot = manager.getAll()

        manager.add(record(2))

        assertEquals(1, snapshot.size)
        assertEquals(2, manager.size())
    }

    private fun record(index: Int): RenameHistoryRecord {
        return RenameHistoryRecord(
            id = "history-$index",
            timestampMillis = index.toLong(),
            beforeName = "before-$index.txt",
            afterName = "after-$index.txt",
            beforeUri = "content://before/$index",
            afterUri = "content://after/$index",
            directoryUri = "content://directory",
            renameMode = RenameMode.Prefix,
            sourceFileIdBefore = "before-id-$index",
            sourceFileIdAfter = "after-id-$index",
            candidateRawPattern = "candidate-$index",
            autoNumber = index,
        )
    }
}
