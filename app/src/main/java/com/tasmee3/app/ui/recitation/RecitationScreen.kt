package com.tasmee3.app.ui.recitation

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
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
import com.tasmee3.app.match.ErrorInfo
import com.tasmee3.app.match.SessionSnapshot
import com.tasmee3.app.mushaf.ArabicDigits
import com.tasmee3.app.mushaf.MushafLine
import com.tasmee3.app.mushaf.MushafPage
import com.tasmee3.app.speech.ModelStatus
import com.tasmee3.app.ui.reader.MushafPageCard
import com.tasmee3.app.ui.theme.Tokens

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

    Column(modifier.fillMaxSize()) {
        RecitationTopBar(
            surah = state.page?.let { state.meta?.surahNames?.get(it.surah) }.orEmpty(),
            subtitle = subtitle(state.page, position, total),
            onBack = onBack,
            onSettings = onSettings,
        )
        ProgressBar(if (total == 0) 0f else position.toFloat() / total)
        Box(Modifier.weight(1f).fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
            MushafPageCard(
                page = state.page,
                meta = state.meta,
                displays = snapshot ?: com.tasmee3.app.match.AllRevealed,
            )
        }
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp).heightIn(min = 120.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            StatusArea(state, snapshot, permissionDenied) { launcher.launch(Manifest.permission.RECORD_AUDIO) }
        }
        Controls(
            listening = state.listening,
            speaking = state.speaking,
            enabled = ready,
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
private fun subtitle(page: MushafPage?, position: Int, total: Int): String {
    if (page == null) return ""
    if (position > 0) {
        return stringResource(R.string.words_progress, ArabicDigits.fromInt(position), ArabicDigits.fromInt(total))
    }
    val verses = page.lines.filterIsInstance<MushafLine.Words>().flatMap { it.words }
        .filter { it.isSpoken }.mapNotNull { it.location.split(':').getOrNull(1)?.toIntOrNull() }
    if (verses.isEmpty()) return ""
    return stringResource(
        R.string.page_verses,
        ArabicDigits.fromInt(page.number),
        ArabicDigits.fromInt(verses.first()),
        ArabicDigits.fromInt(verses.last())
    )
}

@Composable
private fun RecitationTopBar(surah: String, subtitle: String, onBack: () -> Unit, onSettings: () -> Unit) {
    val name = surah.removePrefix("سورة").trim()
    Row(
        Modifier.fillMaxWidth().height(64.dp).padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // First child sits at the right in RTL: back arrow points right, like the design.
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = stringResource(R.string.back), tint = Tokens.Ink)
        }
        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                stringResource(R.string.recite_title, name),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Tokens.Ink
            )
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = Tokens.Muted)
        }
        IconButton(onClick = onSettings) {
            Icon(Icons.Filled.Settings, contentDescription = stringResource(R.string.settings), tint = Tokens.Ink)
        }
    }
}

