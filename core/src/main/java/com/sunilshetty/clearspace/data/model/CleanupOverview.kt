package com.sunilshetty.clearspace.data.model

data class CleanupCandidate(val key: String, val bytes: Long)
data class CleanupOpportunity(val kind: String, val count: Int, val bytes: Long)
data class CleanupOverview(val opportunities: List<CleanupOpportunity>, val uniqueBytes: Long,
    val scannedAt: Long)

fun uniqueCandidateBytes(candidates: List<CleanupCandidate>): Long = candidates
    .groupBy { it.key }.values.sumOf { copies -> copies.maxOf { it.bytes.coerceAtLeast(0) } }
