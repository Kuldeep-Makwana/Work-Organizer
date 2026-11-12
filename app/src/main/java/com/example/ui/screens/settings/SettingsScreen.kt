package com.example.ui.screens.settings

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.Client
import com.example.ui.components.NothingButton
import com.example.ui.components.NothingCard
import com.example.ui.components.NothingTextField
import com.example.ui.theme.AppThemeMode
import com.example.ui.theme.DotFontFamily
import com.example.ui.theme.LocalNothingColors
import com.example.ui.theme.NothingMonoFamily
import com.example.ui.theme.NothingSansFamily
import kotlinx.coroutines.flow.collectLatest

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val colors = LocalNothingColors.current
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.userMessage.collectLatest { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colors.mainBackground)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                SettingsHeader()
            }

            // --- SECTION 1: CLIENT MANAGEMENT ---
            item {
                SectionHeader(title = "CLIENT MANAGEMENT")
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ClientTabs(
                        selectedTab = uiState.clientTab,
                        onTabSelected = { viewModel.onClientTabChanged(it) }
                    )

                    NothingButton(
                        text = "+ Add Client",
                        onClick = { viewModel.showAddClientDialog(true) },
                        modifier = Modifier.height(34.dp),
                        testTag = "settings_add_client_button"
                    )
                }
            }

            if (uiState.filteredClients.isEmpty()) {
                item {
                    NothingCard(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "No ${uiState.clientTab.label.lowercase()} clients found.",
                            style = TextStyle(
                                fontFamily = NothingSansFamily,
                                fontSize = 13.sp,
                                color = colors.secondaryText
                            )
                        )
                    }
                }
            } else {
                items(
                    items = uiState.filteredClients,
                    key = { it.id }
                ) { client ->
                    ClientManagementRow(
                        client = client,
                        onEdit = { viewModel.startEditingClient(client) },
                        onToggleActive = { viewModel.startToggleActive(client) },
                        onDelete = { viewModel.deleteClient(client.id) }
                    )
                }
            }

            // --- SECTION 2: APPEARANCE ---
            item {
                SectionHeader(title = "APPEARANCE")
            }

            item {
                ThemeSelectorCard(
                    currentMode = uiState.themeMode,
                    onModeSelected = { viewModel.setThemeMode(it) }
                )
            }

            // --- SECTION 3: SUPABASE INTEGRATION & AUTH ---
            item {
                SectionHeader(title = "SUPABASE BACKEND & AUTH")
            }

            item {
                SupabaseConfigCard(
                    uiState = uiState,
                    onUrlChange = { viewModel.onUrlChanged(it) },
                    onKeyChange = { viewModel.onAnonKeyChanged(it) },
                    onSave = { viewModel.saveSupabaseCredentials() },
                    onTest = { viewModel.testConnection() },
                    onViewSql = { viewModel.showSqlDialog(true) },
                    onEmailChange = { viewModel.onAuthEmailChanged(it) },
                    onPasswordChange = { viewModel.onAuthPasswordChanged(it) },
                    onSignIn = { viewModel.signIn() },
                    onSignUp = { viewModel.signUp() },
                    onSignOut = { viewModel.signOut() }
                )
            }

            // --- SECTION 4: APP INFORMATION ---
            item {
                SectionHeader(title = "APPLICATION INFO")
            }

            item {
                AppInfoCard()
            }

            item {
                Spacer(modifier = Modifier.height(90.dp))
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 80.dp)
        )
    }

    // Add Client Dialog
    if (uiState.showAddClientDialog) {
        AddClientDialog(
            name = uiState.newClientName,
            onNameChange = { viewModel.onNewClientNameChanged(it) },
            onDismiss = { viewModel.showAddClientDialog(false) },
            onConfirm = { viewModel.addNewClient() }
        )
    }

    // Edit Client Dialog
    uiState.editingClient?.let { client ->
        EditClientDialog(
            initialName = client.clientName,
            currentName = uiState.editClientName,
            onNameChange = { viewModel.onEditClientNameChanged(it) },
            onDismiss = { viewModel.cancelEditingClient() },
            onConfirm = { viewModel.saveEditedClientName() }
        )
    }

    // Toggle Active Confirmation Dialog
    uiState.clientToToggleActive?.let { client ->
        ToggleActiveConfirmDialog(
            client = client,
            onDismiss = { viewModel.dismissToggleActive() },
            onConfirm = { viewModel.confirmToggleActive() }
        )
    }

    // SQL Schema & Setup Guide Dialog
    if (uiState.showSqlMigrationDialog) {
        SqlMigrationDialog(
            context = context,
            onDismiss = { viewModel.showSqlDialog(false) }
        )
    }
}

