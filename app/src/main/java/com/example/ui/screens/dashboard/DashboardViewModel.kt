package com.example.ui.screens.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.model.Client
import com.example.data.model.ClientWorkUiItem
import com.example.data.model.DailyReport
import com.example.data.model.ReelSaveStatus
import com.example.data.repository.ReelLogRepository
import com.example.util.DateUtils
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.time.LocalDate

data class DashboardUiState(
    val currentDate: LocalDate = DateUtils.getTodayInKolkata(),
    val dayName: String = "",
    val displayDate: String = "",
    val isoDate: String = "",
    val dailyReport: DailyReport? = null,
    val clientItems: List<ClientWorkUiItem> = emptyList(),
    val totalClientsWorked: Int = 0,
    val totalReelsCompleted: Int = 0,
    val totalEntries: Int = 0,
    val isLoading: Boolean = true,
    val isSubmitting: Boolean = false,
    val showSubmitConfirmationDialog: Boolean = false,
    val showAddClientDialog: Boolean = false,
    val newClientInputName: String = ""
)

class DashboardViewModel(
    private val repository: ReelLogRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        DashboardUiState(
            currentDate = DateUtils.getTodayInKolkata(),
            dayName = DateUtils.formatDayName(DateUtils.getTodayInKolkata()),
            displayDate = DateUtils.formatDisplayDate(DateUtils.getTodayInKolkata()),
            isoDate = DateUtils.formatIsoDate(DateUtils.getTodayInKolkata())
        )
    )
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    private val _userMessage = MutableSharedFlow<String>()
    val userMessage: SharedFlow<String> = _userMessage.asSharedFlow()

    // Temporary input text state per client id to allow reactive editing
    private val clientInputs = mutableMapOf<String, String>()
    private val clientStatuses = mutableMapOf<String, ReelSaveStatus>()

    init {
        initializeToday()
    }

    private fun initializeToday() {
        val today = DateUtils.getTodayInKolkata()
        val iso = DateUtils.formatIsoDate(today)

        _uiState.value = _uiState.value.copy(
            currentDate = today,
            dayName = DateUtils.formatDayName(today),
            displayDate = DateUtils.formatDisplayDate(today),
            isoDate = iso
        )

        viewModelScope.launch {
            // Get or create today's draft report
            val report = repository.getOrCreateTodayReport(iso)

            // Listen to active clients, report changes, and entries changes
            combine(
                repository.getActiveClients(),
                repository.getTodayReportFlow(iso),
                repository.getEntriesForReport(report.id)
            ) { clients, currentReport, entries ->
                val entryMap = entries.associateBy { it.clientId }

                val items = clients.map { client ->
                    val entry = entryMap[client.id]
                    val savedCodes = entry?.reelCodes ?: emptyList()
                    val inputText = clientInputs[client.id] ?: ""
                    val status = clientStatuses[client.id] ?: if (savedCodes.isNotEmpty()) ReelSaveStatus.SAVED else ReelSaveStatus.UNSAVED

                    ClientWorkUiItem(
                        client = client,
                        existingEntryId = entry?.id,
                        reelCodes = savedCodes,
                        inputText = inputText,
                        saveStatus = status
                    )
                }

                val totalReels = entries.sumOf { it.reelCodes.size }
                val workedClients = entries.count { it.reelCodes.isNotEmpty() }

                Triple(items, currentReport ?: report, Pair(totalReels, workedClients))
            }.collect { (items, report, counts) ->
                _uiState.value = _uiState.value.copy(
                    clientItems = items,
                    dailyReport = report,
                    totalReelsCompleted = counts.first,
                    totalClientsWorked = counts.second,
                    totalEntries = items.count { it.reelCodes.isNotEmpty() },
                    isLoading = false
                )
            }
        }
    }

    fun onClientInputChanged(clientId: String, text: String) {
        if (_uiState.value.dailyReport?.isSubmitted == true) return
        clientInputs[clientId] = text
        clientStatuses[clientId] = ReelSaveStatus.UNSAVED

        val updatedItems = _uiState.value.clientItems.map {
            if (it.client.id == clientId) {
                it.copy(inputText = text, saveStatus = ReelSaveStatus.UNSAVED, errorMessage = null)
            } else it
        }
        _uiState.value = _uiState.value.copy(clientItems = updatedItems)
    }

    fun saveClientReels(clientId: String) {
        val report = _uiState.value.dailyReport ?: return
        if (report.isSubmitted) {
            viewModelScope.launch { _userMessage.emit("This report is locked and cannot be edited.") }
            return
        }

        val item = _uiState.value.clientItems.find { it.client.id == clientId } ?: return
        val rawInput = item.inputText.trim()

        if (rawInput.isBlank()) {
            viewModelScope.launch { _userMessage.emit("Please enter at least one reel code before saving.") }
            return
        }

        // Split by comma
        val codes = rawInput.split(",")
            .map { it.trim() }
            .filter { it.isNotBlank() }

        if (codes.isEmpty()) {
            viewModelScope.launch { _userMessage.emit("No valid reel codes found.") }
            return
        }

        // Set status to SAVING
        clientStatuses[clientId] = ReelSaveStatus.SAVING
        _uiState.value = _uiState.value.copy(
            clientItems = _uiState.value.clientItems.map {
                if (it.client.id == clientId) it.copy(saveStatus = ReelSaveStatus.SAVING) else it
            }
        )

        viewModelScope.launch {
            val result = repository.saveClientReels(
                reportId = report.id,
                clientId = clientId,
                newReelCodes = codes
            )

            result.onSuccess {
                clientInputs[clientId] = ""
                clientStatuses[clientId] = ReelSaveStatus.SAVED
                _uiState.value = _uiState.value.copy(
                    clientItems = _uiState.value.clientItems.map {
                        if (it.client.id == clientId) {
                            it.copy(
                                inputText = "",
                                saveStatus = ReelSaveStatus.SAVED,
                                errorMessage = null
                            )
                        } else it
                    }
                )
                _userMessage.emit("Reel codes saved successfully for ${item.client.clientName}.")
            }.onFailure { error ->
                clientStatuses[clientId] = ReelSaveStatus.FAILED
                _uiState.value = _uiState.value.copy(
                    clientItems = _uiState.value.clientItems.map {
                        if (it.client.id == clientId) {
                            it.copy(
                                saveStatus = ReelSaveStatus.FAILED,
                                errorMessage = error.localizedMessage
                            )
                        } else it
                    }
                )
                _userMessage.emit(error.localizedMessage ?: "Failed to save reel codes.")
            }
        }
    }

    fun openSubmitConfirmation() {
        val report = _uiState.value.dailyReport ?: return
        if (report.isSubmitted) {
            viewModelScope.launch { _userMessage.emit("Report is already permanently submitted.") }
            return
        }

        if (_uiState.value.totalReelsCompleted == 0) {
            viewModelScope.launch { _userMessage.emit("Please log and save at least one reel before submitting.") }
            return
        }

        _uiState.value = _uiState.value.copy(showSubmitConfirmationDialog = true)
    }

    fun dismissSubmitConfirmation() {
        _uiState.value = _uiState.value.copy(showSubmitConfirmationDialog = false)
    }

    fun confirmFinalSubmit() {
        val report = _uiState.value.dailyReport ?: return
        _uiState.value = _uiState.value.copy(
            showSubmitConfirmationDialog = false,
            isSubmitting = true
        )

        viewModelScope.launch {
            val result = repository.submitDailyReport(report.id)
            result.onSuccess { submitted ->
                _uiState.value = _uiState.value.copy(
                    dailyReport = submitted,
                    isSubmitting = false
                )
                _userMessage.emit("Daily report submitted successfully. Report is now permanently locked.")
            }.onFailure { err ->
                _uiState.value = _uiState.value.copy(isSubmitting = false)
                _userMessage.emit(err.localizedMessage ?: "Submission failed.")
            }
        }
    }

    fun showAddClientDialog(show: Boolean) {
        _uiState.value = _uiState.value.copy(showAddClientDialog = show, newClientInputName = "")
    }

    fun onNewClientNameChanged(name: String) {
        _uiState.value = _uiState.value.copy(newClientInputName = name)
    }

    fun addNewClient() {
        val name = _uiState.value.newClientInputName.trim()
        if (name.isBlank()) {
            viewModelScope.launch { _userMessage.emit("Client name cannot be empty.") }
            return
        }

        viewModelScope.launch {
            val result = repository.addClient(name)
            result.onSuccess { client ->
                _uiState.value = _uiState.value.copy(
                    showAddClientDialog = false,
                    newClientInputName = ""
                )
                _userMessage.emit("Client '${client.clientName}' added successfully.")
            }.onFailure { error ->
                _userMessage.emit(error.localizedMessage ?: "Failed to add client.")
            }
        }
    }

    class Factory(private val repository: ReelLogRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return DashboardViewModel(repository) as T
        }
    }
}
