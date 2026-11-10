package com.sunilshetty.clearspace

import android.app.PendingIntent
import android.content.ContentUris
import android.content.ContentValues
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.sunilshetty.clearspace.data.local.*
import com.sunilshetty.clearspace.data.model.JunkFileItem
import com.sunilshetty.clearspace.data.repository.*
import com.sunilshetty.clearspace.ui.components.CleanupController
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class CleanupRecoveryTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private lateinit var database: ClearSpaceDatabase
    private lateinit var trash: TrashRepository
    private lateinit var cleaner: JunkFilesRepository
    private val fixtures = mutableListOf<Uri>()
    private val names = mutableListOf<String>()

    @Before fun setup() {
        database = Room.inMemoryDatabaseBuilder(context, ClearSpaceDatabase::class.java).build()
        trash = TrashRepository(database.trashItemDao(), TrashFileStore(context))
        cleaner = JunkFilesRepository(JunkFileScanner(context), JunkFileClassifier(), trash, context)
    }
    @After fun teardown() = runBlocking {
        trash.deleteForever(trash.loadActiveItems().map { it.id }.toSet())
        fixtures.forEach { runCatching { context.contentResolver.delete(it, null, null) } }
        names.forEach { name ->
            context.contentResolver.delete(MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                "${MediaStore.MediaColumns.DISPLAY_NAME}=?", arrayOf(name))
        }
        database.close()
    }
    private fun fixture(bytes: ByteArray): JunkFileItem {
        val name = "clearspace-e2e-${UUID.randomUUID()}.bin"
        names += name
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, "application/octet-stream")
            put(MediaStore.MediaColumns.RELATIVE_PATH, "Download/ClearSpaceTests/")
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }
        val uri = requireNotNull(context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values))
        fixtures += uri
        context.contentResolver.openOutputStream(uri)!!.use { it.write(bytes) }
        context.contentResolver.update(uri, ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) }, null, null)
        return JunkFileItem(ContentUris.parseId(uri), uri, name, bytes.size.toLong(), "Download/ClearSpaceTests/", "Downloads")
    }
    private fun exists(uri: Uri) = context.contentResolver.query(uri, arrayOf("_id"), null, null, null)?.use { it.moveToFirst() } == true

    @Test fun deleteThenRestorePreservesExactBytes() = runBlocking {
        val bytes = ByteArray(70_001) { (it % 251).toByte() }
        val item = fixture(bytes)
        assertEquals(1, cleaner.deleteSelected(listOf(item)))
        assertFalse(exists(item.uri))
        val archived = trash.loadActiveItems().single()
        assertEquals(bytes.size.toLong(), archived.sizeBytes)
        assertEquals(1, trash.restoreItems(setOf(archived.id)))
        assertTrue(trash.loadActiveItems().isEmpty())
        context.contentResolver.query(MediaStore.Downloads.EXTERNAL_CONTENT_URI, arrayOf("_id"),
            "${MediaStore.MediaColumns.DISPLAY_NAME}=?", arrayOf(item.displayName), null)!!.use {
            assertTrue(it.moveToFirst())
            val restored = ContentUris.withAppendedId(MediaStore.Downloads.EXTERNAL_CONTENT_URI, it.getLong(0))
            assertArrayEquals(bytes, context.contentResolver.openInputStream(restored)!!.use { stream -> stream.readBytes() })
        }
    }
    @Test fun failedRecoveryCopyNeverDeletesOriginal() = runBlocking {
        val item = fixture(byteArrayOf(1, 2, 3))
        assertEquals(0, cleaner.deleteSelected(listOf(item.copy(sizeBytes = 200))))
        assertTrue(exists(item.uri))
        assertTrue(trash.loadActiveItems().isEmpty())
    }
    @Test fun emptyFileCanBeRecoveredAndPermanentlyRemoved() = runBlocking {
        val item = fixture(byteArrayOf())
        assertEquals(1, cleaner.deleteSelected(listOf(item)))
        val archived = trash.loadActiveItems().single()
        assertEquals(0L, archived.sizeBytes)
        assertEquals(1, trash.deleteForever(setOf(archived.id)))
        assertTrue(trash.loadActiveItems().isEmpty())
    }
    @Test fun canceledApprovalDoesNotArchiveOrDelete() = runBlocking {
        val item = fixture(byteArrayOf(1, 2, 3))
        val controller = CleanupController()
        val pending = PendingIntent.getActivity(context, 918, Intent(context, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE)
        val removed = controller.clean(listOf(item),
            { JunkDeletePlan(listOf(it), emptyList()) }, { pending }, cleaner::deleteSelected,
            { controller.onApproval(false) })
        assertEquals(0, removed)
        assertTrue(exists(item.uri))
        assertTrue(trash.loadActiveItems().isEmpty())
    }
    @Test fun approvedBatchUsesRecoverableDeletion() = runBlocking {
        val item = fixture(byteArrayOf(4, 5, 6))
        val controller = CleanupController()
        val pending = PendingIntent.getActivity(context, 919, Intent(context, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE)
        assertEquals(1, controller.clean(listOf(item),
            { JunkDeletePlan(listOf(it), emptyList()) }, { pending }, cleaner::deleteSelected,
            { controller.onApproval(true) }))
        assertFalse(exists(item.uri))
        assertEquals(1, trash.loadActiveItems().size)
    }
    @Test fun duplicateScanRejectsChangedTailsAndCollectionAliases() {
        val bytes = ByteArray(600_000) { 7 }
        val first = fixture(bytes)
        val changed = fixture(bytes.copyOf().also { it[it.lastIndex] = 8 })
        val same = fixture(bytes.copyOf())
        val groups = DuplicateScanner(context).scanDuplicateGroups()
        val matching = groups.single { group -> group.files.any { it.id == first.id } }
        assertEquals(setOf(first.id, same.id), matching.files.map { it.id }.toSet())
        assertEquals(2, matching.files.size)
        assertFalse(groups.any { group -> group.files.any { it.id == changed.id } })
    }
}
