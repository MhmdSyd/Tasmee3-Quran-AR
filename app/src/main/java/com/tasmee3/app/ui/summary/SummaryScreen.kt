package com.tasmee3.app.ui.summary

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tasmee3.app.R
import com.tasmee3.app.match.LastSession
import com.tasmee3.app.match.ReviewItem
import com.tasmee3.app.match.ReviewReason
import com.tasmee3.app.mushaf.ArabicDigits
import com.tasmee3.app.ui.theme.Cairo
import com.tasmee3.app.ui.theme.Tokens
import com.tasmee3.app.ui.theme.UthmanicHafs
import kotlin.math.roundToInt

@Composable
fun SummaryScreen(
    onRepeat: (Int) -> Unit,
    onNext: (Int) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val result = LastSession.result
    Column(modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Row(Modifier.fillMaxWidth().height(64.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onClose) {
                Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.close), tint = Tokens.Ink)
            }
            Text(
                stringResource(R.string.summary_title),
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Tokens.Ink,
                textAlign = TextAlign.Center
            )
            Box(Modifier.size(48.dp)) // balances the close button so the title stays centred
        }
        if (result == null) {
            OutlinedButton(onClick = onClose, modifier = Modifier.fillMaxWidth().height(56.dp)) { Text(stringResource(R.string.close)) }
            return@Column
        }
        val s = result.summary
        val percent = (s.accuracy * 100).roundToInt()
        Column(
            Modifier.weight(1f).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            AccuracyRing(s.accuracy.toFloat(), percent)
            Text(
                if (s.completed) stringResource(R.string.summary_done, ArabicDigits.fromInt(result.page))
                else stringResource(R.string.summary_partial, ArabicDigits.fromInt(s.total - s.notRecited), ArabicDigits.fromInt(s.total)),
                style = MaterialTheme.typography.titleMedium,
                color = Tokens.Ink,
                textAlign = TextAlign.Center
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard(s.total, R.string.stat_words, Tokens.Card, Tokens.GoldLight, Tokens.Ink, Modifier.weight(1f))
                StatCard(s.firstTryCorrect, R.string.stat_correct, Tokens.GreenTint, Tokens.GreenBorder, Tokens.GreenDark, Modifier.weight(1f))
                StatCard(s.review.size, R.string.stat_review, Tokens.ErrorFill, Color(0xFFE8B8B0), Tokens.ErrorText, Modifier.weight(1f))
            }
            Text(
                stringResource(if (s.review.isEmpty()) R.string.summary_all_good else R.string.summary_review_title),
                modifier = Modifier.fillMaxWidth(),
                fontWeight = FontWeight.Bold,
                color = Tokens.Ink
            )
            LazyColumn(Modifier.weight(1f).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(s.review) { ReviewCard(it) }
            }
        }
        Column(Modifier.padding(vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(onClick = { onRepeat(result.page) }, modifier = Modifier.fillMaxWidth().height(56.dp), shape = RoundedCornerShape(28.dp)) {
                Text(stringResource(R.string.repeat_recitation), fontWeight = FontWeight.Bold)
            }
            OutlinedButton(
                onClick = { onNext(result.page + 1) },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(28.dp),
                border = BorderStroke(1.5.dp, Tokens.Green)
            ) { Text(stringResource(R.string.next_page), color = Tokens.GreenDark) }
        }
    }
}

@Composable
private fun AccuracyRing(fraction: Float, percent: Int) {
    Box(Modifier.size(150.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = 12.dp.toPx()
            val inset = stroke / 2
            val arcSize = Size(size.width - stroke, size.height - stroke)
            drawArc(Color(0xFFE8E0C8), -90f, 360f, false, Offset(inset, inset), arcSize, style = Stroke(stroke))
            drawArc(Tokens.Green, -90f, 360f * fraction.coerceIn(0f, 1f), false, Offset(inset, inset), arcSize,
                style = Stroke(stroke, cap = StrokeCap.Round))
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                stringResource(R.string.percent_value, ArabicDigits.fromInt(percent)),
                fontFamily = Cairo, fontWeight = FontWeight.Bold, fontSize = 36.sp, color = Tokens.Ink
            )
            Text(stringResource(R.string.from_first_try), style = MaterialTheme.typography.bodySmall, color = Tokens.Muted)
        }
    }
}

@Composable
private fun StatCard(value: Int, labelRes: Int, fill: Color, border: Color, text: Color, modifier: Modifier) {
    Column(
        modifier.background(fill, RoundedCornerShape(16.dp)).border(1.dp, border, RoundedCornerShape(16.dp)).padding(vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(ArabicDigits.fromInt(value), fontFamily = Cairo, fontWeight = FontWeight.Bold, fontSize = 28.sp, color = text)
        Text(stringResource(labelRes), style = MaterialTheme.typography.bodySmall, color = text)
    }
}

@Composable
private fun ReviewCard(item: ReviewItem) {
    val parts = item.word.id.split(':')
    val verse = parts.getOrNull(1)?.toIntOrNull() ?: 0
    val wordNo = parts.getOrNull(2)?.toIntOrNull() ?: 0
    val reason = when (item.primaryReason) {
        ReviewReason.WRONG_WORD -> R.string.reason_wrong_word
        ReviewReason.SKIPPED -> R.string.reason_skipped
        ReviewReason.LONG_PAUSE -> R.string.reason_long_pause
        ReviewReason.HINTED -> R.string.reason_hinted
    }
    Row(
        Modifier.fillMaxWidth()
            .background(Tokens.Card, RoundedCornerShape(16.dp))
            .border(1.dp, Tokens.GoldLight, RoundedCornerShape(16.dp))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(item.word.text, fontFamily = UthmanicHafs, fontSize = 28.sp, color = Tokens.Ink, maxLines = 1, softWrap = false)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                stringResource(R.string.error_position, ArabicDigits.fromInt(verse), ArabicDigits.fromInt(wordNo)),
                style = MaterialTheme.typography.bodyMedium, color = Tokens.Ink
            )
            Text(stringResource(reason), style = MaterialTheme.typography.bodySmall, color = Tokens.Muted)
            item.heard?.let {
                Text(stringResource(R.string.review_heard, it), style = MaterialTheme.typography.bodySmall, color = Tokens.ErrorText)
            }
        }
    }
}
