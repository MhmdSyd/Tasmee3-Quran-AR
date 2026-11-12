package com.tasmee3.app.ui.reader

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import com.tasmee3.app.match.AllRevealed
import com.tasmee3.app.match.WordDisplay
import com.tasmee3.app.match.WordDisplays
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

private class LinePlan(
    val sizePx: Float,
    val centered: Boolean,
    val widthsPx: FloatArray = FloatArray(0),   // width of every word at the chosen size
    val gapPx: Float = 0f,                      // space between words (so hidden lines can run on unbroken)
)

private class Measured(val words: List<MushafWord>, val widthsRef: FloatArray, val marks: Int, val fit: Float)

/**
 * One page of the Madani mushaf (1405 print) in the reading style of the reference screenshots:
 * plain light page, black text, ornamental surah banner, ringed verse numbers. Lines come from data;
 * text never wraps. While reciting, a hidden word is a thin line and the newest word is blue.
 */
@Composable
fun MushafPageCard(
    page: MushafPage?,
    meta: MushafMeta?,
    modifier: Modifier = Modifier,
    displays: WordDisplays = AllRevealed,
) {
    Box(modifier.fillMaxSize().background(Tokens.MushafBg)) {
        // The pair of thin lines along the page edge, like the printed mushaf.
        Canvas(Modifier.fillMaxHeight().width(8.dp).align(Alignment.CenterEnd)) {
            val w = 1.2.dp.toPx()
            drawLine(Tokens.MushafLine, Offset(w, 0f), Offset(w, size.height), w)
            drawLine(Tokens.MushafLine, Offset(w * 4f, 0f), Offset(w * 4f, size.height), w)
        }
        if (page != null && meta != null) {
            Box(Modifier.fillMaxSize().padding(start = 6.dp, end = 12.dp, top = 2.dp, bottom = 2.dp)) {
                PageLines(page, meta, displays)
            }
        }
    }
}

@Composable
private fun PageLines(page: MushafPage, meta: MushafMeta, displays: WordDisplays) {
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val basmala = remember(meta) { meta.basmala.split(' ').map { MushafWord("", it, 0) } }
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val availPx = constraints.maxWidth.toFloat()
        val slotPx = constraints.maxHeight.toFloat() / page.lines.size.coerceAtLeast(1)
        val maxSizePx = minOf(slotPx * 0.62f, with(density) { 28.dp.toPx() })
        val plans = remember(page.number, availPx, slotPx, density.density) {
            planPage(page, basmala, measurer, density, availPx, maxSizePx)
        }
        Column(Modifier.fillMaxSize()) {
            page.lines.forEachIndexed { i, line ->
                Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    when (line) {
                        is MushafLine.SurahHeader -> HeaderBanner(line.text, plans[i].sizePx, density)
                        is MushafLine.Basmala -> WordsRow(basmala, plans[i], AllRevealed, density)
                        is MushafLine.Words -> WordsRow(line.words, plans[i], displays, density)
                    }
                }
            }
        }
    }
}

/** Two passes: measure every line, then size them so loose lines stay close to the page's typical size. */
private fun planPage(
    page: MushafPage,
    basmala: List<MushafWord>,
    measurer: TextMeasurer,
    density: Density,
    availPx: Float,
    maxSizePx: Float,
): List<LinePlan> {
    val refStyle = TextStyle(fontFamily = UthmanicHafs, fontSize = with(density) { REF_PX.toSp() })
    fun width(text: String): Float =
        measurer.measure(text = AnnotatedString(text), style = refStyle, softWrap = false, maxLines = 1)
            .size.width.toFloat()

    val measured: List<Measured?> = page.lines.map { line ->
        val words = when (line) {
            is MushafLine.SurahHeader -> return@map null
            is MushafLine.Basmala -> basmala
            is MushafLine.Words -> line.words
        }
        val widths = FloatArray(words.size) { k -> if (words[k].isSpoken) width(words[k].text) else 0f }
        val marks = words.count { it.verseEnd > 0 }
        Measured(words, widths, marks, LineFitter.fitSize(availPx, widths.sum(), REF_PX, words.size, marks))
    }
    val cap = LineFitter.uniformCap(maxSizePx, LineFitter.median(measured.filterNotNull().map { it.fit }))

    return page.lines.mapIndexed { i, line ->
        val m = measured[i]
        if (m == null) {
            val title = (line as MushafLine.SurahHeader).text
            val inner = availPx * 0.52f
            LinePlan(minOf(maxSizePx, inner / (width(title) / REF_PX)), true)
        } else {
            val size = minOf(cap, m.fit)
            val items = m.words.size
            val natural = size * LineFitter.emWidth(m.widthsRef.sum(), REF_PX, items, m.marks)
            val centered = LineFitter.isCentered(natural, availPx)
            val widths = FloatArray(items) { k -> m.widthsRef[k] * size / REF_PX }
            val itemsWidth = widths.sum() + m.marks * (LineFitter.MARK_EM + 0.1f) * size
            val gap = if (centered) size * 0.4f else LineFitter.justifiedGap(availPx, itemsWidth, items)
            LinePlan(size, centered, widths, gap)
        }
    }
}

