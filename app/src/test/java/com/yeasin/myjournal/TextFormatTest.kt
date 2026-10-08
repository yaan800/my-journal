package com.yeasin.myjournal

import com.yeasin.myjournal.util.TextFormat
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

class TextFormatTest {

    @Test fun previewStripsMarkdown() {
        val md = "**Bold** and *italic* with [a link](https://x.y)\n\n- first\n- second"
        assertEquals("Bold and italic with a link\n\u2022 first\n\u2022 second", TextFormat.preview(md))
    }

    @Test fun previewKeepsBangla() {
        assertEquals("সিমেন্টের চাহিদা কম হতে পারে", TextFormat.preview("সিমেন্টের চাহিদা কম হতে পারে"))
    }

    private val zone = ZoneId.of("Asia/Dhaka")
    private val today = LocalDate.of(2026, 10, 8)
    private fun at(y: Int, m: Int, d: Int, h: Int, min: Int) = ZonedDateTime.of(y, m, d, h, min, 0, 0, zone).toInstant().toEpochMilli()

    @Test fun whenLabelToday() = assertEquals("Today \u00b7 11:42 AM", TextFormat.whenLabel(at(2026, 10, 8, 11, 42), zone, today))
    @Test fun whenLabelYesterday() = assertEquals("Yesterday \u00b7 4:20 PM", TextFormat.whenLabel(at(2026, 10, 7, 16, 20), zone, today))
    @Test fun whenLabelThisYear() = assertEquals("6 Oct", TextFormat.whenLabel(at(2026, 10, 6, 9, 0), zone, today))
    @Test fun whenLabelOlderYear() = assertEquals("6 Oct 2025", TextFormat.whenLabel(at(2025, 10, 6, 9, 0), zone, today))
}
