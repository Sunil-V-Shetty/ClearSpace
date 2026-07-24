package com.sunilshetty.clearspace.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import com.sunilshetty.clearspace.R
import com.sunilshetty.clearspace.util.StorageFormatter

@Composable
fun rememberStorageFormatBytes(): (Long) -> String {
    val labels = rememberStorageFormatLabels()
    return remember(labels) { { bytes: Long -> StorageFormatter.format(bytes, labels) } }
}

@Composable
fun rememberStorageFormatPercent(): (Float) -> String {
    val labels = rememberStorageFormatLabels()
    return remember(labels) { { fraction: Float -> StorageFormatter.formatPercent(fraction, labels) } }
}

@Composable
private fun rememberStorageFormatLabels(): StorageFormatter.Labels {
    val zeroBytesLabel = stringResource(R.string.storage_size_zero)
    val unitB = stringResource(R.string.storage_unit_b)
    val unitKb = stringResource(R.string.storage_unit_kb)
    val unitMb = stringResource(R.string.storage_unit_mb)
    val unitGb = stringResource(R.string.storage_unit_gb)
    val unitTb = stringResource(R.string.storage_unit_tb)
    val wholeSizeFormat = stringResource(R.string.storage_size_whole)
    val decimalSizeFormat = stringResource(R.string.storage_size_decimal)
    val percentFormat = stringResource(R.string.storage_percent)
    return remember(
        zeroBytesLabel,
        unitB,
        unitKb,
        unitMb,
        unitGb,
        unitTb,
        wholeSizeFormat,
        decimalSizeFormat,
        percentFormat,
    ) {
        StorageFormatter.Labels(
            zeroBytesLabel = zeroBytesLabel,
            unitLabels = listOf(unitB, unitKb, unitMb, unitGb, unitTb),
            wholeSizeFormat = wholeSizeFormat,
            decimalSizeFormat = decimalSizeFormat,
            percentFormat = percentFormat,
        )
    }
}
