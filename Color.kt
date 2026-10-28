package com.yeasin.myjournal.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Calm blue-teal on cool paper — the same tokens as prototype v0.4.
 * Every text colour meets WCAG AA (4.5:1) on the background it sits on.
 */
internal object LightTokens {
    val bg = Color(0xFFF2F6F7)
    val bar = Color(0xFFEAF1F2)
    val card = Color(0xFFFCFDFD)
    val border = Color(0xFFD9E3E6)
    val text = Color(0xFF1C2B33)
    val sub = Color(0xFF52626A)
    val chip = Color(0xFFE4EEF0)
    val chipText = Color(0xFF2A4650)
    val accent = Color(0xFF227580)
    val onAccent = Color(0xFFFFFFFF)
    val soft = Color(0xFFD8ECEE)
    val softText = Color(0xFF17535B)
    val fab = Color(0xFFCBE7EA)
    val fabIcon = Color(0xFF12505A)
    val sheet = Color(0xFFFBFDFD)
    val input = Color(0xFFE6EEF0)
    val kBrain = Color(0xFF4E5BA6)
    val kQuick = Color(0xFF8A5E0E)
    val heroBg = Color(0xFFDCEEF0)
    val heroText = Color(0xFF14505A)
    val danger = Color(0xFFB3261E)
    val selBg = Color(0xFFE1F0F2)

    /** Project colours: (tile background, text/dot colour). Each pair passes 4.5:1. */
    val projects = listOf(
        Color(0xFFD6EEF0) to Color(0xFF1B5F67),
        Color(0xFFDDE7F8) to Color(0xFF2C4F8C),
        Color(0xFFDCEFE3) to Color(0xFF2B6440),
        Color(0xFFE7E3F8) to Color(0xFF4B3F8F),
        Color(0xFFF6EBD5) to Color(0xFF76531A),
        Color(0xFFF6E1E8) to Color(0xFF86344F),
    )
}

internal object DarkTokens {
    val bg = Color(0xFF0F1A1E)
    val bar = Color(0xFF132126)
    val card = Color(0xFF16242A)
    val border = Color(0xFF2A3B42)
    val text = Color(0xFFE3ECEE)
    val sub = Color(0xFFA3B4BA)
    val chip = Color(0xFF1F3138)
    val chipText = Color(0xFFCFE0E3)
    val accent = Color(0xFF7CC6CF)
    val onAccent = Color(0xFF08343A)
    val soft = Color(0xFF1B3A40)
    val softText = Color(0xFFBFE6EA)
    val fab = Color(0xFF1F4A51)
    val fabIcon = Color(0xFFD6F1F3)
    val sheet = Color(0xFF16242A)
    val input = Color(0xFF1C2C32)
    val kBrain = Color(0xFFAFB8F2)
    val kQuick = Color(0xFFE5C07B)
    val heroBg = Color(0xFF16323A)
    val heroText = Color(0xFFCFEFF2)
    val danger = Color(0xFFF2B8B5)
    val selBg = Color(0xFF183238)

    val projects = listOf(
        Color(0xFF1E3E44) to Color(0xFF9FDCE3),
        Color(0xFF20304A) to Color(0xFFAFC6F2),
        Color(0xFF22382C) to Color(0xFFA9D9B6),
        Color(0xFF2E2A46) to Color(0xFFC7BEF2),
        Color(0xFF3A3020) to Color(0xFFE9CF9A),
        Color(0xFF3A2630) to Color(0xFFEDB7CB),
    )
}
