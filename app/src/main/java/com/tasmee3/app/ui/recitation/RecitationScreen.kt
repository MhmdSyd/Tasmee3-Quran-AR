package com.tasmee3.app.ui.recitation

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tasmee3.app.R
import com.tasmee3.app.match.AllRevealed
import com.tasmee3.app.match.ErrorInfo
import com.tasmee3.app.match.SessionSnapshot
import com.tasmee3.app.mushaf.ArabicDigits
import com.tasmee3.app.speech.ModelStatus
import com.tasmee3.app.ui.reader.MintButton
import com.tasmee3.app.ui.reader.MushafPageCard
import com.tasmee3.app.ui.reader.MushafTopBar
import com.tasmee3.app.ui.reader.WhitePill
import com.tasmee3.app.ui.theme.Tokens
import java.util.Locale

@Composable
fun RecitationScreen(
    onBack: () -> Unit,
    onSettings: () -> Unit,
    onFinished: () -> Unit,
    modifier: Modifier = Modifier,
    vm: RecitationViewModel = viewModel(),
) {
    val state by vm.state.collectAsState()
    val context = LocalContext.current
    var permissionDenied by rememberSaveable { mutableStateOf(false) }
    var asked by rememberSaveable { mutableStateOf(false) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        permissionDenied = !granted
        if (granted) vm.startListening()
    }
    fun hasMic() = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
        PackageManager.PERMISSION_GRANTED

    val ready = state.model is ModelStatus.Ready && state.snapshot != null
    LaunchedEffect(ready) {
        if (ready && !asked) {
            asked = true
            if (hasMic()) vm.startListening() else launcher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }
    LaunchedEffect(state.finished) { if (state.finished) onFinished() }

    val snapshot = state.snapshot
    val total = snapshot?.total ?: 0
    val position = snapshot?.position ?: 0

    Column(modifier.fillMaxSize().background(Tokens.MushafBg)) {
        MushafTopBar(
            page = state.page,
            meta = state.meta,
            leading = {
                // First child sits at the right in RTL: the back arrow points right, like the reference.
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, stringResource(R.string.back), tint = Tokens.MushafInk)
                }
            },
            trailing = {
                IconButton(onClick = onSettings) {
                    Icon(Icons.Filled.Settings, stringResource(R.string.settings), tint = Tokens.MushafInk)
                }
            }
        )
        ProgressBar(if (total == 0) 0f else position.toFloat() / total)
        Box(Modifier.weight(1f).fillMaxWidth()) {
            MushafPageCard(state.page, state.meta, displays = snapshot ?: AllRevealed)
        }
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp).heightIn(min = 36.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            StatusArea(state, snapshot, permissionDenied) { launcher.launch(Manifest.permission.RECORD_AUDIO) }
        }
        Controls(
            listening = state.listening,
            speaking = state.speaking,
            enabled = ready,
            elapsedMs = state.elapsedMs,
            position = position,
            total = total,
            onHint = vm::hint,
            onToggle = {
                if (state.listening || hasMic()) vm.toggleListening()
                else launcher.launch(Manifest.permission.RECORD_AUDIO)
            },
            onFinish = vm::finish,
        )
    }
}

@Composable
private fun ProgressBar(fraction: Float) {
    Box(Modifier.padding(horizontal = 12.dp).fillMaxWidth().height(3.dp).background(Color(0xFFE6E6E6), RoundedCornerShape(2.dp))) {
        // Fills from the right edge in RTL.
        Box(Modifier.fillMaxWidth(fraction.coerceIn(0f, 1f)).height(3.dp).background(Tokens.Mint, RoundedCornerShape(2.dp)))
    }
}

