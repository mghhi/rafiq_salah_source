package com.rafiqalsalah.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Dao
import androidx.room.Query
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import kotlinx.coroutines.flow.Flow

@Dao
interface RafiqDao {
    @Query("SELECT * FROM daily_records WHERE date = :date") fun observeDay(date: String): Flow<DailyRecord?>
    @Query("SELECT * FROM daily_records WHERE date = :date") suspend fun getDay(date: String): DailyRecord?
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun saveDay(day: DailyRecord)
    @Query("SELECT * FROM daily_records ORDER BY date DESC") fun observeAllDays(): Flow<List<DailyRecord>>
    @Query("SELECT * FROM daily_records WHERE date BETWEEN :start AND :end ORDER BY date") suspend fun daysBetween(start: String, end: String): List<DailyRecord>
    @Query("SELECT * FROM prayer_entries WHERE date = :date ORDER BY prayerId") fun observePrayers(date: String): Flow<List<PrayerEntry>>
    @Query("SELECT * FROM prayer_entries WHERE date = :date ORDER BY prayerId") suspend fun getPrayers(date: String): List<PrayerEntry>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun savePrayer(entry: PrayerEntry)
    @Query("DELETE FROM prayer_entries WHERE date = :date") suspend fun deletePrayersForDate(date: String)
    @Query("SELECT * FROM rawatib_entries WHERE date = :date ORDER BY sunnahId") fun observeRawatib(date: String): Flow<List<RawatibEntry>>
    @Query("SELECT * FROM rawatib_entries WHERE date = :date") suspend fun getRawatib(date: String): List<RawatibEntry>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun saveRawatib(entry: RawatibEntry)
    @Query("SELECT * FROM app_settings WHERE id = 1") fun observeSettings(): Flow<AppSettings?>
    @Query("SELECT * FROM app_settings WHERE id = 1") suspend fun getSettings(): AppSettings?
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun saveSettings(settings: AppSettings)
}

@Database(entities = [DailyRecord::class, PrayerEntry::class, RawatibEntry::class, AppSettings::class], version = 1, exportSchema = true)
abstract class AppDatabase : RoomDatabase() {
    abstract fun dao(): RafiqDao
    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null
        fun get(context: Context): AppDatabase = INSTANCE ?: synchronized(this) {
            INSTANCE ?: Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, "rafiq_salah.db").build().also { INSTANCE = it }
        }
    }
}
