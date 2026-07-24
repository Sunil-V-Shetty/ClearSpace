package com.sunilshetty.clearspace.ui.unusedapps

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sunilshetty.clearspace.R
import com.sunilshetty.clearspace.core.permissions.hasUsageStatsAccess
import com.sunilshetty.clearspace.core.permissions.uninstallPackageIntent
import com.sunilshetty.clearspace.data.repository.UnusedAppsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@HiltViewModel
class UnusedAppsViewModel @Inject constructor(
    private val unusedAppsRepository: UnusedAppsRepository,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    private val _uiState = MutableStateFlow<UnusedAppsUiState>(UnusedAppsUiState.Loading)
    val uiState: StateFlow<UnusedAppsUiState> = _uiState.asStateFlow()

    private var lastScanAtEpochMs: Long = 0L
    private var lastUsageAccessGranted: Boolean = false
    private var scanInFlight: Boolean = false

    private var uninstallQueue: ArrayDeque<String> = ArrayDeque()
    private var uninstallLauncher: ((Intent) -> Unit)? = null

    init {
        loadUnusedApps(force = true)
    }

    fun loadUnusedApps(force: Boolean = false) {
        val accessGranted = hasUsageStatsAccess(context)
        val cacheFresh = !force &&
            accessGranted == lastUsageAccessGranted &&
            System.currentTimeMillis() - lastScanAtEpochMs < SCAN_CACHE_TTL_MS &&
            _uiState.value is UnusedAppsUiState.Ready

        if (cacheFresh || scanInFlight) return

        viewModelScope.launch {
            scanInFlight = true
            val showLoading = force || _uiState.value !is UnusedAppsUiState.Ready
            if (showLoading) {
                _uiState.value = UnusedAppsUiState.Loading
            }
            if (!accessGranted) {
                runCatching {
                    withContext(Dispatchers.IO) {
                        unusedAppsRepository.loadUninstallableUserApps()
                    }
                }.onSuccess { apps ->
                    _uiState.value = UnusedAppsUiState.Ready(
                        apps = apps,
                        selectedKeys = emptySet(),
                        isUninstalling = false,
                        message = null,
                        needsUsageAccess = true,
                    )
                    lastUsageAccessGranted = false
                }.onFailure {
                    _uiState.value = UnusedAppsUiState.Error(
                        message = context.getString(R.string.unused_apps_error_load),
                    )
                }
                scanInFlight = false
                return@launch
            }
            runCatching {
                withContext(Dispatchers.IO) {
                    unusedAppsRepository.loadUnusedApps()
                }
            }.onSuccess { apps ->
                _uiState.value = UnusedAppsUiState.Ready(
                    apps = apps,
                    selectedKeys = emptySet(),
                    isUninstalling = false,
                    message = null,
                    needsUsageAccess = false,
                )
                lastScanAtEpochMs = System.currentTimeMillis()
                lastUsageAccessGranted = true
            }.onFailure {
                if (_uiState.value is UnusedAppsUiState.Loading) {
                    _uiState.value = UnusedAppsUiState.Error(
                        message = context.getString(R.string.unused_apps_error_load),
                    )
                }
            }
            scanInFlight = false
        }
    }

    fun onScreenResumed() {
        val accessChanged = hasUsageStatsAccess(context) != lastUsageAccessGranted
        if (uninstallQueue.isNotEmpty()) {
            continueUninstallQueue()
        }
        loadUnusedApps(force = accessChanged || uninstallQueue.isEmpty())
    }

    fun toggleSelection(packageName: String) {
        val current = _uiState.value as? UnusedAppsUiState.Ready ?: return
        val updated = if (packageName in current.selectedKeys) {
            current.selectedKeys - packageName
        } else {
            current.selectedKeys + packageName
        }
        _uiState.value = current.copy(selectedKeys = updated, message = null)
    }

    fun selectAll() {
        val current = _uiState.value as? UnusedAppsUiState.Ready ?: return
        _uiState.value = current.copy(
            selectedKeys = current.apps.map { it.selectionKey }.toSet(),
            message = null,
        )
    }

    fun clearSelection() {
        val current = _uiState.value as? UnusedAppsUiState.Ready ?: return
        _uiState.value = current.copy(selectedKeys = emptySet(), message = null)
    }

    fun uninstallSelected(launchUninstall: (Intent) -> Unit) {
        val ready = _uiState.value as? UnusedAppsUiState.Ready ?: return
        if (ready.selectedKeys.isEmpty() || ready.isUninstalling) return

        uninstallLauncher = launchUninstall
        uninstallQueue = ArrayDeque(ready.selectedKeys.toList())
        _uiState.value = ready.copy(isUninstalling = true, message = null)
        startNextUninstall()
    }

    fun onUninstallActivityReturned() {
        uninstallQueue.removeFirstOrNull()
        if (uninstallQueue.isEmpty()) {
            uninstallLauncher = null
            loadUnusedApps(force = true)
        } else {
            startNextUninstall()
        }
    }

    private fun continueUninstallQueue() {
        val ready = _uiState.value as? UnusedAppsUiState.Ready ?: return
        if (uninstallQueue.isNotEmpty() && !ready.isUninstalling) {
            _uiState.value = ready.copy(isUninstalling = true)
            startNextUninstall()
        }
    }

    private fun startNextUninstall() {
        val pkg = uninstallQueue.firstOrNull()
        val launcher = uninstallLauncher
        if (pkg == null || launcher == null) {
            val current = _uiState.value as? UnusedAppsUiState.Ready
            if (current != null) {
                _uiState.value = current.copy(isUninstalling = false)
            }
            if (uninstallQueue.isEmpty()) {
                loadUnusedApps(force = true)
            }
            return
        }
        launcher(uninstallPackageIntent(pkg))
    }

    companion object {
        private const val SCAN_CACHE_TTL_MS = 60_000L
    }
}
