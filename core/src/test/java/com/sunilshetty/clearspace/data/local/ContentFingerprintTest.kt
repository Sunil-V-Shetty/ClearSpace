package com.sunilshetty.clearspace.data.local

import org.junit.Assert.*
import org.junit.Test

class ContentFingerprintTest {
    @Test fun samePrefixDifferentTailIsNotADuplicate() {
        val original = ByteArray(600_000) { 7 }
        val changed = original.copyOf().also { it[it.lastIndex] = 8 }
        assertNotEquals(hash(original), hash(changed))
        assertEquals(hash(original), hash(original.copyOf()))
    }
    @Test fun truncatedStreamIsRejected() {
        assertNull(ContentFingerprint.sha256(byteArrayOf(1, 2).inputStream(), 3))
    }
    @Test fun grownStreamIsRejected() {
        assertNull(ContentFingerprint.sha256(byteArrayOf(1, 2, 3).inputStream(), 2))
    }
    private fun hash(bytes: ByteArray) = ContentFingerprint.sha256(bytes.inputStream(), bytes.size.toLong())
}
