package com.sunilshetty.clearspace.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.sunilshetty.clearspace.data.local.dao.StorageCategoryCacheDao
import com.sunilshetty.clearspace.data.local.dao.TrashItemDao
import com.sunilshetty.clearspace.data.local.entity.StorageCategoryCacheEntity
import com.sunilshetty.clearspace.data.local.entity.TrashItemEntity

@Database(
    entities = [
        StorageCategoryCacheEntity::class,
        TrashItemEntity::class,
    ],
    version = 2,
    exportSchema = false,
)
abstract class ClearSpaceDatabase : RoomDatabase() {
    abstract fun storageCategoryCacheDao(): StorageCategoryCacheDao
    abstract fun trashItemDao(): TrashItemDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `trash_item` (
                        `id` TEXT NOT NULL,
                        `displayName` TEXT NOT NULL,
                        `sizeBytes` INTEGER NOT NULL,
                        `mimeType` TEXT,
                        `mediaBucket` TEXT NOT NULL,
                        `relativePath` TEXT,
                        `deletedAtEpochMs` INTEGER NOT NULL,
                        `storedFileName` TEXT NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent(),
                )
            }
        }
    }
}
