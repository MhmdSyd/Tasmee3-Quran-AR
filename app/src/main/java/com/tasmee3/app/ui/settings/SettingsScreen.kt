package com.tasmee3.app.ui.settings

import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tasmee3.app.R
import com.tasmee3.app.match.RevealMode
import com.tasmee3.app.settings.SettingsRepository
import com.tasmee3.app.speech.SherpaCtcModel
import com.tasmee3.app.ui.theme.Tokens
import kotlin.math.roundToInt

@Composable
fun SettingsScreen(onBack: () -> Unit, onOpenModelTest: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val repo = remember { SettingsRepository.get(context) }
    val settings by repo.settings.collectAsState()
    val modelMb = remember { modelSizeMb(context) }

    Column(modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().height(64.dp).padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = stringResource(R.string.back), tint = Tokens.Ink)
            }
            Text(
                stringResource(R.string.settings_title),
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Tokens.Ink
            )
        }
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            SectionTitle(R.string.settings_reveal_title)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ChoiceChip(stringResource(R.string.reveal_word), settings.revealMode == RevealMode.WORD, true, Modifier.weight(1f)) {
                    repo.update { it.copy(revealMode = RevealMode.WORD) }
                }
                ChoiceChip(stringResource(R.string.reveal_verse), settings.revealMode == RevealMode.VERSE, true, Modifier.weight(1f)) {
                    repo.update { it.copy(revealMode = RevealMode.VERSE) }
                }
            }
            Text(stringResource(R.string.reveal_apply_next), style = MaterialTheme.typography.bodySmall, color = Tokens.Muted)

            SectionTitle(R.string.settings_strictness_title)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ChoiceChip(stringResource(R.string.strict_letters), true, true, Modifier.weight(1f)) { }
                // Disabled on purpose: the current model does not output vowel marks.
                ChoiceChip(stringResource(R.string.strict_diacritics), false, false, Modifier.weight(1f)) { }
            }
            Text(stringResource(R.string.strict_note), style = MaterialTheme.typography.bodySmall, color = Tokens.Muted)

            Column(
                Modifier.fillMaxWidth()
                    .background(Tokens.Card, RoundedCornerShape(20.dp))
                    .border(1.dp, Tokens.GoldLight, RoundedCornerShape(20.dp))
                    .padding(horizontal = 16.dp)
            ) {
                SwitchRow(stringResource(R.string.auto_hint), settings.autoHint) { v -> repo.update { it.copy(autoHint = v) } }
                HorizontalDivider(color = Tokens.GoldLight)
                SwitchRow(stringResource(R.string.show_after_second), settings.revealAfterSecondTry) { v ->
                    repo.update { it.copy(revealAfterSecondTry = v) }
                }
                HorizontalDivider(color = Tokens.GoldLight)
                SwitchRow(stringResource(R.string.error_sound), settings.errorSound) { v -> repo.update { it.copy(errorSound = v) } }
            }

            Column(
                Modifier.fillMaxWidth()
                    .background(Tokens.GreenTint, RoundedCornerShape(20.dp))
                    .border(1.dp, Tokens.GreenBorder, RoundedCornerShape(20.dp))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(Modifier.size(22.dp).border(2.dp, Tokens.GreenDark, CircleShape), contentAlignment = Alignment.Center) {
                        Icon(Icons.Filled.Check, null, tint = Tokens.GreenDark, modifier = Modifier.size(14.dp))
                    }
                    Text(stringResource(R.string.model_card_title), fontWeight = FontWeight.Bold, color = Tokens.GreenDark)
                }
                Text(
                    if (modelMb != null) stringResource(R.string.model_card_body, modelMb.toString())
                    else stringResource(R.string.model_missing),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Tokens.GreenDark
                )
            }

            Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.riwaya_label), color = Tokens.Ink, modifier = Modifier.weight(1f))
                Text(stringResource(R.string.riwaya_value), color = Tokens.Muted)
            }

            OutlinedButton(onClick = onOpenModelTest, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                Text(stringResource(R.string.developer_test))
            }
        }
    }
}

@Composable
private fun SectionTitle(res: Int) =
    Text(stringResource(res), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Tokens.Ink)

@Composable
private fun ChoiceChip(text: String, selected: Boolean, enabled: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(28.dp),
        color = if (selected) Tokens.GreenTint else Tokens.Card,
        border = BorderStroke(if (selected) 2.dp else 1.dp, if (selected) Tokens.Green else Tokens.GoldLight),
        modifier = modifier.heightIn(min = 56.dp)
    ) {
        Box(Modifier.fillMaxWidth().padding(vertical = 14.dp), contentAlignment = Alignment.Center) {
            Text(
                text,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                color = when {
                    !enabled -> Tokens.Muted.copy(alpha = 0.5f)
                    selected -> Tokens.GreenDark
                    else -> Tokens.Ink
                }
            )
        }
    }
}

@Composable
private fun SwitchRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().heightIn(min = 64.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, modifier = Modifier.weight(1f), color = Tokens.Ink)
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(
                checkedTrackColor = Tokens.Green,
                checkedThumbColor = Color.White,
                uncheckedTrackColor = Tokens.GoldHeader,
                uncheckedThumbColor = Tokens.Muted,
                uncheckedBorderColor = Tokens.Muted
            )
        )
    }
}

/** Size of the bundled model files in MB, or null when they are not in the app. */
private fun modelSizeMb(context: Context): Int? {
    val assets = context.assets
    if (!SherpaCtcModel.isInstalled(assets)) return null
    var total = 0L
    for (f in SherpaCtcModel.REQUIRED_FILES) {
        val path = "${SherpaCtcModel.DIR}/$f"
        total += runCatching { assets.openFd(path).use { it.length } }
            .getOrElse { assets.open(path).use { s -> s.available().toLong() } }
    }
    return (total / 1_048_576f).roundToInt()
}
