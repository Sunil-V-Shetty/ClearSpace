package com.sunilshetty.clearspace.ui.diagnostics

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sunilshetty.clearspace.data.local.DeviceDiagnosticsReader
import com.sunilshetty.clearspace.data.model.DeviceDiagnostics
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@HiltViewModel
class DiagnosticsViewModel @Inject constructor(private val reader: DeviceDiagnosticsReader,
    private val savedState: SavedStateHandle) : ViewModel() {
    private val _snapshot = MutableStateFlow<DeviceDiagnostics?>(null)
    val snapshot = _snapshot.asStateFlow()
    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()
    val results = savedState.getStateFlow("results", hashMapOf<String, String>())
    private var refreshing = false
    init { refresh() }
    fun refresh() {
        if (refreshing) return
        refreshing = true
        viewModelScope.launch {
            try { _snapshot.value = withContext(Dispatchers.IO) { reader.read() }; _error.value = null }
            catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
            catch (_: Exception) { _error.value = "Unable to read device information. Try refreshing." }
            finally { refreshing = false }
        }
    }
    fun record(test: String, passed: Boolean) {
        savedState["results"] = HashMap(results.value).apply {
            put(test, if (passed) "Passed by you" else "Issue reported by you")
        }
    }
}
