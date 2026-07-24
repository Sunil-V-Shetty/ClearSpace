package com.sunilshetty.clearspace.ui.detail

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.sunilshetty.clearspace.ui.components.BentoScreenBackground
import com.sunilshetty.clearspace.ui.components.ClearSpaceTopAppBar
import com.sunilshetty.clearspace.ui.components.rememberBentoScrollState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreenScaffold(
    @StringRes titleRes: Int,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets(0),
        topBar = {
            ClearSpaceTopAppBar(
                title = stringResource(titleRes),
                onBack = onBack,
            )
        },
    ) { innerPadding ->
        BentoScreenBackground(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberBentoScrollState())
                    .navigationBarsPadding()
                    .padding(16.dp),
            ) {
                content()
            }
        }
    }
}

@Composable
fun DetailBodyText(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        modifier = modifier.padding(vertical = 4.dp),
    )
}
