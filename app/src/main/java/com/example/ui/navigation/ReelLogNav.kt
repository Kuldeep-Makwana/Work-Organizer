package com.example.ui.navigation

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.screens.dashboard.DashboardScreen
import com.example.ui.screens.dashboard.DashboardViewModel
import com.example.ui.screens.history.HistoryScreen
import com.example.ui.screens.history.HistoryViewModel
import com.example.ui.screens.settings.SettingsScreen
import com.example.ui.screens.settings.SettingsViewModel
import com.example.ui.theme.DotFontFamily
import com.example.ui.theme.LocalNothingColors
import com.example.ui.theme.NothingMonoFamily

enum class ReelLogTab(
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    DASHBOARD("DASHBOARD", Icons.Filled.Home, Icons.Outlined.Home),
    HISTORY("HISTORY", Icons.Filled.History, Icons.Outlined.History),
    SETTINGS("SETTINGS", Icons.Filled.Settings, Icons.Outlined.Settings)
}

@Composable
fun ReelLogAppContent(
    dashboardViewModel: DashboardViewModel,
    historyViewModel: HistoryViewModel,
    settingsViewModel: SettingsViewModel,
    modifier: Modifier = Modifier
) {
    val colors = LocalNothingColors.current
    var currentTab by rememberSaveable { mutableStateOf(ReelLogTab.DASHBOARD) }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val isWideScreen = maxWidth >= 600.dp

        if (isWideScreen) {
            // Desktop / Tablet side rail layout
            Row(modifier = Modifier.fillMaxSize()) {
                NothingNavigationRail(
                    currentTab = currentTab,
                    onTabSelected = { currentTab = it }
                )
                Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                    Crossfade(targetState = currentTab, label = "tab_crossfade") { tab ->
                        when (tab) {
                            ReelLogTab.DASHBOARD -> DashboardScreen(viewModel = dashboardViewModel)
                            ReelLogTab.HISTORY -> HistoryScreen(viewModel = historyViewModel)
                            ReelLogTab.SETTINGS -> SettingsScreen(viewModel = settingsViewModel)
                        }
                    }
                }
            }
        } else {
            // Mobile bottom navigation layout
            Box(modifier = Modifier.fillMaxSize()) {
                Box(modifier = Modifier.fillMaxSize()) {
                    Crossfade(targetState = currentTab, label = "tab_crossfade") { tab ->
                        when (tab) {
                            ReelLogTab.DASHBOARD -> DashboardScreen(viewModel = dashboardViewModel)
                            ReelLogTab.HISTORY -> HistoryScreen(viewModel = historyViewModel)
                            ReelLogTab.SETTINGS -> SettingsScreen(viewModel = settingsViewModel)
                        }
                    }
                }

                NothingBottomNavBar(
                    currentTab = currentTab,
                    onTabSelected = { currentTab = it },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                )
            }
        }
    }
}

@Composable
private fun NothingBottomNavBar(
    currentTab: ReelLogTab,
    onTabSelected: (ReelLogTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalNothingColors.current

    Surface(
        shape = RoundedCornerShape(24.dp),
        color = colors.cardBackground.copy(alpha = 0.95f),
        border = androidx.compose.foundation.BorderStroke(1.dp, colors.subtleBorder),
        shadowElevation = 8.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            ReelLogTab.entries.forEach { tab ->
                val isSelected = currentTab == tab
                val itemColor = if (isSelected) colors.primaryText else colors.secondaryText

                Column(
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { onTabSelected(tab) }
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .testTag("nav_tab_${tab.name.lowercase()}"),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = if (isSelected) tab.selectedIcon else tab.unselectedIcon,
                        contentDescription = tab.title,
                        tint = itemColor,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = tab.title,
                        style = TextStyle(
                            fontFamily = NothingMonoFamily,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 10.sp,
                            color = itemColor,
                            letterSpacing = 0.5.sp
                        )
                    )
                    if (isSelected) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Box(
                            modifier = Modifier
                                .size(4.dp)
                                .clip(CircleShape)
                                .background(colors.accentRed)
                        )
                    } else {
                        Spacer(modifier = Modifier.height(6.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun NothingNavigationRail(
    currentTab: ReelLogTab,
    onTabSelected: (ReelLogTab) -> Unit
) {
    val colors = LocalNothingColors.current

    Column(
        modifier = Modifier
            .width(180.dp)
            .fillMaxHeight()
            .background(colors.cardBackground)
            .border(
                androidx.compose.foundation.BorderStroke(1.dp, colors.subtleBorder)
            )
            .padding(16.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(colors.accentRed)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "REELLOG",
                    style = TextStyle(
                        fontFamily = DotFontFamily,
                        fontSize = 16.sp,
                        color = colors.primaryText,
                        letterSpacing = 1.sp
                    )
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            ReelLogTab.entries.forEach { tab ->
                val isSelected = currentTab == tab
                val bgColor = if (isSelected) colors.elevatedBackground else Color.Transparent
                val textColor = if (isSelected) colors.primaryText else colors.secondaryText

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(bgColor)
                        .clickable { onTabSelected(tab) }
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                        .testTag("nav_rail_${tab.name.lowercase()}"),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (isSelected) tab.selectedIcon else tab.unselectedIcon,
                        contentDescription = tab.title,
                        tint = if (isSelected) colors.accentRed else textColor,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = tab.title,
                        style = TextStyle(
                            fontFamily = NothingMonoFamily,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 12.sp,
                            color = textColor,
                            letterSpacing = 0.5.sp
                        )
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
            }
        }

        Text(
            text = "NOTHING OS UI\nv1.0.0",
            style = TextStyle(
                fontFamily = NothingMonoFamily,
                fontSize = 10.sp,
                color = colors.secondaryText
            )
        )
    }
}
