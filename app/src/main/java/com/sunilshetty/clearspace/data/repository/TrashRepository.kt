package com.sunilshetty.clearspace.data.repository

import com.sunilshetty.clearspace.core.constants.Constants
import com.sunilshetty.clearspace.data.local.TrashFileStore
import com.sunilshetty.clearspace.data.local.dao.TrashItemDao
import com.sunilshetty.clearspace.data.local.entity.TrashItemEntity
import com.sunilshetty.clearspace.data.model.JunkFileItem
import com.sunilshetty.clearspace.data.model.TrashItem
import java.io.File
import java.util.UUID
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

@Singleton
class TrashRepository @Inject constructor(
    private val trashItemDao: TrashItemDao,
    private val trashFileStore: TrashFileStore,
) {

    suspend fun purgeExpired() = withContext(Dispatchers.IO) {
        purgeExpiredInternal()
    }

    suspend fun loadActiveItems(): List<TrashItem> = withContext(Dispatchers.IO) {
        purgeExpiredInternal()
        val minDeletedAt = System.currentTimeMillis() - retentionMillis()
        trashItemDao.getActive(minDeletedAt).map { it.toModel() }
    }

    fun archiveFromJunkItems(items: List<JunkFileItem>): Int = runBlocking(Dispatchers.IO) {
        purgeExpiredInternal()
        if (items.isEmpty()) return@runBlocking 0
        val now = System.currentTimeMillis()
        val trashDir = trashFileStore.trashDirectory()
        var archived = 0
        items.forEach { item ->
            val id = UUID.randomUUID().toString()
            val extension = File(item.displayName).extension.let { ext ->
                if (ext.isNotBlank()) ".$ext" else ""
            }
            val storedFileName = id + extension
            val destFile = File(trashDir, storedFileName)
            if (!trashFileStore.copyJunkItemToTrash(item, destFile)) return@forEach
            val entity = TrashItemEntity(
                id = id,
                displayName = item.displayName,
                sizeBytes = destFile.length().takeIf { it > 0L } ?: item.sizeBytes,
                mimeType = trashFileStore.resolveMimeType(item),
                mediaBucket = trashFileStore.mediaBucketFor(item),
                relativePath = item.relativePath,
                deletedAtEpochMs = now,
                storedFileName = storedFileName,
            )
            runCatching {
                trashItemDao.insert(entity)
            }.onSuccess { archived++ }
                .onFailure { trashFileStore.deleteStoredFile(storedFileName) }
        }
        archived
    }

    suspend fun restoreItems(ids: Set<String>): Int = withContext(Dispatchers.IO) {
        if (ids.isEmpty()) return@withContext 0
        val entities = trashItemDao.getByIds(ids)
        var restored = 0
        entities.forEach { entity ->
            if (trashFileStore.restoreToMediaStore(entity)) {
                trashFileStore.deleteStoredFile(entity.storedFileName)
                trashItemDao.deleteByIds(setOf(entity.id))
                restored++
            }
        }
        restored
    }

    suspend fun deleteForever(ids: Set<String>): Int = withContext(Dispatchers.IO) {
        if (ids.isEmpty()) return@withContext 0
        val entities = trashItemDao.getByIds(ids)
        entities.forEach { trashFileStore.deleteStoredFile(it.storedFileName) }
        trashItemDao.deleteByIds(ids)
        entities.size
    }

    private suspend fun purgeExpiredInternal() {
        val cutoff = System.currentTimeMillis() - retentionMillis()
        val stale = trashItemDao.listOlderThan(cutoff)
        stale.forEach { trashFileStore.deleteStoredFile(it.storedFileName) }
        trashItemDao.deleteOlderThan(cutoff)
    }

    private fun TrashItemEntity.toModel(): TrashItem {
        val expiresAt = deletedAtEpochMs + retentionMillis()
        val daysLeft = TimeUnit.MILLISECONDS.toDays(expiresAt - System.currentTimeMillis())
            .toInt()
            .coerceAtLeast(0)
        return TrashItem(
            id = id,
            displayName = displayName,
            sizeBytes = sizeBytes,
            deletedAtEpochMs = deletedAtEpochMs,
            daysLeft = daysLeft,
        )
    }

    private fun retentionMillis(): Long =
        TimeUnit.DAYS.toMillis(Constants.Trash.RETENTION_DAYS.toLong())
}
