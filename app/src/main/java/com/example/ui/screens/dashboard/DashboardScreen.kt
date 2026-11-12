package com.example.ui.screens.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.ClientWorkUiItem
import com.example.data.model.ReelSaveStatus
import com.example.ui.components.DraftBadge
import com.example.ui.components.NothingButton
import com.example.ui.components.NothingCard
import com.example.ui.components.NothingStatusBadge
import com.example.ui.components.NothingTextField
import com.example.ui.components.ReelCodeChip
import com.example.ui.components.SubmittedBadge
import com.example.ui.theme.DotFontFamily
import com.example.ui.theme.LocalNothingColors
import com.example.ui.theme.NothingMonoFamily
import com.example.ui.theme.NothingSansFamily
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val colors = LocalNothingColors.current
    val snackbarHostState = remember { SnackbarHostState() }

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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                DashboardHeader(uiState = uiState)
            }

            item {
                DailySummaryCard(uiState = uiState)
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "CLIENT WORK",
                        style = TextStyle(
                            fontFamily = NothingMonoFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = colors.secondaryText,
                            letterSpacing = 1.sp
                        )
                    )

                    if (uiState.dailyReport?.isSubmitted != true) {
                        NothingButton(
                            text = "+ New Client",
                            onClick = { viewModel.showAddClientDialog(true) },
                            modifier = Modifier.height(36.dp),
                            testTag = "add_client_button"
                        )
                    }
                }
            }

            if (uiState.clientItems.isEmpty() && !uiState.isLoading) {
                item {
                    EmptyClientsCard(onAddClientClick = { viewModel.showAddClientDialog(true) })
                }
            } else {
                items(
                    items = uiState.clientItems,
                    key = { it.client.id }
                ) { item ->
                    ClientWorkRow(
                        item = item,
                        isReportSubmitted = uiState.dailyReport?.isSubmitted == true,
                        onInputChanged = { text -> viewModel.onClientInputChanged(item.client.id, text) },
                        onSaveClicked = { viewModel.saveClientReels(item.client.id) }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
                if (uiState.dailyReport?.isSubmitted == true) {
                    ReportSubmittedLockedCard(uiState = uiState)
                } else {
                    NothingButton(
                        text = "FINAL SUBMIT",
                        onClick = { viewModel.openSubmitConfirmation() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp),
                        isAccentRed = true,
                        enabled = uiState.totalReelsCompleted > 0 && !uiState.isSubmitting,
                        isLoading = uiState.isSubmitting,
                        testTag = "final_submit_button"
                    )
                }
                Spacer(modifier = Modifier.height(90.dp)) // padding for bottom bar
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 80.dp)
        )
    }

    // Submit Confirmation Dialog
    if (uiState.showSubmitConfirmationDialog) {
        SubmitConfirmationDialog(
            uiState = uiState,
            onDismiss = { viewModel.dismissSubmitConfirmation() },
            onConfirm = { viewModel.confirmFinalSubmit() }
        )
    }

    // Add Client Dialog
    if (uiState.showAddClientDialog) {
        AddClientDialog(
            clientName = uiState.newClientInputName,
            onNameChange = { viewModel.onNewClientNameChanged(it) },
            onDismiss = { viewModel.showAddClientDialog(false) },
            onConfirm = { viewModel.addNewClient() }
        )
    }
}

@Composable
private fun DashboardHeader(uiState: DashboardUiState) {
    val colors = LocalNothingColors.current

    Column(modifier = Modifier.fillMaxWidth()) {
        // App brand row with recording indicator
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(colors.accentRed)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "REELLOG",
                    style = TextStyle(
                        fontFamily = DotFontFamily,
                        fontSize = 18.sp,
                        color = colors.primaryText,
                        letterSpacing = 2.sp
                    )
                )
            }

            if (uiState.dailyReport?.isSubmitted == true) {
                SubmittedBadge()
            } else {
                DraftBadge()
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Large Date & Day
        Text(
            text = uiState.dayName,
            style = TextStyle(
                fontFamily = NothingMonoFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                color = colors.accentRed,
                letterSpacing = 1.5.sp
            )
        )
        Text(
            text = uiState.displayDate,
            style = TextStyle(
                fontFamily = DotFontFamily,
                fontSize = 24.sp,
                color = colors.primaryText,
                letterSpacing = 1.sp
            )
        )

        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Daily Work Report (Asia/Kolkata)",
            style = TextStyle(
                fontFamily = NothingSansFamily,
                fontSize = 13.sp,
                color = colors.secondaryText
            )
        )
    }
}

