@file:OptIn(androidx.compose.ui.text.ExperimentalTextApi::class)

package com.tasmee3.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import com.tasmee3.app.R

/** UI font (SIL OFL). Variable font, fetched by scripts/fetch_assets.sh. */
val Cairo = FontFamily(
    Font(R.font.cairo, FontWeight.Normal, variationSettings = FontVariation.Settings(FontVariation.weight(400))),
    Font(R.font.cairo, FontWeight.Medium, variationSettings = FontVariation.Settings(FontVariation.weight(500))),
    Font(R.font.cairo, FontWeight.Bold, variationSettings = FontVariation.Settings(FontVariation.weight(700))),
)

/** Quran text font (KFGQPC Uthmanic Script Hafs, unmodified). Fetched by scripts/fetch_assets.sh. */
val UthmanicHafs = FontFamily(Font(R.font.uthmanic_hafs))

private val base = Typography()

val AppTypography = Typography(
    displayLarge = base.displayLarge.copy(fontFamily = Cairo),
    displayMedium = base.displayMedium.copy(fontFamily = Cairo),
    displaySmall = base.displaySmall.copy(fontFamily = Cairo),
    headlineLarge = base.headlineLarge.copy(fontFamily = Cairo),
    headlineMedium = base.headlineMedium.copy(fontFamily = Cairo),
    headlineSmall = base.headlineSmall.copy(fontFamily = Cairo),
    titleLarge = base.titleLarge.copy(fontFamily = Cairo),
    titleMedium = base.titleMedium.copy(fontFamily = Cairo),
    titleSmall = base.titleSmall.copy(fontFamily = Cairo),
    bodyLarge = base.bodyLarge.copy(fontFamily = Cairo),
    bodyMedium = base.bodyMedium.copy(fontFamily = Cairo),
    bodySmall = base.bodySmall.copy(fontFamily = Cairo),
    labelLarge = base.labelLarge.copy(fontFamily = Cairo),
    labelMedium = base.labelMedium.copy(fontFamily = Cairo),
    labelSmall = base.labelSmall.copy(fontFamily = Cairo),
)