/** Black double frame with a round ornament at each end and the surah name in the middle. */
@Composable
private fun HeaderBanner(text: String, sizePx: Float, density: Density) {
    val ink = Tokens.MushafInk
    Row(
        Modifier.fillMaxWidth().fillMaxHeight(0.84f)
            .border(2.dp, ink, RoundedCornerShape(3.dp)).padding(3.dp)
            .border(1.dp, ink, RoundedCornerShape(2.dp)).padding(horizontal = 6.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Ornament()
        Box(
            Modifier.weight(1f).fillMaxHeight().border(1.dp, ink, RoundedCornerShape(50)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                fontFamily = UthmanicHafs,
                fontSize = with(density) { sizePx.toSp() },
                color = ink,
                maxLines = 1,
                softWrap = false
            )
        }
        Ornament()
    }
}

@Composable
private fun Ornament() {
    Box(
        Modifier.fillMaxHeight().aspectRatio(1f)
            .border(1.dp, Tokens.MushafInk, CircleShape).padding(3.dp)
            .border(1.dp, Tokens.MushafInk, CircleShape)
    )
}

@Composable
private fun WordsRow(words: List<MushafWord>, plan: LinePlan, displays: WordDisplays, density: Density) {
    val sizeSp = with(density) { plan.sizePx.toSp() }
    val markDp = with(density) { (plan.sizePx * LineFitter.MARK_EM).toDp() }
    val gapDp = with(density) { (plan.sizePx * 0.4f).toDp() }
    val tinyDp = with(density) { (plan.sizePx * 0.1f).toDp() }
    val cellHeight = with(density) { plan.sizePx.toDp() }
    Row(
        modifier = Modifier.fillMaxWidth().wrapContentHeight(unbounded = true),
        horizontalArrangement = if (plan.centered) {
            Arrangement.spacedBy(gapDp, Alignment.CenterHorizontally)
        } else {
            Arrangement.SpaceBetween
        },
        verticalAlignment = Alignment.CenterVertically
    ) {
        words.forEachIndexed { k, w ->
            // Keep a word and its verse number together so justification never separates them.
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (w.isSpoken) {
                    val widthDp = with(density) { plan.widthsPx.getOrElse(k) { 0f }.toDp() }
                    WordCell(w.text, displays.display(w), sizeSp, widthDp, cellHeight, plan.gapPx)
                }
                if (w.isSpoken && w.verseEnd > 0) Spacer(Modifier.width(tinyDp))
                if (w.verseEnd > 0) VerseMark(w.verseEnd, markDp, plan.sizePx, density)
            }
        }
    }
}

@Composable
private fun WordCell(text: String, display: WordDisplay, fontSize: TextUnit, widthDp: Dp, heightDp: Dp, gapPx: Float) {
    when (display) {
        WordDisplay.REVEALED -> WordText(text, fontSize, Tokens.MushafInk)
        WordDisplay.FRESH -> WordText(text, fontSize, Tokens.MushafNewest)
        // A hidden word is a thin line under where it will appear; neighbours' lines join into one.
        WordDisplay.HIDDEN -> Box(
            Modifier.width(widthDp).height(heightDp).drawBehind {
                val y = size.height * 0.72f
                drawLine(Tokens.MushafLine, Offset(-gapPx / 2f, y), Offset(size.width + gapPx / 2f, y), 1.4.dp.toPx())
            }
        )
        // Not colour alone: a thick outline plus the mistake panel under the page.
        WordDisplay.ERROR -> Box(
            Modifier.width(widthDp).height(heightDp * 0.6f)
                .background(Tokens.ErrorPill, RoundedCornerShape(50))
                .border(2.dp, Tokens.Error, RoundedCornerShape(50))
        )
    }
}

@Composable
private fun WordText(text: String, size: TextUnit, color: androidx.compose.ui.graphics.Color) {
    Text(text = text, fontFamily = UthmanicHafs, fontSize = size, color = color, maxLines = 1, softWrap = false)
}

/** Verse number in a double ring, as in the printed mushaf. Always shown, even while words are hidden. */
@Composable
private fun VerseMark(number: Int, widthDp: Dp, sizePx: Float, density: Density) {
    Box(Modifier.width(widthDp), contentAlignment = Alignment.Center) {
        Box(
            Modifier.size(widthDp * 0.92f).border(1.2.dp, Tokens.MushafInk, CircleShape).padding(2.dp)
                .border(0.8.dp, Tokens.MushafInk, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = ArabicDigits.fromInt(number),
                fontFamily = Cairo,
                fontWeight = FontWeight.Bold,
                fontSize = with(density) { (sizePx * 0.40f).toSp() },
                color = Tokens.MushafInk,
                maxLines = 1,
                softWrap = false
            )
        }
    }
}
