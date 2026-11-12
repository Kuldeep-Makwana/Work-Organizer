package com.example.ui.screens.history

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.DailyReportWithDetails
import com.example.data.model.HistorySummaryStats
import com.example.ui.components.DraftBadge
import com.example.ui.components.NothingButton
import com.example.ui.components.NothingCard
import com.example.ui.components.NothingTextField
import com.example.ui.components.ReelCodeChip
import com.example.ui.components.SubmittedBadge
import com.example.ui.theme.DotFontFamily
import com.example.ui.theme.LocalNothingColors
import com.example.ui.theme.NothingMonoFamily
import com.example.ui.theme.NothingSansFamily
import com.example.util.DateUtils

@Composable
fun HistoryScreen(
    viewModel: HistoryViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val colors = LocalNothingColors.current

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
                HistoryHeader()
            }

            item {
                HistoryStatsCard(stats = uiState.stats)
            }

            item {
                SearchBar(
                    query = uiState.searchQuery,
                    onQueryChange = { viewModel.onSearchQueryChanged(it) }
                )
            }

            item {
                FilterAndSortRow(
                    selectedDateFilter = uiState.dateFilter,
                    selectedStatusFilter = uiState.statusFilter,
                    sortOrder = uiState.sortOrder,
                    onDateFilterSelected = { viewModel.onDateFilterSelected(it) },
                    onStatusFilterSelected = { viewModel.onStatusFilterSelected(it) },
                    onSortToggle = {
                        val next = if (uiState.sortOrder == SortOrder.NEWEST_FIRST) SortOrder.OLDEST_FIRST else SortOrder.NEWEST_FIRST
                        viewModel.onSortOrderChanged(next)
                    },
                    onResetFilters = { viewModel.resetFilters() }
                )
            }

            if (uiState.filteredReports.isEmpty() && !uiState.isLoading) {
                item {
                    EmptyHistoryCard(hasFilter = uiState.searchQuery.isNotBlank() || uiState.dateFilter != DateFilterOption.ALL)
                }
            } else {
                items(
                    items = uiState.filteredReports,
                    key = { it.report.id }
                ) { reportWithDetails ->
                    ReportHistoryCard(
                        item = reportWithDetails,
                        onClick = { viewModel.selectReportForDetail(reportWithDetails) }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(90.dp))
            }
        }
    }

    // Detail Dialog
    uiState.selectedReportForDetail?.let { detail ->
        ReportDetailDialog(
            detail = detail,
            onDismiss = { viewModel.selectReportForDetail(null) }
        )
    }
}

@Composable
private fun HistoryHeader() {
    val colors = LocalNothingColors.current

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(colors.primaryText)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "WORK HISTORY",
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
            text = "Permanent audit log of all daily reel editing reports",
            style = TextStyle(
                fontFamily = NothingSansFamily,
                fontSize = 13.sp,
                color = colors.secondaryText
            )
        )
    }
}

