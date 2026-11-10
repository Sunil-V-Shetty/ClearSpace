package com.sunilshetty.clearspace.ui.components

import android.app.PendingIntent
import com.sunilshetty.clearspace.data.model.JunkFileItem
import com.sunilshetty.clearspace.data.repository.JunkDeletePlan
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Requests Android write access, then archives before deleting each original. */
class CleanupController {
    private var approval: CompletableDeferred<Boolean>? = null

    fun onApproval(granted: Boolean) { approval?.complete(granted) }

    suspend fun clean(
        items: List<JunkFileItem>,
        plan: (List<JunkFileItem>) -> JunkDeletePlan,
        request: (List<JunkFileItem>) -> PendingIntent?,
        delete: (List<JunkFileItem>) -> Int,
        launch: (PendingIntent) -> Unit,
    ): Int {
        var removed = 0
        try {
            val batches = withContext(Dispatchers.IO) { plan(items) }
            for (batch in batches.systemPromptBatches) {
                val pending = withContext(Dispatchers.IO) { request(batch) }
                if (pending != null) {
                    val result = CompletableDeferred<Boolean>()
                    approval = result
                    launch(pending)
                    if (!result.await()) return removed
                    approval = null
                }
                // One archive/delete pair is atomic from the caller's perspective.
                // withContext checks cancellation before starting the next file.
                for (item in batch) removed += withContext(Dispatchers.IO) { delete(listOf(item)) }
            }
            for (item in batches.directDelete) removed += withContext(Dispatchers.IO) { delete(listOf(item)) }
            return removed
        } finally {
            approval = null
        }
    }
}
