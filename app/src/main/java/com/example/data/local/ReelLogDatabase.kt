package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.data.model.Client
import com.example.data.model.DailyReport
import com.example.data.model.DailyReportEntry

@Database(
    entities = [
        Client::class,
        DailyReport::class,
        DailyReportEntry::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class ReelLogDatabase : RoomDatabase() {
    abstract fun clientDao(): ClientDao
    abstract fun dailyReportDao(): DailyReportDao
    abstract fun dailyReportEntryDao(): DailyReportEntryDao

    companion object {
        @Volatile
        private var INSTANCE: ReelLogDatabase? = null

        fun getDatabase(context: Context): ReelLogDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ReelLogDatabase::class.java,
                    "reellog_database"
                )
                    .fallbackToDestructiveMigration(true)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