@Composable
private fun DailySummaryCard(uiState: DashboardUiState) {
    val colors = LocalNothingColors.current

    NothingCard(modifier = Modifier.fillMaxWidth()) {
        Column {
            Text(
                text = "DAILY SUMMARY",
                style = TextStyle(
                    fontFamily = NothingMonoFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    color = colors.secondaryText,
                    letterSpacing = 1.sp
                )
            )

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                SummaryMetricItem(
                    count = String.format("%02d", uiState.totalClientsWorked),
                    label = "Clients"
                )
                SummaryMetricItem(
                    count = String.format("%02d", uiState.totalReelsCompleted),
                    label = "Reels"
                )
                SummaryMetricItem(
                    count = String.format("%02d", uiState.totalEntries),
                    label = "Entries"
                )
            }
        }
    }
}

@Composable
private fun SummaryMetricItem(count: String, label: String) {
    val colors = LocalNothingColors.current

    Column(horizontalAlignment = Alignment.Start) {
        Text(
            text = count,
            style = TextStyle(
                fontFamily = NothingMonoFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 28.sp,
                color = colors.primaryText,
                letterSpacing = 0.5.sp
            )
        )
        Text(
            text = label.uppercase(),
            style = TextStyle(
                fontFamily = NothingMonoFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 11.sp,
                color = colors.secondaryText,
                letterSpacing = 0.5.sp
            )
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ClientWorkRow(
    item: ClientWorkUiItem,
    isReportSubmitted: Boolean,
    onInputChanged: (String) -> Unit,
    onSaveClicked: () -> Unit
) {
    val colors = LocalNothingColors.current

    NothingCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Client header row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(if (item.client.isActive) colors.accentRed else colors.secondaryText)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = item.client.clientName,
                        style = TextStyle(
                            fontFamily = NothingSansFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = colors.primaryText
                        )
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${item.reelCodes.size} reels",
                        style = TextStyle(
                            fontFamily = NothingMonoFamily,
                            fontWeight = FontWeight.Medium,
                            fontSize = 12.sp,
                            color = colors.secondaryText
                        )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    NothingStatusBadge(status = item.saveStatus)
                }
            }

            // Saved Reel Chips
            if (item.reelCodes.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    item.reelCodes.forEach { code ->
                        ReelCodeChip(code = code)
                    }
                }
            }

            // Error notice
            if (item.errorMessage != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = item.errorMessage,
                    style = TextStyle(
                        fontFamily = NothingSansFamily,
                        fontSize = 12.sp,
                        color = colors.statusFailed
                    )
                )
            }

            // Input & Save Row (only enabled if not submitted)
            if (!isReportSubmitted) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    NothingTextField(
                        value = item.inputText,
                        onValueChange = onInputChanged,
                        placeholder = "Enter reel codes (e.g. 101, 102)...",
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { onSaveClicked() }),
                        testTag = "input_${item.client.id}"
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    NothingButton(
                        text = "SAVE",
                        onClick = onSaveClicked,
                        isPrimary = true,
                        isLoading = item.saveStatus == ReelSaveStatus.SAVING,
                        enabled = item.inputText.isNotBlank() && item.saveStatus != ReelSaveStatus.SAVING,
                        modifier = Modifier.height(44.dp),
                        testTag = "save_${item.client.id}"
                    )
                }
            }
        }
    }
}

@Composable
private fun ReportSubmittedLockedCard(uiState: DashboardUiState) {
    val colors = LocalNothingColors.current

    NothingCard(
        modifier = Modifier.fillMaxWidth(),
        elevationColor = colors.elevatedBackground,
        borderWidth = 1.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = "Locked",
                tint = colors.accentRed,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "DAILY REPORT SUBMITTED & LOCKED",
                style = TextStyle(
                    fontFamily = NothingMonoFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = colors.primaryText,
                    letterSpacing = 1.sp,
                    textAlign = TextAlign.Center
                )
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "All entries for ${uiState.displayDate} are permanently recorded. Modifications and resubmissions are disabled.",
                style = TextStyle(
                    fontFamily = NothingSansFamily,
                    fontSize = 12.sp,
                    color = colors.secondaryText,
                    textAlign = TextAlign.Center
                )
            )
        }
    }
}

