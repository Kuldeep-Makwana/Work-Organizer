package com.example.data.model

enum class ReelSaveStatus {
    SAVED,
    SAVING,
    UNSAVED,
    FAILED
}

data class ClientWorkUiItem(
    val client: Client,
    val existingEntryId: String? = null,
    val reelCodes: List<String> = emptyList(),
    val inputText: String = "",
    val saveStatus: ReelSaveStatus = ReelSaveStatus.SAVED,
    val errorMessage: String? = null
)

data class ClientEntryDetail(
    val entryId: String,
    val clientId: String,
    val clientName: String,
    val isClientActive: Boolean,
    val reelCodes: List<String>
) {
    val count: Int get() = reelCodes.size
}

data class DailyReportWithDetails(
    val report: DailyReport,
    val entries: List<ClientEntryDetail>
) {
    val totalReels: Int get() = entries.sumOf { it.count }
    val totalClients: Int get() = entries.count { it.count > 0 }
    val totalEntries: Int get() = entries.size
}

data class HistorySummaryStats(
    val totalSubmittedDays: Int = 0,
    val totalReelsCompleted: Int = 0,
    val totalClientsWorkedWith: Int = 0
)

data class SupabaseAuthConfig(
    val url: String = "",
    val anonKey: String = "",
    val accessToken: String? = null,
    val userEmail: String? = null,
    val userId: String? = null,
    val isConnected: Boolean = false
)
