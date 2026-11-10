package com.tasmee3.app.ui.reader

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import com.tasmee3.app.mushaf.ArabicDigits
import com.tasmee3.app.mushaf.LineFitter
import com.tasmee3.app.mushaf.MushafLine
import com.tasmee3.app.mushaf.MushafMeta
import com.tasmee3.app.mushaf.MushafPage
import com.tasmee3.app.mushaf.MushafWord
import com.tasmee3.app.ui.theme.Cairo
import com.tasmee3.app.ui.theme.Tokens
import com.tasmee3.app.ui.theme.UthmanicHafs

private const val REF_PX = 100f

private data class LinePlan(val sizePx: Float, val centered: Boolean)

/** One mushaf page in the double gold frame. Lines come from data; text never wraps. */
@Composable
fun MushafPageCard(page: MushafPage?, meta: MushafMeta?, modifier: Modifier = Modifier) {
    val outer = RoundedCornerShape(14.dp)
    val inner = RoundedCornerShape(10.dp)
    Box(
        modifier
            .fillMaxSize()
            .background(Tokens.Card, outer)
            .border(3.dp, Tokens.Gold, outer)
            .padding(5.dp)
            .border(1.dp, Tokens.GoldLight, inner)
            .padding(horizontal = 10.dp, vertical = 8.dp)
    ) {
        if (page != null && meta != null) PageLines(page, meta)
    }
}

@Composable
private fun PageLines(page: MushafPage, meta: MushafMeta) {
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val basmala = remember(meta) { meta.basmala.split(' ').map { MushafWord("", it, 0) } }
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val availPx = constraints.maxWidth.toFloat()
        val slotPx = constraints.maxHeight.toFloat() / page.lines.size.coerceAtLeast(1)
        val maxSizePx = minOf(slotPx * 0.58f, with(density) { 26.dp.toPx() })
        val plans = remember(page.number, availPx, slotPx, density.density) {
            page.lines.map { planLine(it, basmala, measurer, density, availPx, maxSizePx) }
        }
        Column(Modifier.fillMaxSize()) {
            page.lines.forEachIndexed { i, line ->
                Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    when (line) {
                        is MushafLine.SurahHeader -> HeaderBanner(line.text, plans[i].sizePx, density)
                        is MushafLine.Basmala -> WordsRow(basmala, plans[i], density)
                        is MushafLine.Words -> WordsRow(line.words, plans[i], density)
                    }
                }
            }
        }
    }
}

private fun planLine(
    line: MushafLine,
    basmala: List<MushafWord>,
    measurer: TextMeasurer,
    density: Density,
    availPx: Float,
    maxSizePx: Float,
): LinePlan {
    val refStyle = TextStyle(fontFamily = UthmanicHafs, fontSize = with(density) { REF_PX.toSp() })
    fun width(text: String): Float =
        measurer.measure(text = AnnotatedString(text), style = refStyle, softWrap = false, maxLines = 1)
            .size.width.toFloat()

    if (line is MushafLine.SurahHeader) {
        val bannerInner = availPx * 0.82f * 0.9f
        return LinePlan(minOf(maxSizePx, bannerInner / (width(line.text) / REF_PX)), true)
    }
    val words = if (line is MushafLine.Words) line.words else basmala
    val wordsWidth = words.filter { it.isSpoken }.fold(0f) { acc, w -> acc + width(w.text) }
    val marks = words.count { it.verseEnd > 0 }
    val fit = LineFitter.fitSize(availPx, wordsWidth, REF_PX, words.size, marks)
    val size = minOf(maxSizePx, fit)
    val natural = size * LineFitter.emWidth(wordsWidth, REF_PX, words.size, marks)
    return LinePlan(size, LineFitter.isCentered(natural, availPx))
}

@Composable
private fun HeaderBanner(text: String, sizePx: Float, density: Density) {
    val shape = RoundedCornerShape(8.dp)
    Box(
        Modifier
            .fillMaxWidth(0.82f)
            .fillMaxHeight(0.84f)
            .background(Tokens.GoldHeader, shape)
            .border(1.dp, Tokens.Gold, shape),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontFamily = UthmanicHafs,
            fontSize = with(density) { sizePx.toSp() },
            color = Tokens.Ink,
            maxLines = 1,
            softWrap = false
        )
    }
}

@Composable
private fun WordsRow(words: List<MushafWord>, plan: LinePlan, density: Density) {
    val sizeSp = with(density) { plan.sizePx.toSp() }
    val markDp = with(density) { (plan.sizePx * LineFitter.MARK_EM).toDp() }
    val gapDp = with(density) { (plan.sizePx * 0.4f).toDp() }
    val tinyDp = with(density) { (plan.sizePx * 0.1f).toDp() }
    Row(
        modifier = Modifier.fillMaxWidth().wrapContentHeight(unbounded = true),
        horizontalArrangement = if (plan.centered) {
            Arrangement.spacedBy(gapDp, Alignment.CenterHorizontally)
        } else {
            Arrangement.SpaceBetween
        },
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (w in words) {
            // Keep a word and its verse mark together so justification never separates them.
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (w.isSpoken) WordText(w.text, sizeSp)
                if (w.isSpoken && w.verseEnd > 0) Spacer(Modifier.width(tinyDp))
                if (w.verseEnd > 0) VerseMark(w.verseEnd, markDp, plan.sizePx, density)
            }
        }
    }
}

@Composable
private fun WordText(text: String, size: TextUnit) {
    Text(
        text = text,
        fontFamily = UthmanicHafs,
        fontSize = size,
        color = Tokens.Ink,
        maxLines = 1,
        softWrap = false
    )
}

@Composable
private fun VerseMark(number: Int, widthDp: androidx.compose.ui.unit.Dp, sizePx: Float, density: Density) {
    Box(Modifier.width(widthDp), contentAlignment = Alignment.Center) {
        Box(
            Modifier.size(widthDp * 0.88f).border(1.dp, Tokens.Gold, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = ArabicDigits.fromInt(number),
                fontFamily = Cairo,
                fontWeight = FontWeight.Medium,
                fontSize = with(density) { (sizePx * 0.42f).toSp() },
                color = Tokens.GoldMarkerText,
                maxLines = 1,
                softWrap = false
            )
        }
    }
}