@Composable
private fun ProgressBar(fraction: Float) {
    Box(
        Modifier.padding(horizontal = 16.dp).fillMaxWidth().height(4.dp)
            .background(Color(0xFFE8E0C8), RoundedCornerShape(2.dp))
    ) {
        // Fills from the right edge in RTL.
        Box(Modifier.fillMaxWidth(fraction.coerceIn(0f, 1f)).height(4.dp).background(Tokens.Green, RoundedCornerShape(2.dp)))
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
        else -> {
            if ((snapshot?.position ?: 0) == 0) {
                OfflineBadge()
                Text(
                    stringResource(R.string.listen_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Tokens.Ink
                )
            }
            Waveform(state.levels, state.listening)
        }
    }
    state.error?.let { Text(it, color = Tokens.ErrorText, style = MaterialTheme.typography.bodySmall) }
}

@Composable
private fun OfflineBadge() {
    Row(
        Modifier.background(Tokens.GreenTint, RoundedCornerShape(16.dp)).padding(horizontal = 14.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(painterResource(R.drawable.ic_phone_offline), null, tint = Tokens.GreenDark, modifier = Modifier.size(16.dp))
        Text(stringResource(R.string.offline_badge), style = MaterialTheme.typography.bodySmall, color = Tokens.GreenDark)
    }
}

@Composable
private fun Waveform(levels: List<Float>, active: Boolean) {
    Row(
        Modifier.height(44.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        val bars = RecitationViewModel.WAVE_BARS
        val padded = List(bars - levels.size.coerceAtMost(bars)) { 0f } + levels.takeLast(bars)
        padded.forEach { level ->
            val h = if (active) 6f + level * 36f else 6f
            Box(Modifier.width(4.dp).height(h.dp).background(Tokens.Green, RoundedCornerShape(2.dp)))
        }
    }
}

@Composable
private fun ErrorPanel(error: ErrorInfo) {
    Column(
        Modifier.fillMaxWidth()
            .background(Tokens.ErrorFill, RoundedCornerShape(16.dp))
            .border(1.5.dp, Tokens.Error, RoundedCornerShape(16.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(22.dp).border(2.dp, Tokens.Error, CircleShape),
                contentAlignment = Alignment.Center
            ) { Text("!", color = Tokens.Error, fontWeight = FontWeight.Bold, fontSize = 13.sp) }
            Spacer(Modifier.width(8.dp))
            Text(
                stringResource(R.string.error_title),
                color = Tokens.ErrorText,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            Text(
                stringResource(
                    R.string.error_position,
                    ArabicDigits.fromInt(error.verse),
                    ArabicDigits.fromInt(error.wordInVerse)
                ),
                style = MaterialTheme.typography.bodySmall,
                color = Tokens.Muted
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.error_heard), color = Tokens.Muted, style = MaterialTheme.typography.bodyMedium)
            Text(error.heard, fontSize = 22.sp, color = Tokens.ErrorText, fontWeight = FontWeight.Bold)
        }
        Text(stringResource(R.string.error_retry), style = MaterialTheme.typography.bodyMedium, color = Tokens.Ink)
    }
}

@Composable
private fun Controls(
    listening: Boolean,
    speaking: Boolean,
    enabled: Boolean,
    onHint: () -> Unit,
    onToggle: () -> Unit,
    onFinish: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // First child sits at the right in RTL: hint, microphone, finish (like the design).
        SideButton(R.string.hint, enabled, onHint) {
            Icon(painterResource(R.drawable.ic_bulb), null, tint = Tokens.Ink)
        }
        val pulse by animateFloatAsState(if (listening && speaking) 1.08f else 1f, label = "mic-pulse")
        Surface(
            onClick = onToggle,
            enabled = enabled,
            shape = CircleShape,
            color = Color(0xFFCFE3D6),
            modifier = Modifier.size(96.dp).scale(pulse)
        ) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Box(
                    Modifier.size(76.dp).background(if (listening) Tokens.Green else Tokens.Muted, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painterResource(R.drawable.ic_mic),
                        contentDescription = stringResource(if (listening) R.string.pause_listening else R.string.resume_listening),
                        tint = Color.White,
                        modifier = Modifier.size(34.dp)
                    )
                }
            }
        }
        SideButton(R.string.finish, enabled, onFinish) {
            Box(Modifier.size(18.dp).background(Tokens.Ink, RoundedCornerShape(3.dp)))
        }
    }
}

@Composable
private fun SideButton(labelRes: Int, enabled: Boolean, onClick: () -> Unit, icon: @Composable () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Surface(
            onClick = onClick,
            enabled = enabled,
            shape = CircleShape,
            color = Tokens.Card,
            border = BorderStroke(1.dp, Tokens.GoldLight),
            modifier = Modifier.size(56.dp)
        ) { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { icon() } }
        Text(stringResource(labelRes), style = MaterialTheme.typography.bodySmall, color = Tokens.Ink)
    }
}