@Composable
private fun StatusArea(
    state: RecitationUiState,
    snapshot: SessionSnapshot?,
    permissionDenied: Boolean,
    onAskPermission: () -> Unit,
) {
    val error = snapshot?.error
    when {
        state.model is ModelStatus.Loading -> Text(stringResource(R.string.model_loading), color = Tokens.Muted)
        state.model is ModelStatus.Missing -> Text(stringResource(R.string.model_missing), color = Tokens.ErrorText)
        state.model is ModelStatus.Failed ->
            Text(stringResource(R.string.model_failed, (state.model as ModelStatus.Failed).message), color = Tokens.ErrorText)
        permissionDenied -> {
            Text(stringResource(R.string.mic_permission_needed), color = Tokens.ErrorText)
            Button(onClick = onAskPermission) { Text(stringResource(R.string.allow_mic)) }
        }
        error != null -> ErrorPanel(error)
        (snapshot?.position ?: 0) == 0 -> {
            Row(
                Modifier.background(Tokens.GreenTint, RoundedCornerShape(16.dp)).padding(horizontal = 12.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(painterResource(R.drawable.ic_phone_offline), null, tint = Tokens.GreenDark, modifier = Modifier.size(14.dp))
                Text(stringResource(R.string.offline_badge), style = MaterialTheme.typography.bodySmall, color = Tokens.GreenDark)
            }
            Text(stringResource(R.string.listen_hint), style = MaterialTheme.typography.bodySmall, color = Tokens.MushafInk)
        }
    }
    state.error?.let { Text(it, color = Tokens.ErrorText, style = MaterialTheme.typography.bodySmall) }
}

@Composable
private fun ErrorPanel(error: ErrorInfo) {
    Column(
        Modifier.fillMaxWidth()
            .background(Tokens.ErrorFill, RoundedCornerShape(16.dp))
            .border(1.5.dp, Tokens.Error, RoundedCornerShape(16.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(22.dp).border(2.dp, Tokens.Error, CircleShape), contentAlignment = Alignment.Center) {
                Text("!", color = Tokens.Error, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.error_title), color = Tokens.ErrorText, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            Text(
                stringResource(R.string.error_position, ArabicDigits.fromInt(error.verse), ArabicDigits.fromInt(error.wordInVerse)),
                style = MaterialTheme.typography.bodySmall, color = Tokens.Muted
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.error_heard), color = Tokens.Muted, style = MaterialTheme.typography.bodyMedium)
            Text(error.heard, fontSize = 20.sp, color = Tokens.ErrorText, fontWeight = FontWeight.Bold)
        }
        Text(stringResource(R.string.error_retry), style = MaterialTheme.typography.bodySmall, color = Tokens.MushafInk)
    }
}

@Composable
private fun Controls(
    listening: Boolean,
    speaking: Boolean,
    enabled: Boolean,
    elapsedMs: Long,
    position: Int,
    total: Int,
    onHint: () -> Unit,
    onToggle: () -> Unit,
    onFinish: () -> Unit,
) {
    val pulse by animateFloatAsState(if (listening && speaking) 1.08f else 1f, label = "mic-pulse")
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // First child sits at the right (start) edge in RTL: the big mint button, like the reference.
        MintButton(onClick = onToggle, enabled = enabled, pulse = pulse) {
            if (listening) {
                Box(Modifier.size(24.dp).background(Color.White, RoundedCornerShape(4.dp)))   // stop square
            } else {
                Icon(
                    painterResource(R.drawable.ic_mic),
                    contentDescription = stringResource(R.string.resume_listening),
                    tint = Color.White, modifier = Modifier.size(34.dp)
                )
            }
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    Modifier.background(Color(0xFFEFEFEF), RoundedCornerShape(16.dp)).padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(Modifier.size(10.dp).background(if (listening) Color(0xFFEF5350) else Tokens.Muted, CircleShape))
                    val s = elapsedMs / 1000
                    Text(
                        ArabicDigits.convert(String.format(Locale.ROOT, "%02d:%02d", s / 60, s % 60)),
                        style = MaterialTheme.typography.bodyMedium, color = Tokens.Muted
                    )
                }
                if (total > 0) {
                    Text(
                        stringResource(R.string.words_progress, ArabicDigits.fromInt(position), ArabicDigits.fromInt(total)),
                        style = MaterialTheme.typography.bodySmall, color = Tokens.Muted
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                WhitePill(stringResource(R.string.hint), onHint, enabled = enabled) {
                    Icon(painterResource(R.drawable.ic_bulb), null, tint = Tokens.MushafInk, modifier = Modifier.size(20.dp))
                }
                WhitePill(stringResource(R.string.finish), onFinish, enabled = enabled) {
                    Box(Modifier.size(14.dp).background(Tokens.MushafInk, RoundedCornerShape(3.dp)))
                }
            }
        }
    }
}
