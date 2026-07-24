package com.sunilshetty.clearspace.ui.theme

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sunilshetty.clearspace.data.repository.UserPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class ThemeViewModel @Inject constructor(
    private val userPreferencesRepository: UserPreferencesRepository,
) : ViewModel() {

    val darkModeEnabled: StateFlow<Boolean> = userPreferencesRepository.darkModeEnabled
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = false,
        )

    fun setDarkModeEnabled(enabled: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.setDarkModeEnabled(enabled)
        }
    }
}
