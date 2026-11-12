package com.example.data.repository

import com.example.data.local.ClientDao
import com.example.data.local.DailyReportDao
import com.example.data.local.DailyReportEntryDao
import com.example.data.model.Client
import com.example.data.model.ClientEntryDetail
import com.example.data.model.DailyReport
import com.example.data.model.DailyReportEntry
import com.example.data.model.DailyReportWithDetails
import com.example.data.model.HistorySummaryStats
import com.example.data.model.ReportStatus
import com.example.data.remote.AuthRequest
import com.example.data.remote.SubmitReportRpcRequest
import com.example.data.remote.SupabaseClientManager
import com.example.util.DateUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.util.UUID

class ReelLogRepository(
    private val clientDao: ClientDao,
    private val reportDao: DailyReportDao,
    private val entryDao: DailyReportEntryDao,
    val supabaseManager: SupabaseClientManager
) {
    private val userId: String
        get() = supabaseManager.getEffectiveUserId()

    // ------------------------------------------------------------------------
    // CLIENT MANAGEMENT
    // ------------------------------------------------------------------------

    fun getActiveClients(): Flow<List<Client>> {
        return clientDao.getActiveClients(userId)
    }

    fun getAllClients(): Flow<List<Client>> {
        return clientDao.getAllClients(userId)
    }

    suspend fun addClient(name: String): Result<Client> = withContext(Dispatchers.IO) {
        val trimmed = name.trim()
        if (trimmed.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Client name cannot be empty."))
        }

        val existing = clientDao.getClientByName(userId, trimmed)
        if (existing != null) {
            return@withContext Result.failure(IllegalArgumentException("A client named '$trimmed' already exists."))
        }

        val client = Client(
            id = UUID.randomUUID().toString(),
            userId = userId,
            clientName = trimmed,
            isActive = true,
            createdAt = DateUtils.nowIsoTimestamp(),
            updatedAt = DateUtils.nowIsoTimestamp()
        )

        clientDao.insertClient(client)

        // Sync with Supabase if configured
        if (supabaseManager.isConfigured()) {
            try {
                val api = supabaseManager.getApi()
                val resp = api?.insertClient(
                    apiKey = supabaseManager.getApiKey(),
                    authHeader = supabaseManager.getAuthHeader(),
                    client = client
                )
                if (resp != null && !resp.isSuccessful) {
                    // Log or handle remote issue gracefully
                }
            } catch (_: Exception) {
                // Continue with local save
            }
        }

        Result.success(client)
    }

    suspend fun updateClientName(clientId: String, newName: String): Result<Client> = withContext(Dispatchers.IO) {
        val trimmed = newName.trim()
        if (trimmed.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Client name cannot be empty."))
        }

        val client = clientDao.getClientById(clientId)
            ?: return@withContext Result.failure(IllegalArgumentException("Client not found."))

        val duplicate = clientDao.getClientByName(userId, trimmed)
        if (duplicate != null && duplicate.id != clientId) {
            return@withContext Result.failure(IllegalArgumentException("Another client is already named '$trimmed'."))
        }

        val updated = client.copy(
            clientName = trimmed,
            updatedAt = DateUtils.nowIsoTimestamp()
        )
        clientDao.updateClient(updated)

        if (supabaseManager.isConfigured()) {
            try {
                supabaseManager.getApi()?.updateClient(
                    apiKey = supabaseManager.getApiKey(),
                    authHeader = supabaseManager.getAuthHeader(),
                    idFilter = "eq.$clientId",
                    client = mapOf("client_name" to trimmed, "updated_at" to DateUtils.nowIsoTimestamp())
                )
            } catch (_: Exception) { }
        }

        Result.success(updated)
    }

    suspend fun setClientActive(clientId: String, isActive: Boolean): Result<Unit> = withContext(Dispatchers.IO) {
        val client = clientDao.getClientById(clientId)
            ?: return@withContext Result.failure(IllegalArgumentException("Client not found."))

        val updated = client.copy(
            isActive = isActive,
            updatedAt = DateUtils.nowIsoTimestamp()
        )
        clientDao.updateClient(updated)

        if (supabaseManager.isConfigured()) {
            try {
                supabaseManager.getApi()?.updateClient(
                    apiKey = supabaseManager.getApiKey(),
                    authHeader = supabaseManager.getAuthHeader(),
                    idFilter = "eq.$clientId",
                    client = mapOf("is_active" to isActive, "updated_at" to DateUtils.nowIsoTimestamp())
                )
            } catch (_: Exception) { }
        }

        Result.success(Unit)
    }

    suspend fun deleteClient(clientId: String): Result<Unit> = withContext(Dispatchers.IO) {
        val count = clientDao.countEntriesForClient(clientId)
        if (count > 0) {
            return@withContext Result.failure(
                IllegalStateException("Cannot delete client with existing work history. Please deactivate the client instead.")
            )
        }
        clientDao.deleteClient(clientId)
        Result.success(Unit)
    }

    // ------------------------------------------------------------------------
    // DAILY REPORTS & REEL CODES
    // ------------------------------------------------------------------------

    fun getTodayReportFlow(workDate: String): Flow<DailyReport?> {
        return reportDao.getReportByDate(userId, workDate)
    }

    suspend fun getOrCreateTodayReport(workDate: String): DailyReport = withContext(Dispatchers.IO) {
        val existing = reportDao.getReportByDateSync(userId, workDate)
        if (existing != null) {
            return@withContext existing
        }

        val newReport = DailyReport(
            id = UUID.randomUUID().toString(),
            userId = userId,
            workDate = workDate,
            status = ReportStatus.DRAFT,
            totalReels = 0,
            submittedAt = null,
            createdAt = DateUtils.nowIsoTimestamp(),
            updatedAt = DateUtils.nowIsoTimestamp()
        )
        reportDao.insertReport(newReport)

        if (supabaseManager.isConfigured()) {
            try {
                supabaseManager.getApi()?.createDailyReport(
                    apiKey = supabaseManager.getApiKey(),
                    authHeader = supabaseManager.getAuthHeader(),
                    report = newReport
                )
            } catch (_: Exception) { }
        }

        newReport
    }

    fun getEntriesForReport(reportId: String): Flow<List<DailyReportEntry>> {
        return entryDao.getEntriesForReport(reportId)
    }

    suspend fun saveClientReels(
        reportId: String,
        clientId: String,
        newReelCodes: List<String>
    ): Result<DailyReportEntry> = withContext(Dispatchers.IO) {
        val report = reportDao.getReportById(reportId)
            ?: return@withContext Result.failure(IllegalStateException("Report not found."))

        if (report.isSubmitted) {
            return@withContext Result.failure(
                IllegalStateException("This report is locked and cannot be edited.")
            )
        }

        // Clean and filter new codes
        val cleanedNewCodes = newReelCodes.map { it.trim() }.filter { it.isNotBlank() }
        if (cleanedNewCodes.isEmpty()) {
            return@withContext Result.failure(IllegalArgumentException("No valid reel codes entered."))
        }

        val existingEntry = entryDao.getEntryForReportAndClient(reportId, clientId)
        val existingCodes = existingEntry?.reelCodes ?: emptyList()

        // Check for duplicates
        val duplicateCodes = cleanedNewCodes.filter { code ->
            existingCodes.any { it.equals(code, ignoreCase = true) }
        }
        if (duplicateCodes.isNotEmpty()) {
            return@withContext Result.failure(
                IllegalArgumentException("Duplicate reel code detected: '${duplicateCodes.first()}'. Already saved for this client.")
            )
        }

        // Also check if new codes contain internal duplicates
        val set = mutableSetOf<String>()
        for (code in cleanedNewCodes) {
            val lower = code.lowercase()
            if (!set.add(lower)) {
                return@withContext Result.failure(
                    IllegalArgumentException("Duplicate reel code '$code' in your entry.")
                )
            }
        }

        val mergedCodes = (existingCodes + cleanedNewCodes)

        val updatedEntry = if (existingEntry != null) {
            existingEntry.copy(
                reelCodes = mergedCodes,
                updatedAt = DateUtils.nowIsoTimestamp()
            )
        } else {
            DailyReportEntry(
                id = UUID.randomUUID().toString(),
                reportId = reportId,
                clientId = clientId,
                reelCodes = mergedCodes,
                createdAt = DateUtils.nowIsoTimestamp(),
                updatedAt = DateUtils.nowIsoTimestamp()
            )
        }

        entryDao.insertEntry(updatedEntry)

        // Recalculate and update total reels on report
        val allEntries = entryDao.getEntriesForReportSync(reportId)
        val totalCount = allEntries.sumOf { it.reelCodes.size }
        reportDao.updateReport(report.copy(totalReels = totalCount, updatedAt = DateUtils.nowIsoTimestamp()))

        // Sync to Supabase
        if (supabaseManager.isConfigured()) {
            try {
                supabaseManager.getApi()?.upsertReportEntry(
                    apiKey = supabaseManager.getApiKey(),
                    authHeader = supabaseManager.getAuthHeader(),
                    entry = updatedEntry
                )
            } catch (_: Exception) { }
        }

        Result.success(updatedEntry)
    }

    suspend fun submitDailyReport(reportId: String): Result<DailyReport> = withContext(Dispatchers.IO) {
        val report = reportDao.getReportById(reportId)
            ?: return@withContext Result.failure(IllegalStateException("Report not found."))

        if (report.isSubmitted) {
            return@withContext Result.failure(IllegalStateException("Report is already submitted."))
        }

        val entries = entryDao.getEntriesForReportSync(reportId)
        val totalReels = entries.sumOf { it.reelCodes.size }

        // Execute Supabase RPC if configured
        if (supabaseManager.isConfigured()) {
            try {
                val api = supabaseManager.getApi()
                val resp = api?.submitDailyReportRpc(
                    apiKey = supabaseManager.getApiKey(),
                    authHeader = supabaseManager.getAuthHeader(),
                    body = SubmitReportRpcRequest(reportId)
                )
                if (resp != null && !resp.isSuccessful) {
                    val err = resp.errorBody()?.string() ?: "Submission failed on server."
                    return@withContext Result.failure(IllegalStateException(err))
                }
            } catch (e: Exception) {
                // If network fails, return error rather than false success
                return@withContext Result.failure(
                    IllegalStateException("Failed to submit to Supabase: ${e.localizedMessage ?: "Network error"}")
                )
            }
        }

        // Lock in local Room database
        val submittedReport = report.copy(
            status = ReportStatus.SUBMITTED,
            totalReels = totalReels,
            submittedAt = DateUtils.nowIsoTimestamp(),
            updatedAt = DateUtils.nowIsoTimestamp()
        )
        reportDao.updateReport(submittedReport)

        Result.success(submittedReport)
    }

    // ------------------------------------------------------------------------
    // HISTORY & DETAILS
    // ------------------------------------------------------------------------

    fun getHistoryReportsWithDetails(): Flow<List<DailyReportWithDetails>> {
        return combine(
            reportDao.getAllReports(userId),
            clientDao.getAllClients(userId)
        ) { reports, clients ->
            val clientMap = clients.associateBy { it.id }

            reports.map { report ->
                val entries = entryDao.getEntriesForReportSync(report.id)
                val detailEntries = entries.map { entry ->
                    val client = clientMap[entry.clientId]
                    ClientEntryDetail(
                        entryId = entry.id,
                        clientId = entry.clientId,
                        clientName = client?.clientName ?: "Unknown Client",
                        isClientActive = client?.isActive ?: false,
                        reelCodes = entry.reelCodes
                    )
                }
                DailyReportWithDetails(report = report, entries = detailEntries)
            }
        }.flowOn(Dispatchers.IO)
    }

    fun getHistoryStats(): Flow<HistorySummaryStats> {
        return combine(
            reportDao.getSubmittedDaysCount(userId),
            reportDao.getTotalCompletedReels(userId),
            entryDao.getUniqueClientsWorkedCount(userId)
        ) { submittedDays, totalReels, totalClients ->
            HistorySummaryStats(
                totalSubmittedDays = submittedDays,
                totalReelsCompleted = totalReels,
                totalClientsWorkedWith = totalClients
            )
        }.flowOn(Dispatchers.IO)
    }

    // ------------------------------------------------------------------------
    // SUPABASE AUTH & REMOTE REFRESH
    // ------------------------------------------------------------------------

    suspend fun signIn(email: String, password: String): Result<String> = withContext(Dispatchers.IO) {
        val api = supabaseManager.getApi()
            ?: return@withContext Result.failure(IllegalStateException("Supabase URL and Anon Key are not configured yet."))

        try {
            val response = api.signIn(
                request = AuthRequest(email = email, password = password),
                apiKey = supabaseManager.getApiKey()
            )
            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                supabaseManager.saveAuthSession(
                    token = body.accessToken,
                    email = body.user?.email ?: email,
                    userId = body.user?.id
                )
                // Sync data after login
                syncRemoteData()
                Result.success("Signed in as ${body.user?.email ?: email}")
            } else {
                val err = response.errorBody()?.string() ?: "Authentication failed."
                Result.failure(IllegalArgumentException(err))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun signUp(email: String, password: String): Result<String> = withContext(Dispatchers.IO) {
        val api = supabaseManager.getApi()
            ?: return@withContext Result.failure(IllegalStateException("Supabase URL and Anon Key are not configured yet."))

        try {
            val response = api.signUp(
                request = AuthRequest(email = email, password = password),
                apiKey = supabaseManager.getApiKey()
            )
            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                supabaseManager.saveAuthSession(
                    token = body.accessToken,
                    email = body.user?.email ?: email,
                    userId = body.user?.id
                )
                Result.success("Account created successfully. Check your email if confirmation is required.")
            } else {
                val err = response.errorBody()?.string() ?: "Sign up failed."
                Result.failure(IllegalArgumentException(err))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun signOut(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val api = supabaseManager.getApi()
            api?.signOut(
                apiKey = supabaseManager.getApiKey(),
                authHeader = supabaseManager.getAuthHeader()
            )
        } catch (_: Exception) { }
        supabaseManager.clearAuthSession()
        Result.success(Unit)
    }

    suspend fun syncRemoteData(): Result<Unit> = withContext(Dispatchers.IO) {
        if (!supabaseManager.isConfigured()) return@withContext Result.success(Unit)

        try {
            val api = supabaseManager.getApi() ?: return@withContext Result.success(Unit)
            val clientResp = api.getClients(
                apiKey = supabaseManager.getApiKey(),
                authHeader = supabaseManager.getAuthHeader()
            )
            if (clientResp.isSuccessful && clientResp.body() != null) {
                clientDao.insertClients(clientResp.body()!!)
            }

            val reportsResp = api.getAllReports(
                apiKey = supabaseManager.getApiKey(),
                authHeader = supabaseManager.getAuthHeader()
            )
            if (reportsResp.isSuccessful && reportsResp.body() != null) {
                for (rep in reportsResp.body()!!) {
                    reportDao.insertReport(rep)
                }
            }

            val entriesResp = api.getAllEntries(
                apiKey = supabaseManager.getApiKey(),
                authHeader = supabaseManager.getAuthHeader()
            )
            if (entriesResp.isSuccessful && entriesResp.body() != null) {
                for (ent in entriesResp.body()!!) {
                    entryDao.insertEntry(ent)
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