@Composable
private fun HistoryStatsCard(stats: HistorySummaryStats) {
    val colors = LocalNothingColors.current

    NothingCard(modifier = Modifier.fillMaxWidth()) {
        Column {
            Text(
                text = "ALL-TIME STATS",
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
                Column {
                    Text(
                        text = String.format("%02d", stats.totalSubmittedDays),
                        style = TextStyle(
                            fontFamily = NothingMonoFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 26.sp,
                            color = colors.primaryText
                        )
                    )
                    Text(
                        text = "SUBMITTED DAYS",
                        style = TextStyle(
                            fontFamily = NothingMonoFamily,
                            fontSize = 10.sp,
                            color = colors.secondaryText
                        )
                    )
                }
                Column {
                    Text(
                        text = String.format("%02d", stats.totalReelsCompleted),
                        style = TextStyle(
                            fontFamily = NothingMonoFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 26.sp,
                            color = colors.accentRed
                        )
                    )
                    Text(
                        text = "TOTAL REELS",
                        style = TextStyle(
                            fontFamily = NothingMonoFamily,
                            fontSize = 10.sp,
                            color = colors.secondaryText
                        )
                    )
                }
                Column {
                    Text(
                        text = String.format("%02d", stats.totalClientsWorkedWith),
                        style = TextStyle(
                            fontFamily = NothingMonoFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 26.sp,
                            color = colors.primaryText
                        )
                    )
                    Text(
                        text = "TOTAL CLIENTS",
                        style = TextStyle(
                            fontFamily = NothingMonoFamily,
                            fontSize = 10.sp,
                            color = colors.secondaryText
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit
) {
    val colors = LocalNothingColors.current

    NothingTextField(
        value = query,
        onValueChange = onQueryChange,
        placeholder = "Search client, reel code, or date...",
        modifier = Modifier.fillMaxWidth(),
        isMono = false,
        testTag = "history_search_input"
    )
}

@Composable
private fun FilterAndSortRow(
    selectedDateFilter: DateFilterOption,
    selectedStatusFilter: String?,
    sortOrder: SortOrder,
    onDateFilterSelected: (DateFilterOption) -> Unit,
    onStatusFilterSelected: (String?) -> Unit,
    onSortToggle: () -> Unit,
    onResetFilters: () -> Unit
) {
    val colors = LocalNothingColors.current
    val scrollState = rememberScrollState()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Sort toggle button
        Surface(
            onClick = onSortToggle,
            shape = RoundedCornerShape(6.dp),
            color = colors.elevatedBackground,
            border = androidx.compose.foundation.BorderStroke(1.dp, colors.subtleBorder)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Sort,
                    contentDescription = "Sort",
                    tint = colors.accentRed,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = sortOrder.label.uppercase(),
                    style = TextStyle(
                        fontFamily = NothingMonoFamily,
                        fontSize = 11.sp,
                        color = colors.primaryText
                    )
                )
            }
        }

        // Date filters
        DateFilterOption.entries.forEach { option ->
            val isSelected = selectedDateFilter == option
            Surface(
                onClick = { onDateFilterSelected(option) },
                shape = RoundedCornerShape(6.dp),
                color = if (isSelected) colors.primaryText else colors.elevatedBackground,
                border = if (isSelected) null else androidx.compose.foundation.BorderStroke(1.dp, colors.subtleBorder)
            ) {
                Text(
                    text = option.label.uppercase(),
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

        // Clear filter button
        Surface(
            onClick = onResetFilters,
            shape = RoundedCornerShape(6.dp),
            color = colors.mainBackground,
            border = androidx.compose.foundation.BorderStroke(1.dp, colors.subtleBorder)
        ) {
            Text(
                text = "CLEAR",
                style = TextStyle(
                    fontFamily = NothingMonoFamily,
                    fontSize = 11.sp,
                    color = colors.secondaryText
                ),
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
            )
        }
    }
}

@Composable
private fun ReportHistoryCard(
    item: DailyReportWithDetails,
    onClick: () -> Unit
) {
    val colors = LocalNothingColors.current
    val parsedDate = DateUtils.parseIsoDate(item.report.workDate)
    val formattedDate = DateUtils.formatDisplayDate(parsedDate)

    NothingCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = formattedDate,
                    style = TextStyle(
                        fontFamily = DotFontFamily,
                        fontSize = 16.sp,
                        color = colors.primaryText,
                        letterSpacing = 0.5.sp
                    )
                )

                if (item.report.isSubmitted) {
                    SubmittedBadge()
                } else {
                    DraftBadge()
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Client breakdown list
            item.entries.filter { it.count > 0 }.forEach { clientEntry ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "${clientEntry.clientName} — ${clientEntry.count} reels",
                        style = TextStyle(
                            fontFamily = NothingSansFamily,
                            fontSize = 13.sp,
                            color = colors.secondaryText
                        )
                    )
                }
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 10.dp),
                color = colors.subtleBorder
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "TOTAL: ${item.totalReels} REELS",
                    style = TextStyle(
                        fontFamily = NothingMonoFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = colors.primaryText,
                        letterSpacing = 0.5.sp
                    )
                )

                Text(
                    text = "VIEW DETAILS →",
                    style = TextStyle(
                        fontFamily = NothingMonoFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = colors.accentRed,
                        letterSpacing = 0.5.sp
                    )
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ReportDetailDialog(
    detail: DailyReportWithDetails,
    onDismiss: () -> Unit
) {
    val colors = LocalNothingColors.current
    val parsedDate = DateUtils.parseIsoDate(detail.report.workDate)
    val formattedDate = DateUtils.formatDisplayDate(parsedDate)

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.cardBackground,
        title = {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = formattedDate,
                        style = TextStyle(
                            fontFamily = DotFontFamily,
                            fontSize = 18.sp,
                            color = colors.primaryText
                        )
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = colors.secondaryText
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (detail.report.isSubmitted) {
                        SubmittedBadge()
                    } else {
                        DraftBadge()
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Submitted: ${DateUtils.formatTimestamp(detail.report.submittedAt)}",
                        style = TextStyle(
                            fontFamily = NothingMonoFamily,
                            fontSize = 10.sp,
                            color = colors.secondaryText
                        )
                    )
                }
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "CLIENT WORK BREAKDOWN",
                    style = TextStyle(
                        fontFamily = NothingMonoFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = colors.secondaryText,
                        letterSpacing = 1.sp
                    )
                )
                Spacer(modifier = Modifier.height(10.dp))

                detail.entries.forEach { entry ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = entry.clientName,
                                style = TextStyle(
                                    fontFamily = NothingSansFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = colors.primaryText
                                )
                            )
                            Text(
                                text = "${entry.count} reels",
                                style = TextStyle(
                                    fontFamily = NothingMonoFamily,
                                    fontSize = 12.sp,
                                    color = colors.secondaryText
                                )
                            )
                        }

                        if (entry.reelCodes.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                entry.reelCodes.forEach { code ->
                                    ReelCodeChip(code = code)
                                }
                            }
                        }
                    }
                }

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 12.dp),
                    color = colors.subtleBorder
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "TOTAL REELS COMPLETED",
                        style = TextStyle(
                            fontFamily = NothingMonoFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = colors.accentRed
                        )
                    )
                    Text(
                        text = "${detail.totalReels} REELS",
                        style = TextStyle(
                            fontFamily = NothingMonoFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = colors.primaryText
                        )
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Immutable",
                        tint = colors.secondaryText,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "This submitted record is permanently immutable.",
                        style = TextStyle(
                            fontFamily = NothingSansFamily,
                            fontSize = 11.sp,
                            color = colors.secondaryText
                        )
                    )
                }
            }
        },
        confirmButton = {
            NothingButton(
                text = "CLOSE",
                onClick = onDismiss,
                isPrimary = true
            )
        }
    )
}

@Composable
private fun EmptyHistoryCard(hasFilter: Boolean) {
    val colors = LocalNothingColors.current

    NothingCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = if (hasFilter) "NO MATCHING REPORTS" else "NO REPORTS YET",
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
                text = if (hasFilter) {
                    "Try adjusting your search terms or clearing the date filter."
                } else {
                    "Your submitted daily work reports will appear here."
                },
                style = TextStyle(
                    fontFamily = NothingSansFamily,
                    fontSize = 13.sp,
                    color = colors.secondaryText,
                    textAlign = TextAlign.Center
                )
            )
        }
    }
}
