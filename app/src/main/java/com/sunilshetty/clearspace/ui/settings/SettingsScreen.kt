package com.sunilshetty.clearspace.ui.settings

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sunilshetty.clearspace.R
import com.sunilshetty.clearspace.ui.components.BentoCard
import com.sunilshetty.clearspace.ui.components.BentoSubtleText
import com.sunilshetty.clearspace.ui.components.ClearSpaceTopAppBar
import com.sunilshetty.clearspace.ui.components.rememberBentoScrollState
import com.sunilshetty.clearspace.ui.theme.ClearSpaceTheme
import com.sunilshetty.clearspace.ui.theme.ThemeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    themeViewModel: ThemeViewModel = hiltViewModel(LocalContext.current as ComponentActivity),
) {
    val darkModeEnabled by themeViewModel.darkModeEnabled.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets(0),
        topBar = {
            ClearSpaceTopAppBar(
                title = stringResource(R.string.settings_title),
                onBack = onBack,
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberBentoScrollState())
                .navigationBarsPadding()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            BentoCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.settings_dark_mode_title),
                            style = MaterialTheme.typography.titleMedium,
                        )
                        BentoSubtleText(
                            text = stringResource(R.string.settings_dark_mode_body),
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                    Switch(
                        checked = darkModeEnabled,
                        onCheckedChange = themeViewModel::setDarkModeEnabled,
                    )
                }
            }
            SettingsBlock(
                titleRes = R.string.settings_privacy_title,
                bodyRes = R.string.settings_privacy_body,
            )
            SettingsBlock(
                titleRes = R.string.settings_permissions_title,
                bodyRes = R.string.settings_permissions_body,
            )
        }
    }
}

@Composable
private fun SettingsBlock(
    titleRes: Int,
    bodyRes: Int,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = stringResource(titleRes),
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            text = stringResource(bodyRes),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun SettingsScreenPreview() {
    ClearSpaceTheme {
        SettingsScreen(onBack = {})
    }
}
