package com.rafiqalsalah.domain

import com.rafiqalsalah.data.DailyRecord
import com.rafiqalsalah.data.PrayerEntry
import com.rafiqalsalah.data.RawatibEntry

object PrayerStatus {
    const val UNRECORDED = "UNRECORDED"
    const val CONGREGATION = "CONGREGATION"
    const val SOLO = "SOLO"
    const val QADA = "QADA"
}

data class PrayerEvaluation(
    val recordedCount: Int,
    val congregationCount: Int,
    val soloCount: Int,
    val qadaNames: List<String>,
    val isComplete: Boolean,
    val title: String,
    val description: String
)

object PrayerLogic {
    val prayerNames = listOf("الفجر", "الظهر", "العصر", "المغرب", "العشاء")
    private val scores = mapOf(PrayerStatus.CONGREGATION to 66, PrayerStatus.SOLO to 46, PrayerStatus.QADA to 26, PrayerStatus.UNRECORDED to 0)

    fun evaluate(entries: List<PrayerEntry>): PrayerEvaluation {
        val byId = (0..4).associateWith { id -> entries.firstOrNull { it.prayerId == id }?.status ?: PrayerStatus.UNRECORDED }
        val recorded = byId.values.count { it != PrayerStatus.UNRECORDED }
        val congregation = byId.values.count { it == PrayerStatus.CONGREGATION }
        val solo = byId.values.count { it == PrayerStatus.SOLO }
        val qada = byId.filterValues { it == PrayerStatus.QADA }.keys.sorted().map { prayerNames[it] }
        if (recorded < 5) return PrayerEvaluation(recorded, congregation, solo, qada, false, "لسه التسجيل مش مكتمل", "سجلت $recorded من 5 فروض. الصلاة غير المسجلة ليست محسوبة تلقائيًا قضاءً.")
        if (qada.isNotEmpty()) return PrayerEvaluation(5, congregation, solo, qada, true, "فيه صلاة قضاء", "الصلوات المسجلة قضاء: ${qada.joinToString("، ")}. جماعة: $congregation، منفردًا: $solo.")
        if (congregation == 5) return PrayerEvaluation(5, congregation, solo, emptyList(), true, "كويس جدًا 🌟", "أديت الفروض الخمسة كلها جماعة. استمر على المحافظة على الصلاة")
        if (solo == 5) return PrayerEvaluation(5, congregation, solo, emptyList(), true, "كويس 👍", "أديت الفروض الخمسة. حاول تحافظ على الصلاة في وقتها وتدرك الجماعة لما تقدر")
        return PrayerEvaluation(5, congregation, solo, emptyList(), true, "أحسنت، وفيه فرصة للتحسين", "جماعة: $congregation، منفردًا: $solo. حاول تحافظ على الصلاة في وقتها وتحسن انتظامك.")
    }

    fun prayerPoints(entries: List<PrayerEntry>): Int = entries.sumOf { scores[it.status] ?: 0 }

    fun rawatibPoints(entries: List<RawatibEntry>): Int {
        // Five groups: before Fajr 10, before Dhuhr 20, after Dhuhr 10, after Maghrib 10, after Isha 10.
        val weights = mapOf(0 to 10, 1 to 20, 2 to 10, 3 to 10, 4 to 10)
        return entries.filter { it.done }.sumOf { weights[it.sunnahId] ?: 0 }.coerceAtMost(60)
    }

    fun extraPoints(day: DailyRecord, rawatib: List<RawatibEntry>): Int {
        val qiyam = ((day.qiyamRakat.coerceAtLeast(0) / 2) * 10).coerceAtMost(80) + if (day.witrDone) 20 else 0
        val qiyamTotal = qiyam.coerceAtMost(100)
        val nafl = (day.naflRakat.coerceAtLeast(0) / 2) * 5
        val fixed = (if (day.duhaDone) 6 else 0) + (if (day.asrSunnahDone) 4 else 0)
        return qiyamTotal + rawatibPoints(rawatib) + nafl + fixed
    }

    fun totalPoints(day: DailyRecord, prayers: List<PrayerEntry>, rawatib: List<RawatibEntry>): Int =
        prayerPoints(prayers) + extraPoints(day, rawatib)

    fun completionPercent(points: Int, goal: Int): Int = if (goal <= 0) 0 else ((points.coerceAtLeast(0).toDouble() / goal) * 100).toInt().coerceAtMost(100)
}
