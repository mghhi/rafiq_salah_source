package com.rafiqalsalah.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "daily_records")
data class DailyRecord(
    @PrimaryKey val date: String,
    val qiyamRakat: Int = 0,
    val witrDone: Boolean = false,
    val duhaDone: Boolean = false,
    val asrSunnahDone: Boolean = false,
    val naflRakat: Int = 0,
    val quranPages: Int = 0,
    val quranJuz: Int = 0,
    val quranSurahs: String = "",
    val morningAdhkar: Boolean = false,
    val eveningAdhkar: Boolean = false,
    val afterPrayerAdhkar: Boolean = false,
    val sleepAdhkar: Boolean = false,
    val extraActsJson: String = "[]",
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "prayer_entries", primaryKeys = ["date", "prayerId"])
data class PrayerEntry(
    val date: String,
    val prayerId: Int,
    val status: String = "UNRECORDED"
)

@Entity(tableName = "rawatib_entries", primaryKeys = ["date", "sunnahId"])
data class RawatibEntry(val date: String, val sunnahId: Int, val done: Boolean = false)

@Entity(tableName = "app_settings")
data class AppSettings(@PrimaryKey val id: Int = 1, val dailyGoal: Int = 500, val quranPageGoal: Int = 5)
