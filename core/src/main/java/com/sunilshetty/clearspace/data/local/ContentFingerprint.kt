package com.sunilshetty.clearspace.data.local

import java.io.InputStream
import java.security.MessageDigest

/** Only complete, unchanged-length streams can be considered duplicates. */
object ContentFingerprint {
    fun sha256(input: InputStream, expectedBytes: Long): String? {
        val digest = MessageDigest.getInstance("SHA-256")
        val buffer = ByteArray(64 * 1024)
        var total = 0L
        while (true) {
            if (Thread.currentThread().isInterrupted) throw InterruptedException()
            val count = input.read(buffer)
            if (count < 0) break
            if (count == 0) continue
            total += count
            digest.update(buffer, 0, count)
        }
        if (total != expectedBytes) return null
        return digest.digest().joinToString("") { "%02x".format(it) }
    }
}
