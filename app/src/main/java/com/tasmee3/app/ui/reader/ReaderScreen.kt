package com.tasmee3.app.ui.reader

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.tasmee3.app.R
import com.tasmee3.app.mushaf.ArabicDigits
import com.tasmee3.app.mushaf.MushafMeta
import com.tasmee3.app.mushaf.MushafPage
import com.tasmee3.app.mushaf.MushafRepository
import com.tasmee3.app.ui.theme.Tokens
import kotlinx.coroutines.launch

/** Reader: the mushaf page, top info bar, big mint button to start reciting, previous/next page. */
@Composable
fun ReaderScreen(
    initialPage: Int,
    onStartRecitation: (Int) -> Unit,
    onOpenMenu: () -> Unit,   // opens the settings screen
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

    Column(modifier.fillMaxSize().background(Tokens.MushafBg)) {
        MushafTopBar(
            page = current,
            meta = meta,
            trailing = {
                IconButton(onClick = { showJump = true }) {
                    Icon(Icons.Filled.Search, contentDescription = stringResource(R.string.goto_page), tint = Tokens.MushafInk)
                }
                IconButton(onClick = onOpenMenu) {
                    Icon(Icons.Filled.Settings, contentDescription = stringResource(R.string.settings), tint = Tokens.MushafInk)
                }
            }
        )
        if (metaResult?.isFailure == true) {
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.load_failed), color = Tokens.ErrorText)
            }
        } else {
            HorizontalPager(
                state = pager,
                modifier = Modifier.weight(1f).fillMaxWidth(),
                key = { it },
            ) { index ->
                val page by produceState<MushafPage?>(null, index) {
                    value = runCatching { repo.page(index + 1) }.getOrNull()
                }
                MushafPageCard(page, meta)
            }
        }
        // Mint button at the right (start) edge, page buttons at the left, like the reference.
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            MintButton(onClick = { onStartRecitation(pager.currentPage + 1) }) {
                Icon(painterResource(R.drawable.ic_mic), stringResource(R.string.start_recitation_cd), tint = Color.White,
                    modifier = Modifier.size(34.dp))
            }
            Spacer(Modifier.weight(1f))
            Surface(
                shape = RoundedCornerShape(26.dp), color = Color.White, shadowElevation = 2.dp,
                border = BorderStroke(1.dp, Color(0xFFE6E6E6))
            ) {
                Row(Modifier.padding(horizontal = 4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    // First child sits at the right in RTL: previous page (arrow points right).
                    IconButton(
                        onClick = { scope.launch { pager.animateScrollToPage(pager.currentPage - 1) } },
                        enabled = pager.currentPage > 0
                    ) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, stringResource(R.string.prev_page), tint = Tokens.MushafInk) }
                    IconButton(
                        onClick = { scope.launch { pager.animateScrollToPage(pager.currentPage + 1) } },
                        enabled = pager.currentPage < MushafRepository.PAGE_COUNT - 1
                    ) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, stringResource(R.string.next_page), tint = Tokens.MushafInk) }
                }
            }
        }
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