@Composable
private fun SettingsHeader() {
    val colors = LocalNothingColors.current

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(colors.accentRed)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "SETTINGS",
                style = TextStyle(
                    fontFamily = DotFontFamily,
                    fontSize = 22.sp,
                    color = colors.primaryText,
                    letterSpacing = 1.5.sp
                )
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Client management, database integration, and appearance",
            style = TextStyle(
                fontFamily = NothingSansFamily,
                fontSize = 13.sp,
                color = colors.secondaryText
            )
        )
    }
}

@Composable
private fun SectionHeader(title: String) {
    val colors = LocalNothingColors.current

    Text(
        text = title,
        style = TextStyle(
            fontFamily = NothingMonoFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            color = colors.secondaryText,
            letterSpacing = 1.sp
        )
    )
}

@Composable
private fun ClientTabs(
    selectedTab: ClientFilterTab,
    onTabSelected: (ClientFilterTab) -> Unit
) {
    val colors = LocalNothingColors.current

    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        ClientFilterTab.entries.forEach { tab ->
            val isSelected = selectedTab == tab
            Surface(
                onClick = { onTabSelected(tab) },
                shape = RoundedCornerShape(6.dp),
                color = if (isSelected) colors.primaryText else colors.elevatedBackground,
                border = if (isSelected) null else androidx.compose.foundation.BorderStroke(1.dp, colors.subtleBorder)
            ) {
                Text(
                    text = tab.label.uppercase(),
                    style = TextStyle(
                        fontFamily = NothingMonoFamily,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) colors.mainBackground else colors.secondaryText
                    ),
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }
        }
    }
}

