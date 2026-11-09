package com.tasmee3.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection

/** Design tokens from DEVELOPER_HANDOFF.md section 3. */
object Tokens {
    val Background = Color(0xFFFBF6EA)
    val Card = Color(0xFFFFFDF6)
    val Ink = Color(0xFF1F2A24)
    val Muted = Color(0xFF5B6258)

    val Green = Color(0xFF1F6B4F)
    val GreenDark = Color(0xFF17513C)
    val GreenTint = Color(0xFFE3EFE7)
    val GreenBorder = Color(0xFFB7D6C2)
    val GreenFresh = Color(0xFFD7EADD)

    val Gold = Color(0xFFB8923A)
    val GoldLight = Color(0xFFD9C48A)
    val GoldHeader = Color(0xFFF3E9CC)
    val GoldMarkerText = Color(0xFF6B5518)

    val Error = Color(0xFFA33A2B)
    val ErrorText = Color(0xFF8A2D20)
    val ErrorFill = Color(0xFFFBEAE6)
    val ErrorPill = Color(0xFFF6D9D3)

    val HiddenPill = Color(0xFFDDD4B8)
}

private val Scheme = lightColorScheme(
    primary = Tokens.Green,
    onPrimary = Tokens.Card,
    primaryContainer = Tokens.GreenTint,
    onPrimaryContainer = Tokens.GreenDark,
    background = Tokens.Background,
    onBackground = Tokens.Ink,
    surface = Tokens.Card,
    onSurface = Tokens.Ink,
    onSurfaceVariant = Tokens.Muted,
    outline = Tokens.GoldLight,
    error = Tokens.Error,
    errorContainer = Tokens.ErrorFill,
    onErrorContainer = Tokens.ErrorText,
)

/** Forces RTL for the whole app (Arabic only) and applies the design colors. */
@Composable
fun TasmeeTheme(content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        MaterialTheme(colorScheme = Scheme, content = content)
    }
}
