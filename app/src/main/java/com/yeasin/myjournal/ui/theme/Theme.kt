package com.yeasin.myjournal.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/** Colours Material 3 has no slot for: note-type labels, the Quick thought button, card borders. */
@Immutable
data class JournalColors(
    val card: Color,
    val border: Color,
    val sub: Color,
    val chip: Color,
    val chipText: Color,
    val input: Color,
    val fab: Color,
    val fabIcon: Color,
    val kBrain: Color,
    val kQuick: Color,
    val heroBg: Color,
    val heroText: Color,
    val selBg: Color,
    val projects: List<Pair<Color, Color>>,
    val isDark: Boolean,
) {
    fun project(slot: Int): Pair<Color, Color> = projects[((slot % projects.size) + projects.size) % projects.size]
}

private val LightJournal = with(LightTokens) {
    JournalColors(card, border, sub, chip, chipText, input, fab, fabIcon, kBrain, kQuick, heroBg, heroText, selBg, projects, isDark = false)
}
private val DarkJournal = with(DarkTokens) {
    JournalColors(card, border, sub, chip, chipText, input, fab, fabIcon, kBrain, kQuick, heroBg, heroText, selBg, projects, isDark = true)
}

private val LightScheme = with(LightTokens) {
    lightColorScheme(
        primary = accent, onPrimary = onAccent,
        primaryContainer = soft, onPrimaryContainer = softText,
        secondary = accent, onSecondary = onAccent,
        secondaryContainer = chip, onSecondaryContainer = chipText,
        tertiary = kBrain,
        background = bg, onBackground = text,
        surface = bg, onSurface = text,
        surfaceVariant = input, onSurfaceVariant = sub,
        surfaceContainerLowest = card, surfaceContainerLow = sheet, surfaceContainer = bar,
        surfaceContainerHigh = bar, surfaceContainerHighest = input,
        outline = border, outlineVariant = border,
        error = danger,
        inverseSurface = Color(0xFF1F3640), inverseOnSurface = Color(0xFFEAF2F4), inversePrimary = Color(0xFF8FD3DB),
    )
}

private val DarkScheme = with(DarkTokens) {
    darkColorScheme(
        primary = accent, onPrimary = onAccent,
        primaryContainer = soft, onPrimaryContainer = softText,
        secondary = accent, onSecondary = onAccent,
        secondaryContainer = chip, onSecondaryContainer = chipText,
        tertiary = kBrain,
        background = bg, onBackground = text,
        surface = bg, onSurface = text,
        surfaceVariant = input, onSurfaceVariant = sub,
        surfaceContainerLowest = card, surfaceContainerLow = sheet, surfaceContainer = bar,
        surfaceContainerHigh = bar, surfaceContainerHighest = input,
        outline = border, outlineVariant = border,
        error = danger,
        inverseSurface = text, inverseOnSurface = bar, inversePrimary = Color(0xFF1F6E78),
    )
}

private val LocalJournalColors = staticCompositionLocalOf { LightJournal }

/** Read extra colours as `Journal.colors.kQuick`. */
object Journal {
    val colors: JournalColors
        @Composable get() = LocalJournalColors.current
}

@Composable
fun MyJournalTheme(dark: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalJournalColors provides if (dark) DarkJournal else LightJournal) {
        MaterialTheme(
            colorScheme = if (dark) DarkScheme else LightScheme,
            typography = JournalTypography,
            content = content,
        )
    }
}
