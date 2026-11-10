package com.tasmee3.app.ui.reader

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.tasmee3.app.R
import com.tasmee3.app.mushaf.ArabicDigits
import com.tasmee3.app.mushaf.MushafMeta
import com.tasmee3.app.mushaf.MushafPage
import com.tasmee3.app.mushaf.MushafRepository
import com.tasmee3.app.ui.theme.Tokens
import kotlinx.coroutines.launch

@Composable
fun ReaderScreen(
    initialPage: Int,
    onStartRecitation: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val repo = remember { MushafRepository(context) }
    val metaResult by produceState<Result<MushafMeta>?>(null) { value = runCatching { repo.meta() } }
    val meta = metaResult?.getOrNull()
    val pager = rememberPagerState(
        initialPage = (initialPage - 1).coerceIn(0, MushafRepository.PAGE_COUNT - 1)
    ) { MushafRepository.PAGE_COUNT }
    val scope = rememberCoroutineScope()
    var showJump by remember { mutableStateOf(false) }
    val current by produceState<MushafPage?>(null, pager.currentPage) {
        value = runCatching { repo.page(pager.currentPage + 1) }.getOrNull()
    }

    Column(modifier.fillMaxSize()) {
        TopBar(
            title = current?.let { meta?.surahNames?.get(it.surah) }.orEmpty(),
            subtitle = current?.let {
                stringResource(R.string.page_label, ArabicDigits.fromInt(it.number), ArabicDigits.fromInt(it.juz))
            }.orEmpty(),
            onSearch = { showJump = true },
        )
        if (metaResult?.isFailure == true) {
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.load_failed), color = Tokens.ErrorText)
            }
        } else {
            HorizontalPager(
                state = pager,
                modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                pageSpacing = 12.dp,
                key = { it },
            ) { index ->
                val page by produceState<MushafPage?>(null, index) {
                    value = runCatching { repo.page(index + 1) }.getOrNull()
                }
                MushafPageCard(page, meta)
            }
        }
        BottomBar(
            canGoPrevious = pager.currentPage > 0,
            canGoNext = pager.currentPage < MushafRepository.PAGE_COUNT - 1,
            onPrevious = { scope.launch { pager.animateScrollToPage(pager.currentPage - 1) } },
            onNext = { scope.launch { pager.animateScrollToPage(pager.currentPage + 1) } },
            onStart = onStartRecitation,
        )
    }

    if (showJump) {
        JumpDialog(
            onDismiss = { showJump = false },
            onGo = { n ->
                showJump = false
                scope.launch { pager.scrollToPage(n - 1) }
            }
        )
    }
}

@Composable
private fun TopBar(title: String, subtitle: String, onSearch: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().height(64.dp).padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = { /* menu comes in a later step */ }) {
            Icon(Icons.Filled.Menu, contentDescription = stringResource(R.string.menu), tint = Tokens.Ink)
        }
        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Tokens.Ink)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = Tokens.Muted)
        }
        IconButton(onClick = onSearch) {
            Icon(Icons.Filled.Search, contentDescription = stringResource(R.string.goto_page), tint = Tokens.Ink)
        }
    }
}

@Composable
private fun BottomBar(
    canGoPrevious: Boolean,
    canGoNext: Boolean,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onStart: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // First child sits at the right in RTL: previous page (arrow points right, like the design).
        RoundNav(canGoPrevious, Icons.AutoMirrored.Filled.KeyboardArrowLeft, R.string.prev_page, onPrevious)
        Button(
            onClick = onStart,
            modifier = Modifier.weight(1f).height(56.dp),
            shape = RoundedCornerShape(28.dp)
        ) {
            Text(stringResource(R.string.start_recitation), fontWeight = FontWeight.Bold)
        }
        RoundNav(canGoNext, Icons.AutoMirrored.Filled.KeyboardArrowRight, R.string.next_page, onNext)
    }
}

@Composable
private fun RoundNav(enabled: Boolean, icon: ImageVector, descriptionRes: Int, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = CircleShape,
        color = Tokens.Card,
        border = BorderStroke(1.dp, Tokens.GoldLight),
        modifier = Modifier.size(56.dp)
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Icon(
                icon,
                contentDescription = stringResource(descriptionRes),
                tint = Tokens.Ink.copy(alpha = if (enabled) 1f else 0.3f)
            )
        }
    }
}

@Composable
private fun JumpDialog(onDismiss: () -> Unit, onGo: (Int) -> Unit) {
    var text by remember { mutableStateOf("") }
    val page = ArabicDigits.toInt(text)?.takeIf { it in 1..MushafRepository.PAGE_COUNT }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.goto_page)) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                label = { Text(stringResource(R.string.goto_hint)) },
            )
        },
        confirmButton = {
            TextButton(onClick = { page?.let(onGo) }, enabled = page != null) {
                Text(stringResource(R.string.go))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}
