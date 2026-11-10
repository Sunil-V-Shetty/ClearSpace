package com.sunilshetty.clearspace.util

import org.junit.Assert.assertEquals
import org.junit.Test

class StorageFormatterTest {
    private val labels = StorageFormatter.Labels(
        zeroBytesLabel = "0 B",
        unitLabels = listOf("B", "KB", "MB", "GB", "TB"),
        wholeSizeFormat = "%1\$d %2\$s",
        decimalSizeFormat = "%1\$.0f %2\$s",
        percentFormat = "%1\$d%%",
    )

    @Test
    fun emptyOrNegativeStorageUsesZeroLabel() {
        assertEquals("0 B", StorageFormatter.format(0L, labels))
        assertEquals("0 B", StorageFormatter.format(-1L, labels))
    }

    @Test
    fun byteValuesUseWholeNumbers() {
        assertEquals("1023 B", StorageFormatter.format(1023L, labels))
    }

    @Test
    fun storageUnitsChangeAtBinaryBoundaries() {
        assertEquals("1 KB", StorageFormatter.format(1024L, labels))
        assertEquals("1 MB", StorageFormatter.format(1024L * 1024, labels))
        assertEquals("1 GB", StorageFormatter.format(1024L * 1024 * 1024, labels))
        assertEquals("1 TB", StorageFormatter.format(1024L * 1024 * 1024 * 1024, labels))
    }

    @Test
    fun percentageStaysWithinDisplayBounds() {
        assertEquals("0%", StorageFormatter.formatPercent(-0.1f, labels))
        assertEquals("50%", StorageFormatter.formatPercent(0.5f, labels))
        assertEquals("100%", StorageFormatter.formatPercent(1.2f, labels))
    }

    @Test(expected = IllegalArgumentException::class)
    fun positiveStorageRequiresAllUnitLabels() {
        StorageFormatter.format(1024L, labels.copy(unitLabels = listOf("B")))
    }
}
