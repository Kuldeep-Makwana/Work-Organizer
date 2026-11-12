package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.DailyReport
import kotlinx.coroutines.flow.Flow

@Dao
interface DailyReportDao {
    @Query("SELECT * FROM daily_reports WHERE userId = :userId AND workDate = :workDate LIMIT 1")
    fun getReportByDate(userId: String, workDate: String): Flow<DailyReport?>

    @Query("SELECT * FROM daily_reports WHERE userId = :userId AND workDate = :workDate LIMIT 1")
    suspend fun getReportByDateSync(userId: String, workDate: String): DailyReport?

    @Query("SELECT * FROM daily_reports WHERE id = :reportId LIMIT 1")
    suspend fun getReportById(reportId: String): DailyReport?

    @Query("SELECT * FROM daily_reports WHERE userId = :userId AND status = 'SUBMITTED' ORDER BY workDate DESC")
    fun getSubmittedReports(userId: String): Flow<List<DailyReport>>

    @Query("SELECT * FROM daily_reports WHERE userId = :userId ORDER BY workDate DESC")
    fun getAllReports(userId: String): Flow<List<DailyReport>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReport(report: DailyReport)

    @Update
    suspend fun updateReport(report: DailyReport)

    @Query("SELECT COUNT(DISTINCT workDate) FROM daily_reports WHERE userId = :userId AND status = 'SUBMITTED'")
    fun getSubmittedDaysCount(userId: String): Flow<Int>

    @Query("SELECT COALESCE(SUM(totalReels), 0) FROM daily_reports WHERE userId = :userId AND status = 'SUBMITTED'")
    fun getTotalCompletedReels(userId: String): Flow<Int>
}