@Composable
private fun ClientManagementRow(
    client: Client,
    onEdit: () -> Unit,
    onToggleActive: () -> Unit,
    onDelete: () -> Unit
) {
    val colors = LocalNothingColors.current

    NothingCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(if (client.isActive) colors.accentRed else colors.secondaryText)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = client.clientName,
                        style = TextStyle(
                            fontFamily = NothingSansFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = colors.primaryText
                        )
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = if (client.isActive) "ACTIVE — VISIBLE ON DASHBOARD" else "INACTIVE — HIDDEN FROM DASHBOARD",
                    style = TextStyle(
                        fontFamily = NothingMonoFamily,
                        fontSize = 10.sp,
                        color = if (client.isActive) colors.statusSaved else colors.secondaryText
                    )
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                // Edit button
                IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit Client",
                        tint = colors.primaryText,
                        modifier = Modifier.size(16.dp)
                    )
                }

                // Toggle active button
                Surface(
                    onClick = onToggleActive,
                    shape = RoundedCornerShape(4.dp),
                    color = if (client.isActive) colors.elevatedBackground else colors.accentRed.copy(alpha = 0.15f),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (client.isActive) colors.subtleBorder else colors.accentRed
                    ),
                    modifier = Modifier.padding(horizontal = 4.dp)
                ) {
                    Text(
                        text = if (client.isActive) "DEACTIVATE" else "ACTIVATE",
                        style = TextStyle(
                            fontFamily = NothingMonoFamily,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (client.isActive) colors.secondaryText else colors.accentRed
                        ),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                // Delete button
                IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete Client",
                        tint = colors.secondaryText,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ThemeSelectorCard(
    currentMode: AppThemeMode,
    onModeSelected: (AppThemeMode) -> Unit
) {
    val colors = LocalNothingColors.current

    NothingCard(modifier = Modifier.fillMaxWidth()) {
        Column {
            Text(
                text = "COLOR SCHEME",
                style = TextStyle(
                    fontFamily = NothingMonoFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    color = colors.secondaryText,
                    letterSpacing = 1.sp
                )
            )
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AppThemeMode.entries.forEach { mode ->
                    val isSelected = currentMode == mode
                    Surface(
                        onClick = { onModeSelected(mode) },
                        shape = RoundedCornerShape(6.dp),
                        color = if (isSelected) colors.primaryText else colors.elevatedBackground,
                        border = if (isSelected) null else androidx.compose.foundation.BorderStroke(1.dp, colors.subtleBorder),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = mode.name,
                            style = TextStyle(
                                fontFamily = NothingMonoFamily,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) colors.mainBackground else colors.primaryText
                            ),
                            modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SupabaseConfigCard(
    uiState: SettingsUiState,
    onUrlChange: (String) -> Unit,
    onKeyChange: (String) -> Unit,
    onSave: () -> Unit,
    onTest: () -> Unit,
    onViewSql: () -> Unit,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onSignIn: () -> Unit,
    onSignUp: () -> Unit,
    onSignOut: () -> Unit
) {
    val colors = LocalNothingColors.current
    val config = uiState.supabaseConfig

    NothingCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "SUPABASE REST & AUTH",
                    style = TextStyle(
                        fontFamily = NothingMonoFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = colors.secondaryText,
                        letterSpacing = 1.sp
                    )
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(if (config.isConnected) colors.statusSaved else colors.secondaryText)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (config.isConnected) "CONFIGURED" else "LOCAL CACHE",
                        style = TextStyle(
                            fontFamily = NothingMonoFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            color = if (config.isConnected) colors.statusSaved else colors.secondaryText
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // URL input
            Text(
                text = "PROJECT URL",
                style = TextStyle(
                    fontFamily = NothingMonoFamily,
                    fontSize = 10.sp,
                    color = colors.secondaryText
                )
            )
            Spacer(modifier = Modifier.height(4.dp))
            NothingTextField(
                value = uiState.inputSupabaseUrl,
                onValueChange = onUrlChange,
                placeholder = "https://your-project.supabase.co",
                modifier = Modifier.fillMaxWidth(),
                testTag = "supabase_url_input"
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Anon Key input
            Text(
                text = "ANON PUBLIC KEY",
                style = TextStyle(
                    fontFamily = NothingMonoFamily,
                    fontSize = 10.sp,
                    color = colors.secondaryText
                )
            )
            Spacer(modifier = Modifier.height(4.dp))
            NothingTextField(
                value = uiState.inputSupabaseAnonKey,
                onValueChange = onKeyChange,
                placeholder = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
                modifier = Modifier.fillMaxWidth(),
                testTag = "supabase_anon_key_input"
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                NothingButton(
                    text = "Save Credentials",
                    onClick = onSave,
                    modifier = Modifier.weight(1f),
                    testTag = "save_supabase_creds_button"
                )
                NothingButton(
                    text = "Test Connection",
                    onClick = onTest,
                    isPrimary = true,
                    isLoading = uiState.isTestingConnection,
                    modifier = Modifier.weight(1f),
                    testTag = "test_connection_button"
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            NothingButton(
                text = "View SQL Schema & Setup Guide",
                onClick = onViewSql,
                modifier = Modifier.fillMaxWidth(),
                testTag = "view_sql_button"
            )

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 14.dp),
                color = colors.subtleBorder
            )

            // Auth Section
            Text(
                text = "AUTHENTICATION",
                style = TextStyle(
                    fontFamily = NothingMonoFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    color = colors.secondaryText,
                    letterSpacing = 1.sp
                )
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (!config.userEmail.isNullOrBlank()) {
                // Logged in
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = config.userEmail,
                            style = TextStyle(
                                fontFamily = NothingSansFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = colors.primaryText
                            )
                        )
                        Text(
                            text = "UID: ${config.userId?.take(8)}...",
                            style = TextStyle(
                                fontFamily = NothingMonoFamily,
                                fontSize = 11.sp,
                                color = colors.secondaryText
                            )
                        )
                    }

                    NothingButton(
                        text = "Sign Out",
                        onClick = onSignOut,
                        testTag = "sign_out_button"
                    )
                }
            } else {
                // Not logged in
                Text(
                    text = "Sign in to isolate and sync your daily reports with your Supabase account.",
                    style = TextStyle(
                        fontFamily = NothingSansFamily,
                        fontSize = 12.sp,
                        color = colors.secondaryText
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))
                NothingTextField(
                    value = uiState.authEmail,
                    onValueChange = onEmailChange,
                    placeholder = "email@example.com",
                    modifier = Modifier.fillMaxWidth(),
                    testTag = "auth_email_input"
                )
                Spacer(modifier = Modifier.height(6.dp))
                NothingTextField(
                    value = uiState.authPassword,
                    onValueChange = onPasswordChange,
                    placeholder = "Password (min 6 chars)",
                    modifier = Modifier.fillMaxWidth(),
                    testTag = "auth_password_input"
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    NothingButton(
                        text = "Sign In",
                        onClick = onSignIn,
                        isPrimary = true,
                        isLoading = uiState.isAuthenticating,
                        modifier = Modifier.weight(1f),
                        testTag = "sign_in_button"
                    )
                    NothingButton(
                        text = "Sign Up",
                        onClick = onSignUp,
                        isLoading = uiState.isAuthenticating,
                        modifier = Modifier.weight(1f),
                        testTag = "sign_up_button"
                    )
                }
            }
        }
    }
}

@Composable
private fun AppInfoCard() {
    val colors = LocalNothingColors.current

    NothingCard(modifier = Modifier.fillMaxWidth()) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "REELLOG",
                    style = TextStyle(
                        fontFamily = DotFontFamily,
                        fontSize = 16.sp,
                        color = colors.primaryText,
                        letterSpacing = 1.sp
                    )
                )
                Text(
                    text = "v1.0.0",
                    style = TextStyle(
                        fontFamily = NothingMonoFamily,
                        fontSize = 12.sp,
                        color = colors.accentRed
                    )
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Built for video editors tracking daily client reel outputs. Follows the minimalist visual identity of Nothing OS with dot-matrix typography, immutable submissions, and Asia/Kolkata timezone support.",
                style = TextStyle(
                    fontFamily = NothingSansFamily,
                    fontSize = 12.sp,
                    lineHeight = 17.sp,
                    color = colors.secondaryText
                )
            )
        }
    }
}

@Composable
private fun AddClientDialog(
    name: String,
    onNameChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    val colors = LocalNothingColors.current

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.cardBackground,
        title = {
            Text(
                text = "ADD CLIENT",
                style = TextStyle(
                    fontFamily = NothingMonoFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = colors.primaryText,
                    letterSpacing = 1.sp
                )
            )
        },
        text = {
            Column {
                Text(
                    text = "Enter client name. Active clients will appear in today's daily dashboard.",
                    style = TextStyle(
                        fontFamily = NothingSansFamily,
                        fontSize = 13.sp,
                        color = colors.secondaryText
                    )
                )
                Spacer(modifier = Modifier.height(10.dp))
                NothingTextField(
                    value = name,
                    onValueChange = onNameChange,
                    placeholder = "Client Name",
                    isMono = false,
                    testTag = "settings_dialog_client_name_input"
                )
            }
        },
        confirmButton = {
            NothingButton(
                text = "SAVE CLIENT",
                onClick = onConfirm,
                isPrimary = true,
                enabled = name.isNotBlank(),
                testTag = "settings_dialog_save_client_button"
            )
        },
        dismissButton = {
            NothingButton(
                text = "CANCEL",
                onClick = onDismiss
            )
        }
    )
}

