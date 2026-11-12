package com.example.ui.screens.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.model.Client
import com.example.data.model.SupabaseAuthConfig
import com.example.data.repository.ReelLogRepository
import com.example.ui.theme.AppThemeMode
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class ClientFilterTab(val label: String) {
    ALL("All"),
    ACTIVE("Active"),
    INACTIVE("Inactive")
}

data class SettingsUiState(
    val clients: List<Client> = emptyList(),
    val filteredClients: List<Client> = emptyList(),
    val clientTab: ClientFilterTab = ClientFilterTab.ALL,
    val themeMode: AppThemeMode = AppThemeMode.DARK,
    val supabaseConfig: SupabaseAuthConfig = SupabaseAuthConfig(),
    val inputSupabaseUrl: String = "",
    val inputSupabaseAnonKey: String = "",
    val authEmail: String = "",
    val authPassword: String = "",
    val isTestingConnection: Boolean = false,
    val isAuthenticating: Boolean = false,
    val showAddClientDialog: Boolean = false,
    val newClientName: String = "",
    val editingClient: Client? = null,
    val editClientName: String = "",
    val clientToToggleActive: Client? = null,
    val showSqlMigrationDialog: Boolean = false
)

class SettingsViewModel(
    private val repository: ReelLogRepository,
    private val context: Context
) : ViewModel() {

    private val prefs = context.getSharedPreferences("app_settings_prefs", Context.MODE_PRIVATE)

    private val _uiState = MutableStateFlow(
        SettingsUiState(
            themeMode = loadThemeMode()
        )
    )
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    private val _userMessage = MutableSharedFlow<String>()
    val userMessage: SharedFlow<String> = _userMessage.asSharedFlow()

    init {
        loadClients()
        loadSupabaseConfig()
    }

    private fun loadThemeMode(): AppThemeMode {
        val saved = prefs.getString("theme_mode", AppThemeMode.DARK.name) ?: AppThemeMode.DARK.name
        return try {
            AppThemeMode.valueOf(saved)
        } catch (_: Exception) {
            AppThemeMode.DARK
        }
    }

    fun setThemeMode(mode: AppThemeMode) {
        prefs.edit().putString("theme_mode", mode.name).apply()
        _uiState.value = _uiState.value.copy(themeMode = mode)
    }

    private fun loadSupabaseConfig() {
        viewModelScope.launch {
            repository.supabaseManager.configState.collect { config ->
                _uiState.value = _uiState.value.copy(
                    supabaseConfig = config,
                    inputSupabaseUrl = config.url,
                    inputSupabaseAnonKey = config.anonKey
                )
            }
        }
    }

    private fun loadClients() {
        viewModelScope.launch {
            repository.getAllClients().collect { allClients ->
                _uiState.value = _uiState.value.copy(clients = allClients)
                filterClients()
            }
        }
    }

    fun onClientTabChanged(tab: ClientFilterTab) {
        _uiState.value = _uiState.value.copy(clientTab = tab)
        filterClients()
    }

    private fun filterClients() {
        val state = _uiState.value
        val filtered = when (state.clientTab) {
            ClientFilterTab.ALL -> state.clients
            ClientFilterTab.ACTIVE -> state.clients.filter { it.isActive }
            ClientFilterTab.INACTIVE -> state.clients.filter { !it.isActive }
        }
        _uiState.value = state.copy(filteredClients = filtered)
    }

    fun onUrlChanged(url: String) {
        _uiState.value = _uiState.value.copy(inputSupabaseUrl = url)
    }

    fun onAnonKeyChanged(key: String) {
        _uiState.value = _uiState.value.copy(inputSupabaseAnonKey = key)
    }

    fun saveSupabaseCredentials() {
        val url = _uiState.value.inputSupabaseUrl.trim()
        val key = _uiState.value.inputSupabaseAnonKey.trim()

        repository.supabaseManager.saveCredentials(url, key)
        viewModelScope.launch {
            _userMessage.emit("Supabase endpoint credentials updated.")
        }
    }

    fun testConnection() {
        val url = _uiState.value.inputSupabaseUrl.trim()
        val key = _uiState.value.inputSupabaseAnonKey.trim()

        if (url.isBlank() || key.isBlank()) {
            viewModelScope.launch {
                _userMessage.emit("Please enter both Supabase URL and Anon Key first.")
            }
            return
        }

        saveSupabaseCredentials()
        _uiState.value = _uiState.value.copy(isTestingConnection = true)

        viewModelScope.launch {
            val syncResult = repository.syncRemoteData()
            _uiState.value = _uiState.value.copy(isTestingConnection = false)
            if (syncResult.isSuccess) {
                _userMessage.emit("Connection successful! Supabase tables reached.")
            } else {
                _userMessage.emit("Connection test: ${syncResult.exceptionOrNull()?.localizedMessage ?: "Failed to reach Supabase"}")
            }
        }
    }

    fun onAuthEmailChanged(email: String) {
        _uiState.value = _uiState.value.copy(authEmail = email)
    }

    fun onAuthPasswordChanged(pwd: String) {
        _uiState.value = _uiState.value.copy(authPassword = pwd)
    }

    fun signIn() {
        val email = _uiState.value.authEmail.trim()
        val pwd = _uiState.value.authPassword.trim()

        if (email.isBlank() || pwd.isBlank()) {
            viewModelScope.launch { _userMessage.emit("Please enter both email and password.") }
            return
        }

        _uiState.value = _uiState.value.copy(isAuthenticating = true)
        viewModelScope.launch {
            val result = repository.signIn(email, pwd)
            _uiState.value = _uiState.value.copy(isAuthenticating = false)
            result.onSuccess { msg ->
                _userMessage.emit(msg)
            }.onFailure { err ->
                _userMessage.emit(err.localizedMessage ?: "Sign in failed.")
            }
        }
    }

    fun signUp() {
        val email = _uiState.value.authEmail.trim()
        val pwd = _uiState.value.authPassword.trim()

        if (email.isBlank() || pwd.length < 6) {
            viewModelScope.launch { _userMessage.emit("Password must be at least 6 characters.") }
            return
        }

        _uiState.value = _uiState.value.copy(isAuthenticating = true)
        viewModelScope.launch {
            val result = repository.signUp(email, pwd)
            _uiState.value = _uiState.value.copy(isAuthenticating = false)
            result.onSuccess { msg ->
                _userMessage.emit(msg)
            }.onFailure { err ->
                _userMessage.emit(err.localizedMessage ?: "Sign up failed.")
            }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            repository.signOut()
            _userMessage.emit("Logged out successfully.")
        }
    }

    // Client Add / Edit / Toggle
    fun showAddClientDialog(show: Boolean) {
        _uiState.value = _uiState.value.copy(showAddClientDialog = show, newClientName = "")
    }

    fun onNewClientNameChanged(name: String) {
        _uiState.value = _uiState.value.copy(newClientName = name)
    }

    fun addNewClient() {
        val name = _uiState.value.newClientName.trim()
        if (name.isBlank()) {
            viewModelScope.launch { _userMessage.emit("Client name cannot be empty.") }
            return
        }

        viewModelScope.launch {
            val result = repository.addClient(name)
            result.onSuccess {
                _uiState.value = _uiState.value.copy(showAddClientDialog = false, newClientName = "")
                _userMessage.emit("Client '${it.clientName}' added successfully.")
            }.onFailure {
                _userMessage.emit(it.localizedMessage ?: "Failed to add client.")
            }
        }
    }

    fun startEditingClient(client: Client) {
        _uiState.value = _uiState.value.copy(editingClient = client, editClientName = client.clientName)
    }

    fun onEditClientNameChanged(name: String) {
        _uiState.value = _uiState.value.copy(editClientName = name)
    }

    fun saveEditedClientName() {
        val client = _uiState.value.editingClient ?: return
        val newName = _uiState.value.editClientName.trim()

        viewModelScope.launch {
            val result = repository.updateClientName(client.id, newName)
            result.onSuccess {
                _uiState.value = _uiState.value.copy(editingClient = null, editClientName = "")
                _userMessage.emit("Client renamed to '${it.clientName}'.")
            }.onFailure {
                _userMessage.emit(it.localizedMessage ?: "Failed to update client.")
            }
        }
    }

    fun cancelEditingClient() {
        _uiState.value = _uiState.value.copy(editingClient = null, editClientName = "")
    }

    fun startToggleActive(client: Client) {
        _uiState.value = _uiState.value.copy(clientToToggleActive = client)
    }

    fun dismissToggleActive() {
        _uiState.value = _uiState.value.copy(clientToToggleActive = null)
    }

    fun confirmToggleActive() {
        val client = _uiState.value.clientToToggleActive ?: return
        val newActive = !client.isActive

        viewModelScope.launch {
            val result = repository.setClientActive(client.id, newActive)
            _uiState.value = _uiState.value.copy(clientToToggleActive = null)
            result.onSuccess {
                val stateText = if (newActive) "activated" else "deactivated"
                _userMessage.emit("Client '${client.clientName}' $stateText. Historical reports preserved.")
            }.onFailure {
                _userMessage.emit(it.localizedMessage ?: "Failed to update client status.")
            }
        }
    }

    fun deleteClient(clientId: String) {
        viewModelScope.launch {
            val result = repository.deleteClient(clientId)
            result.onSuccess {
                _userMessage.emit("Client deleted.")
            }.onFailure {
                _userMessage.emit(it.localizedMessage ?: "Cannot delete client.")
            }
        }
    }

    fun showSqlDialog(show: Boolean) {
        _uiState.value = _uiState.value.copy(showSqlMigrationDialog = show)
    }

    class Factory(
        private val repository: ReelLogRepository,
        private val context: Context
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SettingsViewModel(repository, context) as T
        }
    }
}
