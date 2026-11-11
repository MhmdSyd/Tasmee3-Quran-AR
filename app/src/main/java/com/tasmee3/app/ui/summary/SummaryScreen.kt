package com.tasmee3.app.ui.summary

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tasmee3.app.R
import com.tasmee3.app.match.LastSession
import com.tasmee3.app.match.ReviewReason
import com.tasmee3.app.mushaf.ArabicDigits
import com.tasmee3.app.ui.theme.Tokens
import com.tasmee3.app.ui.theme.UthmanicHafs
import kotlin.math.roundToInt

/** Basic version so a finished session can be seen. The designed summary screen comes in step 6. */
@Composable
fun SummaryScreen(onRepeat: (Int) -> Unit, onNext: (Int) -> Unit, onClose: () -> Unit, modifier: Modifier = Modifier) {
    val result = LastSession.result
    Column(
        modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(stringResource(R.string.summary_title), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        if (result == null) {
            Button(onClick = onClose, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text(stringResource(R.string.close)) }
            return@Column
        }
        val s = result.summary
        Text(
            stringResource(R.string.summary_accuracy, ArabicDigits.fromInt((s.accuracy * 100).roundToInt())),
            style = MaterialTheme.typography.headlineMedium,
            color = Tokens.Green,
            fontWeight = FontWeight.Bold
        )
        Text(
            stringResource(
                R.string.summary_counts,
                ArabicDigits.fromInt(s.total), ArabicDigits.fromInt(s.firstTryCorrect), ArabicDigits.fromInt(s.review.size)
            ),
            color = Tokens.Muted
        )
        if (s.review.isNotEmpty()) {
            Text(stringResource(R.string.summary_review_title), fontWeight = FontWeight.Bold)
            LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                items(s.review) { item ->
                    val verse = item.word.id.split(':').getOrNull(1)?.toIntOrNull() ?: 0
                    val reason = when (item.primaryReason) {
                        ReviewReason.WRONG_WORD -> R.string.reason_wrong_word
                        ReviewReason.SKIPPED -> R.string.reason_skipped
                        ReviewReason.LONG_PAUSE -> R.string.reason_long_pause
                        ReviewReason.HINTED -> R.string.reason_hinted
                    }
                    Column(Modifier.fillMaxWidth()) {
                        Text(item.word.text, fontFamily = UthmanicHafs, fontSize = 24.sp)
                        Text(
                            stringResource(R.string.summary_verse, ArabicDigits.fromInt(verse)) + " · " + stringResource(reason),
                            style = MaterialTheme.typography.bodySmall, color = Tokens.Muted
                        )
                    }
                }
            }
        } else {
            Text("", Modifier.weight(1f))
        }
        Button(onClick = { onRepeat(result.page) }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
            Text(stringResource(R.string.repeat_recitation))
        }
        OutlinedButton(onClick = { onNext(result.page + 1) }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
            Text(stringResource(R.string.next_page))
        }
        OutlinedButton(onClick = onClose, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
            Text(stringResource(R.string.close))
        }
    }
}
