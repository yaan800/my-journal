package com.yeasin.myjournal.util

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Small pure helpers, unit-tested in TextFormatTest. */
object TextFormat {

    private val linkRe = Regex("""\[([^\]]+)\]\([^)]*\)""")
    private val boldRe = Regex("""\*\*(.+?)\*\*""")
    private val italicRe = Regex("""\*(.+?)\*""")
    private val bulletRe = Regex("""(?m)^\s*[-*]\s+""")
    private val blankLinesRe = Regex("""\n{2,}""")

    /** Card preview: Markdown markers removed, bullets shown as •, blank lines collapsed. */
    fun preview(text: String): String =
        text.replace(linkRe, "\$1")
            .replace(boldRe, "\$1")
            .replace(italicRe, "\$1")
            .replace(bulletRe, "\u2022 ")
            .replace(blankLinesRe, "\n")
            .trim()

    private val clock = DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH)
    private val dayMonth = DateTimeFormatter.ofPattern("d MMM", Locale.ENGLISH)
    private val dayMonthYear = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH)

    /** "Today · 11:42 AM", "Yesterday · 4:20 PM", "6 Oct", or "6 Oct 2025" for older years. */
    fun whenLabel(epochMillis: Long, zone: ZoneId = ZoneId.systemDefault(), today: LocalDate = LocalDate.now(zone)): String {
        val dt = Instant.ofEpochMilli(epochMillis).atZone(zone)
        val date = dt.toLocalDate()
        return when {
            date == today -> "Today \u00b7 " + dt.format(clock)
            date == today.minusDays(1) -> "Yesterday \u00b7 " + dt.format(clock)
            date.year == today.year -> dt.format(dayMonth)
            else -> dt.format(dayMonthYear)
        }
    }

    fun clockLabel(epochMillis: Long, zone: ZoneId = ZoneId.systemDefault()): String =
        Instant.ofEpochMilli(epochMillis).atZone(zone).format(clock)
}
