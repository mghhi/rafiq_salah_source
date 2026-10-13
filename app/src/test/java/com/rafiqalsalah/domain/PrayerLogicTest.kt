package com.rafiqalsalah.domain

import com.rafiqalsalah.data.PrayerEntry
import org.junit.Assert.*
import org.junit.Test

class PrayerLogicTest {
    private fun entries(vararg statuses: String) = statuses.mapIndexed { i, s -> PrayerEntry("2026-10-13", i, s) }
    @Test fun fiveCongregationGetsVeryGood() {
        val e = PrayerLogic.evaluate(entries(*Array(5) { PrayerStatus.CONGREGATION }))
        assertTrue(e.isComplete); assertEquals("كويس جدًا 🌟", e.title); assertEquals(5, e.congregationCount)
    }
    @Test fun fiveSoloGetsGood() {
        assertEquals("كويس 👍", PrayerLogic.evaluate(entries(*Array(5) { PrayerStatus.SOLO })).title)
    }
    @Test fun qadaNeverHiddenByOtherPrayers() {
        val e = PrayerLogic.evaluate(entries(PrayerStatus.QADA, PrayerStatus.CONGREGATION, PrayerStatus.CONGREGATION, PrayerStatus.CONGREGATION, PrayerStatus.CONGREGATION))
        assertEquals("فيه صلاة قضاء", e.title); assertEquals(listOf("الفجر"), e.qadaNames)
    }
    @Test fun mixedWithoutQadaIsImprovement() {
        val e = PrayerLogic.evaluate(entries(PrayerStatus.SOLO, PrayerStatus.CONGREGATION, PrayerStatus.SOLO, PrayerStatus.CONGREGATION, PrayerStatus.CONGREGATION))
        assertEquals("أحسنت، وفيه فرصة للتحسين", e.title)
    }
    @Test fun partialDoesNotPretendUnrecordedIsQada() {
        val e = PrayerLogic.evaluate(entries(PrayerStatus.SOLO, PrayerStatus.UNRECORDED, PrayerStatus.CONGREGATION))
        assertFalse(e.isComplete); assertTrue(e.qadaNames.isEmpty()); assertTrue(e.description.contains("3 من 5"))
    }
    @Test fun percentCapsAtOneHundred() { assertEquals(100, PrayerLogic.completionPercent(700, 500)); assertEquals(64, PrayerLogic.completionPercent(320, 500)) }
}