@Composable
private fun EmptyClientsCard(onAddClientClick: () -> Unit) {
    val colors = LocalNothingColors.current

    NothingCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "NO CLIENTS CONFIGURED",
                style = TextStyle(
                    fontFamily = NothingMonoFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = colors.primaryText,
                    letterSpacing = 1.sp
                )
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Add your editing clients to begin tracking reel numbers.",
                style = TextStyle(
                    fontFamily = NothingSansFamily,
                    fontSize = 13.sp,
                    color = colors.secondaryText,
                    textAlign = TextAlign.Center
                )
            )
            Spacer(modifier = Modifier.height(14.dp))
            NothingButton(
                text = "Add First Client",
                onClick = onAddClientClick,
                isPrimary = true,
                testTag = "add_first_client_button"
            )
        }
    }
}

@Composable
private fun SubmitConfirmationDialog(
    uiState: DashboardUiState,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    val colors = LocalNothingColors.current

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.cardBackground,
        title = {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(colors.accentRed)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "SUBMIT DAILY REPORT?",
                        style = TextStyle(
                            fontFamily = NothingMonoFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = colors.primaryText,
                            letterSpacing = 1.sp
                        )
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Date: ${uiState.displayDate}",
                    style = TextStyle(
                        fontFamily = NothingSansFamily,
                        fontSize = 13.sp,
                        color = colors.secondaryText
                    )
                )
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Summary Breakdown:",
                    style = TextStyle(
                        fontFamily = NothingMonoFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp,
                        color = colors.primaryText
                    )
                )
                Spacer(modifier = Modifier.height(8.dp))

                val workedClients = uiState.clientItems.filter { it.reelCodes.isNotEmpty() }
                workedClients.forEach { item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "${item.client.clientName} — ${item.reelCodes.size} reels",
                            style = TextStyle(
                                fontFamily = NothingSansFamily,
                                fontSize = 13.sp,
                                color = colors.primaryText
                            )
                        )
                    }
                }

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 8.dp),
                    color = colors.subtleBorder
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "TOTAL REELS",
                        style = TextStyle(
                            fontFamily = NothingMonoFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = colors.accentRed
                        )
                    )
                    Text(
                        text = "${uiState.totalReelsCompleted} REELS",
                        style = TextStyle(
                            fontFamily = NothingMonoFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = colors.primaryText
                        )
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "ATTENTION: Once submitted, this report is permanent. No reel codes can be added, deleted, or edited.",
                    style = TextStyle(
                        fontFamily = NothingSansFamily,
                        fontSize = 11.sp,
                        color = colors.accentRed
                    )
                )
            }
        },
        confirmButton = {
            NothingButton(
                text = "CONFIRM SUBMISSION",
                onClick = onConfirm,
                isAccentRed = true,
                testTag = "dialog_confirm_submit_button"
            )
        },
        dismissButton = {
            NothingButton(
                text = "CANCEL",
                onClick = onDismiss,
                testTag = "dialog_cancel_button"
            )
        }
    )
}

@Composable
private fun AddClientDialog(
    clientName: String,
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
                text = "ADD NEW CLIENT",
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
                    text = "Enter the name of the client you are editing reels for.",
                    style = TextStyle(
                        fontFamily = NothingSansFamily,
                        fontSize = 13.sp,
                        color = colors.secondaryText
                    )
                )
                Spacer(modifier = Modifier.height(12.dp))
                NothingTextField(
                    value = clientName,
                    onValueChange = onNameChange,
                    placeholder = "e.g. Client Alpha",
                    isMono = false,
                    testTag = "new_client_name_input"
                )
            }
        },
        confirmButton = {
            NothingButton(
                text = "ADD CLIENT",
                onClick = onConfirm,
                isPrimary = true,
                enabled = clientName.isNotBlank(),
                testTag = "dialog_save_new_client_button"
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
