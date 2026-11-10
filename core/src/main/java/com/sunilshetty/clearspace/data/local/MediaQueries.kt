package com.sunilshetty.clearspace.data.local

import android.content.ContentResolver
import android.database.Cursor
import android.net.Uri

/** Denial for one collection must not hide other collections the user allowed. */
internal fun ContentResolver.queryVisible(
    uri: Uri, projection: Array<String>?, selection: String?,
    selectionArgs: Array<String>?, sortOrder: String?,
): Cursor? = try {
    query(uri, projection, selection, selectionArgs, sortOrder)
} catch (_: SecurityException) {
    null
}
