package com.sunilshetty.clearspace.util

import kotlin.math.ln
import kotlin.math.pow

object StorageFormatter {

    data class Labels(
        val zeroBytesLabel: String,
        val unitLabels: List<String>,
        val wholeSizeFormat: String,
        val decimalSizeFormat: String,
        val percentFormat: String,
    )

    private const val REQUIRED_UNIT_LABEL_COUNT = 5

    fun format(bytes: Long, labels: Labels): String {
        if (bytes <= 0L) return labels.zeroBytesLabel
        val units = labels.unitLabels
        require(units.size >= REQUIRED_UNIT_LABEL_COUNT)
        val digitGroup = (ln(bytes.toDouble()) / ln(1024.0)).toInt().coerceIn(0, units.lastIndex)
        val unit = units[digitGroup]
        return if (digitGroup == 0) {
            labels.wholeSizeFormat.format(bytes, unit)
        } else {
            val value = bytes / 1024.0.pow(digitGroup.toDouble())
            labels.decimalSizeFormat.format(value, unit)
        }
    }

    fun formatPercent(fraction: Float, labels: Labels): String {
        val percent = (fraction * 100f).coerceIn(0f, 100f).toInt()
        return labels.percentFormat.format(percent)
    }

    fun formatTimestamp(epochMs: Long): String {
        val formatter = java.text.DateFormat.getDateTimeInstance(
            java.text.DateFormat.MEDIUM,
            java.text.DateFormat.SHORT,
        )
        return formatter.format(java.util.Date(epochMs))
    }
}
