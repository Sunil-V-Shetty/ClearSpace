package com.sunilshetty.clearspace.ui.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sunilshetty.clearspace.data.model.CleanupOverview
import com.sunilshetty.clearspace.data.repository.CleanupOverviewRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class CleanupOverviewState(val loading: Boolean = false, val result: CleanupOverview? = null, val error: Boolean = false)

@HiltViewModel
class CleanupOverviewViewModel @Inject constructor(private val repository: CleanupOverviewRepository) : ViewModel() {
    private val _state = MutableStateFlow(CleanupOverviewState())
    val state = _state.asStateFlow()
    fun scan() {
        if (_state.value.loading) return
        _state.value = _state.value.copy(loading = true, error = false)
        viewModelScope.launch {
            try { _state.value = CleanupOverviewState(result = repository.scan()) }
            catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
            catch (_: Exception) { _state.value = _state.value.copy(loading = false, error = true) }
        }
    }
}