@Composable
private fun EditClientDialog(
    initialName: String,
    currentName: String,
    onNameChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    val colors = LocalNothingColors.current

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.cardBackground,
        title = {
            Text(
                text = "EDIT CLIENT NAME",
                style = TextStyle(
                    fontFamily = NothingMonoFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = colors.primaryText,
                    letterSpacing = 1.sp
                )
            )
        },
        text = {
            Column {
                Text(
                    text = "Updating client name will update past references automatically.",
                    style = TextStyle(
                        fontFamily = NothingSansFamily,
                        fontSize = 13.sp,
                        color = colors.secondaryText
                    )
                )
                Spacer(modifier = Modifier.height(10.dp))
                NothingTextField(
                    value = currentName,
                    onValueChange = onNameChange,
                    placeholder = initialName,
                    isMono = false
                )
            }
        },
        confirmButton = {
            NothingButton(
                text = "UPDATE",
                onClick = onConfirm,
                isPrimary = true,
                enabled = currentName.isNotBlank() && currentName != initialName
            )
        },
        dismissButton = {
            NothingButton(
                text = "CANCEL",
                onClick = onDismiss
            )
        }
    )
}

@Composable
private fun ToggleActiveConfirmDialog(
    client: Client,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    val colors = LocalNothingColors.current
    val willDeactivate = client.isActive

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.cardBackground,
        title = {
            Text(
                text = if (willDeactivate) "DEACTIVATE CLIENT?" else "ACTIVATE CLIENT?",
                style = TextStyle(
                    fontFamily = NothingMonoFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = colors.primaryText,
                    letterSpacing = 1.sp
                )
            )
        },
        text = {
            Text(
                text = if (willDeactivate) {
                    "Deactivating '${client.clientName}' will remove them from today's daily dashboard. All past historical reports and completed reel codes remain permanently preserved."
                } else {
                    "Activating '${client.clientName}' will display them in today's daily dashboard for recording new reel codes."
                },
                style = TextStyle(
                    fontFamily = NothingSansFamily,
                    fontSize = 13.sp,
                    color = colors.secondaryText
                )
            )
        },
        confirmButton = {
            NothingButton(
                text = if (willDeactivate) "DEACTIVATE" else "ACTIVATE",
                onClick = onConfirm,
                isAccentRed = willDeactivate,
                isPrimary = !willDeactivate
            )
        },
        dismissButton = {
            NothingButton(
                text = "CANCEL",
                onClick = onDismiss
            )
        }
    )
}

