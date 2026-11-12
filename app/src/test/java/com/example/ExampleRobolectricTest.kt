package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.ReelLogDatabase
import com.example.data.model.Client
import com.example.data.model.DailyReport
import com.example.data.model.ReportStatus
import com.example.data.remote.SupabaseClientManager
import com.example.data.repository.ReelLogRepository
import com.example.util.DateUtils
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    private lateinit var database: ReelLogDatabase
    private lateinit var repository: ReelLogRepository
    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, ReelLogDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val supabaseManager = SupabaseClientManager(context)
        repository = ReelLogRepository(
            clientDao = database.clientDao(),
            reportDao = database.dailyReportDao(),
            entryDao = database.dailyReportEntryDao(),
            supabaseManager = supabaseManager
        )
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `read string from context verifies ReelLog app_name`() {
        val appName = context.getString(R.string.app_name)
        assertEquals("ReelLog", appName)
    }

    @Test
    fun `date utils returns valid Asia_Kolkata dates and formats`() {
        val today = DateUtils.getTodayInKolkata()
        assertNotNull(today)

        val isoDate = DateUtils.formatIsoDate(today)
        assertTrue(isoDate.matches(Regex("\\d{4}-\\d{2}-\\d{2}")))

        val dayName = DateUtils.formatDayName(today)
        assertTrue(dayName.isNotEmpty())

        val displayDate = DateUtils.formatDisplayDate(today)
        assertTrue(displayDate.isNotEmpty())
    }

    @Test
    fun `add client prevents empty and duplicate names`() = runBlocking {
        // Valid add
        val result1 = repository.addClient("Client Alpha")
        assertTrue(result1.isSuccess)

        // Duplicate name add
        val resultDuplicate = repository.addClient("client alpha ")
        assertTrue(resultDuplicate.isFailure)

        // Empty name add
        val resultEmpty = repository.addClient("   ")
        assertTrue(resultEmpty.isFailure)
    }

    @Test
    fun `saving reels merges codes and detects duplicates`() = runBlocking {
        val client = repository.addClient("Client Beta").getOrThrow()
        val report = repository.getOrCreateTodayReport("2026-11-12")

        // First save: 101, 102
        val save1 = repository.saveClientReels(
            reportId = report.id,
            clientId = client.id,
            newReelCodes = listOf("101", "102")
        )
        assertTrue(save1.isSuccess)
        assertEquals(listOf("101", "102"), save1.getOrThrow().reelCodes)

        // Second save: 103 (preserving 101, 102)
        val save2 = repository.saveClientReels(
            reportId = report.id,
            clientId = client.id,
            newReelCodes = listOf("103")
        )
        assertTrue(save2.isSuccess)
        assertEquals(listOf("101", "102", "103"), save2.getOrThrow().reelCodes)

        // Duplicate save: 102 (should fail)
        val saveDup = repository.saveClientReels(
            reportId = report.id,
            clientId = client.id,
            newReelCodes = listOf("102")
        )
        assertTrue(saveDup.isFailure)
    }

    @Test
    fun `submitting report makes it immutable and blocks edits`() = runBlocking {
        val client = repository.addClient("Client Gamma").getOrThrow()
        val report = repository.getOrCreateTodayReport("2026-11-12")

        repository.saveClientReels(
            reportId = report.id,
            clientId = client.id,
            newReelCodes = listOf("201", "202")
        )

        // Submit report
        val submitResult = repository.submitDailyReport(report.id)
        assertTrue(submitResult.isSuccess)
        val submitted = submitResult.getOrThrow()
        assertEquals(ReportStatus.SUBMITTED, submitted.status)
        assertEquals(2, submitted.totalReels)
        assertNotNull(submitted.submittedAt)

        // Attempting to add more reels to submitted report MUST fail
        val editAfterSubmit = repository.saveClientReels(
            reportId = report.id,
            clientId = client.id,
            newReelCodes = listOf("203")
        )
        assertTrue(editAfterSubmit.isFailure)
        assertTrue(editAfterSubmit.exceptionOrNull()?.message?.contains("locked") == true)
    }
}
