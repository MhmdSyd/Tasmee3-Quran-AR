package com.tasmee3.app.ui.modeltest

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tasmee3.app.R
import com.tasmee3.app.ui.theme.Tokens
import java.util.Locale

/** Temporary developer screen for step 3. Opened from the menu icon on the reader. */
@Composable
fun ModelTestScreen(onBack: () -> Unit, modifier: Modifier = Modifier, vm: ModelTestViewModel = viewModel()) {
    val state by vm.state.collectAsState()
    val context = LocalContext.current
    val deniedMessage = stringResource(R.string.mic_denied)
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) vm.start() else vm.onPermissionDenied(deniedMessage)
    }
    val ready = state.model is ModelStatus.Ready

    Column(modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = stringResource(R.string.back))
            }
            Text(
                stringResource(R.string.model_test_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Tokens.Ink
            )
        }

        Surface(
            shape = RoundedCornerShape(12.dp),
            color = if (state.model is ModelStatus.Ready) Tokens.GreenTint else Tokens.ErrorFill,
            border = BorderStroke(1.dp, if (state.model is ModelStatus.Ready) Tokens.GreenBorder else Tokens.Error),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = when (val m = state.model) {
                    ModelStatus.Loading -> stringResource(R.string.model_loading)
                    ModelStatus.Missing -> stringResource(R.string.model_missing)
                    is ModelStatus.Ready -> stringResource(
                        R.string.model_ready, String.format(Locale.ROOT, "%.1f", m.loadMillis / 1000f)
                    )
                    is ModelStatus.Failed -> stringResource(R.string.model_failed, m.message)
                },
                modifier = Modifier.padding(12.dp),
                color = Tokens.Ink
            )
        }

        state.error?.let { Text(it, color = Tokens.ErrorText) }

        Button(
            onClick = {
                if (state.recording) {
                    vm.stop()
                } else if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
                    == PackageManager.PERMISSION_GRANTED
                ) {
                    vm.start()
                } else {
                    permission.launch(Manifest.permission.RECORD_AUDIO)
                }
            },
            enabled = ready,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(28.dp)
        ) {
            Text(
                stringResource(if (state.recording) R.string.stop_recording else R.string.start_recording),
                fontWeight = FontWeight.Bold
            )
        }

        if (state.recording) {
            LinearProgressIndicator(progress = { state.level }, modifier = Modifier.fillMaxWidth())
            Text(
                stringResource(if (state.speaking) R.string.speech_detected else R.string.waiting_speech),
                color = Tokens.Muted
            )
        }
        if (state.pendingDecodes > 0) {
            Text(stringResource(R.string.decoding, state.pendingDecodes), color = Tokens.Muted)
        }

        LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(state.segments.asReversed()) { seg ->
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Tokens.Card,
                    border = BorderStroke(1.dp, Tokens.GoldLight),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(seg.text.ifEmpty { "—" }, fontSize = 22.sp, color = Tokens.Ink)
                        Text(
                            stringResource(
                                R.string.segment_meta,
                                String.format(Locale.ROOT, "%.1f", seg.audioSeconds),
                                String.format(Locale.ROOT, "%.1f", seg.decodeMillis / 1000f)
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = Tokens.Muted
                        )
                    }
                }
            }
        }

        if (state.segments.isNotEmpty()) {
            OutlinedButton(onClick = vm::clear, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.clear_results))
            }
        }
    }
}