@Composable
private fun SqlMigrationDialog(
    context: Context,
    onDismiss: () -> Unit
) {
    val colors = LocalNothingColors.current
    val scrollState = rememberScrollState()

    val sqlSnippet = """
-- ReelLog Supabase Schema (Stored in /reellog_supabase_schema.sql)
-- Run this in your Supabase SQL Editor:

CREATE TABLE IF NOT EXISTS public.clients (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    client_name TEXT NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT timezone('utc'::text, now()),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT timezone('utc'::text, now()),
    CONSTRAINT uq_user_client_name UNIQUE (user_id, client_name)
);

CREATE TABLE IF NOT EXISTS public.daily_reports (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    work_date DATE NOT NULL,
    status TEXT NOT NULL DEFAULT 'DRAFT' CHECK (status IN ('DRAFT', 'SUBMITTED')),
    total_reels INTEGER NOT NULL DEFAULT 0,
    submitted_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT timezone('utc'::text, now()),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT timezone('utc'::text, now()),
    CONSTRAINT uq_user_work_date UNIQUE (user_id, work_date)
);

CREATE TABLE IF NOT EXISTS public.daily_report_entries (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    report_id UUID NOT NULL REFERENCES public.daily_reports(id) ON DELETE CASCADE,
    client_id UUID NOT NULL REFERENCES public.clients(id) ON DELETE RESTRICT,
    reel_codes TEXT[] NOT NULL DEFAULT '{}',
    created_at TIMESTAMPTZ NOT NULL DEFAULT timezone('utc'::text, now()),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT timezone('utc'::text, now()),
    CONSTRAINT uq_report_client UNIQUE (report_id, client_id)
);

-- Triggers & RPC submit_daily_report() included in /reellog_supabase_schema.sql!
""".trimIndent()

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.cardBackground,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "SUPABASE SQL SCHEMA",
                    style = TextStyle(
                        fontFamily = NothingMonoFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = colors.primaryText,
                        letterSpacing = 1.sp
                    )
                )
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = colors.secondaryText
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(320.dp)
                    .verticalScroll(scrollState)
            ) {
                Text(
                    text = "Copy and execute this script in your Supabase SQL Editor. It sets up strict RLS, immutable submitted report triggers, and the atomic submit RPC function.",
                    style = TextStyle(
                        fontFamily = NothingSansFamily,
                        fontSize = 12.sp,
                        color = colors.secondaryText
                    )
                )
                Spacer(modifier = Modifier.height(10.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(colors.mainBackground)
                        .border(1.dp, colors.subtleBorder, RoundedCornerShape(6.dp))
                        .padding(10.dp)
                ) {
                    Text(
                        text = sqlSnippet,
                        style = TextStyle(
                            fontFamily = NothingMonoFamily,
                            fontSize = 10.sp,
                            color = colors.primaryText,
                            lineHeight = 14.sp
                        )
                    )
                }
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                NothingButton(
                    text = "Copy Script",
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("ReelLog SQL Migration", sqlSnippet)
                        clipboard.setPrimaryClip(clip)
                    }
                )
                NothingButton(
                    text = "DONE",
                    onClick = onDismiss,
                    isPrimary = true
                )
            }
        }
    )
}
