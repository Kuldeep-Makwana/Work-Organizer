package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.DailyReportEntry
import kotlinx.coroutines.flow.Flow

@Dao
interface DailyReportEntryDao {
    @Query("SELECT * FROM daily_report_entries WHERE reportId = :reportId")
    fun getEntriesForReport(reportId: String): Flow<List<DailyReportEntry>>

    @Query("SELECT * FROM daily_report_entries WHERE reportId = :reportId")
    suspend fun getEntriesForReportSync(reportId: String): List<DailyReportEntry>

    @Query("SELECT * FROM daily_report_entries WHERE reportId = :reportId AND clientId = :clientId LIMIT 1")
    suspend fun getEntryForReportAndClient(reportId: String, clientId: String): DailyReportEntry?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntry(entry: DailyReportEntry)

    @Update
    suspend fun updateEntry(entry: DailyReportEntry)

    @Delete
    suspend fun deleteEntry(entry: DailyReportEntry)

    @Query("SELECT * FROM daily_report_entries WHERE reportId IN (:reportIds)")
    suspend fun getEntriesForReportIds(reportIds: List<String>): List<DailyReportEntry>

    @Query("""
        SELECT COUNT(DISTINCT e.clientId)
        FROM daily_report_entries e
        INNER JOIN daily_reports r ON e.reportId = r.id
        WHERE r.userId = :userId AND r.status = 'SUBMITTED'
    """)
    fun getUniqueClientsWorkedCount(userId: String): Flow<Int>
}
