package com.sunilshetty.clearspace.ui.diagnostics

import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.automirrored.outlined.VolumeUp
import com.sunilshetty.clearspace.ui.components.ClearSpaceActionRow
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sunilshetty.clearspace.R
import com.sunilshetty.clearspace.ui.components.BentoCard
import com.sunilshetty.clearspace.ui.detail.DetailScreenScaffold
import com.sunilshetty.clearspace.util.StorageFormatter

@Composable
fun DiagnosticsScreen(onBack: () -> Unit, viewModel: DiagnosticsViewModel = hiltViewModel()) {
    val snapshot by viewModel.snapshot.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    val results by viewModel.results.collectAsStateWithLifecycle()
    var activeTest by rememberSaveable { mutableStateOf<String?>(null) }
    val owner = LocalLifecycleOwner.current
    DisposableEffect(owner) {
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_RESUME) viewModel.refresh() }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer) }
    }
    DetailScreenScaffold(R.string.diagnostics_title, onBack) {
        OutlinedButton(onClick = viewModel::refresh) { Text("Refresh readings") }
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        BentoCard(modifier = Modifier.padding(vertical = 12.dp)) {
            Text("Hardware checks", style = MaterialTheme.typography.titleMedium)
            listOf("Display", "Touchscreen", "Speaker", "Vibration").forEach { test ->
                ClearSpaceActionRow(test, results[test] ?: "Not checked", when (test) {
                    "Display" -> Icons.Outlined.Smartphone
                    "Touchscreen" -> Icons.Outlined.TouchApp
                    "Speaker" -> Icons.AutoMirrored.Outlined.VolumeUp
                    else -> Icons.Outlined.Vibration
                }, { activeTest = test }, Modifier.testTag("test_$test"))
            }
            Text("Results are based on your checks on this device.", style = MaterialTheme.typography.bodySmall)
        }
        snapshot?.let { info ->
            Text("Updated ${StorageFormatter.formatTimestamp(info.capturedAt)}", style = MaterialTheme.typography.bodySmall)
            info.sections.forEach { (title, rows) ->
                BentoCard(modifier = Modifier.padding(vertical = 8.dp)) {
                    Text(title, style = MaterialTheme.typography.titleMedium)
                    rows.forEach { reading ->
                        Column(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                            Text(reading.label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(reading.value)
                        }
                    }
                }
            }
            Text("Android reports battery condition, not lifespan. Sensors listed here may need a physical check.", style = MaterialTheme.typography.bodySmall)
        } ?: Text(if (error == null) "Reading device information…" else "No readings available")
    }
    activeTest?.let { test -> GuidedTestDialog(test, { activeTest = null }) {
        viewModel.record(test, it); activeTest = null
    } }
}

@Composable
private fun GuidedTestDialog(test: String, onDismiss: () -> Unit, onResult: (Boolean) -> Unit) {
    val context = LocalContext.current
    val owner = LocalLifecycleOwner.current
    var colorIndex by rememberSaveable(test) { mutableIntStateOf(0) }
    var touched by rememberSaveable(test) { mutableIntStateOf(0) }
    var started by rememberSaveable(test) { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val tone = remember { mutableStateOf<ToneGenerator?>(null) }
    val vibrator = remember(context) { context.getSystemService(Vibrator::class.java) }
    fun stopOutput() {
        runCatching { tone.value?.stopTone(); tone.value?.release() }; tone.value = null
        runCatching { vibrator?.cancel() }
    }
    DisposableEffect(owner) {
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_STOP) stopOutput() }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer); stopOutput() }
    }
    val colors = listOf(Color.Red, Color.Green, Color.Blue, Color.White, Color.Black)
    val canPass = when (test) { "Display" -> colorIndex == 4; "Touchscreen" -> touched == 511; else -> started }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxSize()) {
            Column(Modifier.fillMaxSize().safeDrawingPadding().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("$test check", style = MaterialTheme.typography.headlineSmall)
                when (test) {
                    "Display" -> {
                        Text("Inspect all five colors for unusual spots or lines.")
                        Box(Modifier.fillMaxWidth().weight(1f).background(colors[colorIndex]))
                        OutlinedButton(onClick = { colorIndex++ }, enabled = colorIndex < 4) { Text("Next color (${colorIndex + 1}/5)") }
                    }
                    "Touchscreen" -> {
                        Text("Tap all tiles. Checks these spots only, not multi-touch.")
                        Column(Modifier.fillMaxWidth().weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            repeat(3) { row -> Row(Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                repeat(3) { col ->
                                    val index = row * 3 + col
                                    val checked = touched and (1 shl index) != 0
                                    Box(Modifier.weight(1f).fillMaxHeight().testTag("touch_$index")
                                        .background(if (checked) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant)
                                        .clickable { touched = touched or (1 shl index) }, contentAlignment = Alignment.Center) {
                                        Text(if (checked) "Checked ${index + 1}" else "Tap ${index + 1}")
                                    }
                                }
                            } }
                        }
                    }
                    "Speaker" -> {
                        Text("Disconnect headphones and Bluetooth. Set a comfortable volume, then play the tone.")
                        Button(onClick = {
                            stopOutput()
                            started = runCatching {
                                ToneGenerator(AudioManager.STREAM_MUSIC, 35).also { tone.value = it }.startTone(ToneGenerator.TONE_PROP_BEEP, 700)
                            }.getOrDefault(false)
                            error = if (started) null else "Tone unavailable. Try again."
                        }) { Text("Play tone") }
                        Spacer(Modifier.weight(1f))
                    }
                    "Vibration" -> {
                        Text("Hold your phone. Settings may block vibration.")
                        Button(onClick = {
                            started = runCatching {
                                check(vibrator?.hasVibrator() == true)
                                if (Build.VERSION.SDK_INT >= 26) vibrator.vibrate(VibrationEffect.createOneShot(400, VibrationEffect.DEFAULT_AMPLITUDE))
                                else { @Suppress("DEPRECATION") vibrator.vibrate(400) }
                                true
                            }.getOrDefault(false)
                            error = if (started) null else "Vibration unavailable on this device."
                        }) { Text("Start vibration") }
                        Spacer(Modifier.weight(1f))
                    }
                }
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { onResult(true) }, enabled = canPass) { Text("Passed") }
                    OutlinedButton(onClick = { onResult(false) }) { Text("Found an issue") }
                }
                TextButton(onClick = onDismiss) { Text("Skip") }
            }
        }
    }
}
