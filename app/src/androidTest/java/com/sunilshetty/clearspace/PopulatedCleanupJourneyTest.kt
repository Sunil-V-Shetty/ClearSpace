package com.sunilshetty.clearspace

import android.Manifest
import android.content.ContentValues
import android.net.Uri
import android.provider.MediaStore
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import com.sunilshetty.clearspace.core.constants.Constants
import com.sunilshetty.clearspace.data.local.ClearSpaceDatabase
import com.sunilshetty.clearspace.data.local.CategoryMediaScanner
import com.sunilshetty.clearspace.data.model.StorageCategory
import com.sunilshetty.clearspace.data.local.TrashFileStore
import com.sunilshetty.clearspace.data.repository.TrashRepository
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import java.util.UUID
import java.util.regex.Pattern

@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = 33)
class PopulatedCleanupJourneyTest {
    @get:Rule val ui = createAndroidComposeRule<MainActivity>()
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext
    private val name = "clearspace-ui-${UUID.randomUUID()}.bin"
    private val bytes = ByteArray(8_193) { (it % 251).toByte() }
    private var original: Uri? = null

    @Before fun seedOwnedFile() {
        instrumentation.uiAutomation.grantRuntimePermission(context.packageName, Manifest.permission.READ_MEDIA_AUDIO)
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, "application/octet-stream")
            put(MediaStore.MediaColumns.RELATIVE_PATH, "Download/ClearSpaceTests/")
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }
        original = requireNotNull(context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values))
        context.contentResolver.openOutputStream(original!!)!!.use { it.write(bytes) }
        context.contentResolver.update(original!!, ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) }, null, null)
        ui.waitUntil(15_000) { ui.onAllNodesWithTag("nav_tools").fetchSemanticsNodes().isNotEmpty() }
    }

    @After fun removeOnlyTestArtifacts() = runBlocking<Unit> {
        context.contentResolver.delete(MediaStore.Downloads.EXTERNAL_CONTENT_URI,
            "${MediaStore.MediaColumns.DISPLAY_NAME}=?", arrayOf(name))
        val db = Room.databaseBuilder(context, ClearSpaceDatabase::class.java, Constants.Database.FILE_NAME)
            .addMigrations(ClearSpaceDatabase.MIGRATION_1_2).build()
        try {
            val trash = TrashRepository(db.trashItemDao(), TrashFileStore(context))
            trash.deleteForever(trash.loadActiveItems().filter { it.displayName == name }.map { it.id }.toSet())
        } finally { db.close() }
    }

    @Test fun downloadCanBeSelectedDeletedAndRestoredThroughUi() {
        assertTrue(CategoryMediaScanner(context).scanCategory(StorageCategory.DOWNLOADS).any { it.displayName == name })
        ui.onNodeWithText("Downloads").assertHasClickAction()
        ui.onNodeWithText("Downloads").performScrollTo().performClick()
        ui.waitUntil(10_000) { ui.onAllNodesWithText(name).fetchSemanticsNodes().isNotEmpty() }
        ui.onNodeWithContentDescription("Select $name").performScrollTo().performClick()
        ui.onNodeWithText("Delete selected (1", substring = true).assertIsEnabled().performClick()
        // Some Android builds request write consent even for an app-owned item.
        UiDevice.getInstance(instrumentation).wait(Until.findObject(By.text(Pattern.compile("(?i)allow|confirm|ok"))), 1_500)?.click()
        ui.waitUntil(15_000) {
            context.contentResolver.query(original!!, arrayOf("_id"), null, null, null)?.use { !it.moveToFirst() } ?: true
        }
        ui.onNodeWithContentDescription("Back").performClick()
        ui.onNodeWithTag("nav_tools").performClick()
        ui.onNodeWithText("Trash", ignoreCase = true).performScrollTo().performClick()
        ui.waitUntil(10_000) { ui.onAllNodesWithText(name).fetchSemanticsNodes().isNotEmpty() }
        ui.onNodeWithContentDescription("Select $name").performScrollTo().performClick()
        ui.onNodeWithText("Restore selected (1)").assertIsEnabled().performClick()
        ui.waitUntil(15_000) { restoredUri() != null }
        val restored = requireNotNull(restoredUri())
        assertArrayEquals(bytes, context.contentResolver.openInputStream(restored)!!.use { it.readBytes() })
        ui.waitUntil(10_000) { ui.onAllNodesWithText(name).fetchSemanticsNodes().isEmpty() }
    }

    private fun restoredUri(): Uri? = context.contentResolver.query(MediaStore.Downloads.EXTERNAL_CONTENT_URI,
        arrayOf("_id"), "${MediaStore.MediaColumns.DISPLAY_NAME}=?", arrayOf(name), null)?.use {
        if (it.moveToFirst()) android.content.ContentUris.withAppendedId(MediaStore.Downloads.EXTERNAL_CONTENT_URI, it.getLong(0)) else null
    }
}
