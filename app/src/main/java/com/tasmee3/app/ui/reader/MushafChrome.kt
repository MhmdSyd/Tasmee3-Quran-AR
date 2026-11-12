package com.tasmee3.app.ui.reader

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tasmee3.app.R
import com.tasmee3.app.mushaf.ArabicDigits
import com.tasmee3.app.mushaf.MushafMeta
import com.tasmee3.app.mushaf.MushafPage
import com.tasmee3.app.ui.theme.Tokens
import com.tasmee3.app.ui.theme.UthmanicHafs

/** Top bar of the mushaf screens: leading buttons, the page info card, trailing buttons (like the reference). */
@Composable
fun MushafTopBar(
    page: MushafPage?,
    meta: MushafMeta?,
    modifier: Modifier = Modifier,
    leading: @Composable RowScope.() -> Unit = {},
    trailing: @Composable RowScope.() -> Unit = {},
) {
    Row(
        modifier.fillMaxWidth().height(68.dp).padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        leading()
        PageInfoCard(page, meta, Modifier.weight(1f))
        trailing()
    }
}

/** Rounded grey card: surah name, then "page | juz | surah:verse". */
@Composable
fun PageInfoCard(page: MushafPage?, meta: MushafMeta?, modifier: Modifier = Modifier) {
    val name = page?.let { meta?.surahNames?.get(it.surah) }.orEmpty()
    val info = page?.let { p ->
        val v = p.firstVerse()
        val ref = if (v != null) " | " + ArabicDigits.fromInt(v.first) + ":" + ArabicDigits.fromInt(v.second) else ""
        stringResource(R.string.page_info, ArabicDigits.fromInt(p.number), ArabicDigits.fromInt(p.juz)) + ref
    }.orEmpty()
    Column(
        modifier.background(Tokens.MushafCard, RoundedCornerShape(10.dp))
            .border(1.dp, Tokens.MushafCardBorder, RoundedCornerShape(10.dp))
            .padding(horizontal = 14.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.Start   // right edge in RTL, as in the reference
    ) {
        Text(name, fontFamily = UthmanicHafs, fontWeight = FontWeight.Bold, color = Tokens.MushafInk, maxLines = 1,
            style = MaterialTheme.typography.titleMedium)
        Text(info, style = MaterialTheme.typography.bodySmall, color = Tokens.Muted, maxLines = 1)
    }
}

/** The big mint round button (record / start). [pulse] > 1 makes it breathe while speech is heard. */
@Composable
fun MintButton(onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true, pulse: Float = 1f,
               content: @Composable () -> Unit) {
    Box(modifier.size(92.dp).scale(pulse), contentAlignment = Alignment.Center) {
        Box(Modifier.fillMaxSize().background(Tokens.MintGlow.copy(alpha = 0.6f), CircleShape))
        Surface(
            onClick = onClick, enabled = enabled, shape = CircleShape,
            color = if (enabled) Tokens.Mint else Tokens.MushafLine, modifier = Modifier.size(72.dp)
        ) { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { content() } }
    }
}

/** White rounded pill button with an icon and a label (like "أخطاء" in the reference). */
@Composable
fun WhitePill(label: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true,
              icon: @Composable () -> Unit) {
    Surface(
        onClick = onClick, enabled = enabled, shape = RoundedCornerShape(26.dp), color = Color.White,
        shadowElevation = 2.dp, border = BorderStroke(1.dp, Color(0xFFE6E6E6)), modifier = modifier
    ) {
        Row(
            Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            icon()
            Text(label, fontWeight = FontWeight.Bold, color = if (enabled) Tokens.MushafInk else Tokens.Muted)
        }
    }
}
