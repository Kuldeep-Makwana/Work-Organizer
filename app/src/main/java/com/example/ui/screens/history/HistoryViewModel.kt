package com.example.ui.screens.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.model.DailyReportWithDetails
import com.example.data.model.HistorySummaryStats
import com.example.data.model.ReportStatus
import com.example.data.repository.ReelLogRepository
import com.example.util.DateUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.temporal.ChronoUnit

enum class DateFilterOption(val label: String) {
    ALL("All Time"),
    TODAY("Today"),
    YESTERDAY("Yesterday"),
    THIS_WEEK("This Week"),
    THIS_MONTH("This Month")
}

enum class SortOrder(val label: String) {
    NEWEST_FIRST("Newest"),
    OLDEST_FIRST("Oldest")
}

data class HistoryUiState(
    val allReports: List<DailyReportWithDetails> = emptyList(),
    val filteredReports: List<DailyReportWithDetails> = emptyList(),
    val stats: HistorySummaryStats = HistorySummaryStats(),
    val searchQuery: String = "",
    val dateFilter: DateFilterOption = DateFilterOption.ALL,
    val selectedClientId: String? = null,
    val statusFilter: String? = null, // null = all, or DRAFT, SUBMITTED
    val sortOrder: SortOrder = SortOrder.NEWEST_FIRST,
    val selectedReportForDetail: DailyReportWithDetails? = null,
    val isLoading: Boolean = true
)

class HistoryViewModel(
    private val repository: ReelLogRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HistoryUiState())
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()

    init {
        loadHistory()
    }

    private fun loadHistory() {
        viewModelScope.launch {
            combine(
                repository.getHistoryReportsWithDetails(),
                repository.getHistoryStats()
            ) { reports, stats ->
                Pair(reports, stats)
            }.collect { (reports, stats) ->
                _uiState.value = _uiState.value.copy(
                    allReports = reports,
                    stats = stats,
                    isLoading = false
                )
                applyFiltersAndSorting()
            }
        }
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
        applyFiltersAndSorting()
    }

    fun onDateFilterSelected(filter: DateFilterOption) {
        _uiState.value = _uiState.value.copy(dateFilter = filter)
        applyFiltersAndSorting()
    }

    fun onStatusFilterSelected(status: String?) {
        _uiState.value = _uiState.value.copy(statusFilter = status)
        applyFiltersAndSorting()
    }

    fun onSortOrderChanged(sort: SortOrder) {
        _uiState.value = _uiState.value.copy(sortOrder = sort)
        applyFiltersAndSorting()
    }

    fun resetFilters() {
        _uiState.value = _uiState.value.copy(
            searchQuery = "",
            dateFilter = DateFilterOption.ALL,
            selectedClientId = null,
            statusFilter = null,
            sortOrder = SortOrder.NEWEST_FIRST
        )
        applyFiltersAndSorting()
    }

    fun selectReportForDetail(report: DailyReportWithDetails?) {
        _uiState.value = _uiState.value.copy(selectedReportForDetail = report)
    }

    private fun applyFiltersAndSorting() {
        val state = _uiState.value
        val today = DateUtils.getTodayInKolkata()

        val filtered = state.allReports.filter { item ->
            // Search filter (client name, reel code, date)
            val matchesQuery = if (state.searchQuery.isBlank()) true else {
                val q = state.searchQuery.trim().lowercase()
                val dateMatches = item.report.workDate.lowercase().contains(q)
                val clientMatches = item.entries.any { it.clientName.lowercase().contains(q) }
                val codeMatches = item.entries.any { entry ->
                    entry.reelCodes.any { it.lowercase().contains(q) }
                }
                dateMatches || clientMatches || codeMatches
            }

            // Status filter
            val matchesStatus = if (state.statusFilter == null) true else {
                item.report.status == state.statusFilter
            }

            // Client filter
            val matchesClient = if (state.selectedClientId == null) true else {
                item.entries.any { it.clientId == state.selectedClientId }
            }

            // Date filter
            val matchesDate = when (state.dateFilter) {
                DateFilterOption.ALL -> true
                DateFilterOption.TODAY -> item.report.workDate == DateUtils.formatIsoDate(today)
                DateFilterOption.YESTERDAY -> {
                    val yesterday = today.minusDays(1)
                    item.report.workDate == DateUtils.formatIsoDate(yesterday)
                }
                DateFilterOption.THIS_WEEK -> {
                    val reportDate = DateUtils.parseIsoDate(item.report.workDate)
                    val days = ChronoUnit.DAYS.between(reportDate, today)
                    days in 0..7
                }
                DateFilterOption.THIS_MONTH -> {
                    val reportDate = DateUtils.parseIsoDate(item.report.workDate)
                    reportDate.year == today.year && reportDate.month == today.month
                }
            }

            matchesQuery && matchesStatus && matchesClient && matchesDate
        }

        val sorted = when (state.sortOrder) {
            SortOrder.NEWEST_FIRST -> filtered.sortedByDescending { it.report.workDate }
            SortOrder.OLDEST_FIRST -> filtered.sortedBy { it.report.workDate }
        }

        _uiState.value = state.copy(filteredReports = sorted)
    }

    class Factory(private val repository: ReelLogRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return HistoryViewModel(repository) as T
        }
    }
}
