package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.ReelLogDatabase
import com.example.data.remote.SupabaseClientManager
import com.example.data.repository.ReelLogRepository
import com.example.ui.navigation.ReelLogAppContent
import com.example.ui.screens.dashboard.DashboardViewModel
import com.example.ui.screens.history.HistoryViewModel
import com.example.ui.screens.settings.SettingsViewModel
import com.example.ui.theme.LocalNothingColors
import com.example.ui.theme.ReelLogTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val database = ReelLogDatabase.getDatabase(applicationContext)
        val supabaseManager = SupabaseClientManager(applicationContext)
        val repository = ReelLogRepository(
            clientDao = database.clientDao(),
            reportDao = database.dailyReportDao(),
            entryDao = database.dailyReportEntryDao(),
            supabaseManager = supabaseManager
        )

        val dashboardViewModel by viewModels<DashboardViewModel> {
            DashboardViewModel.Factory(repository)
        }
        val historyViewModel by viewModels<HistoryViewModel> {
            HistoryViewModel.Factory(repository)
        }
        val settingsViewModel by viewModels<SettingsViewModel> {
            SettingsViewModel.Factory(repository, applicationContext)
        }

        setContent {
            val settingsState by settingsViewModel.uiState.collectAsStateWithLifecycle()

            ReelLogTheme(themeMode = settingsState.themeMode) {
                val colors = LocalNothingColors.current
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(colors.mainBackground)
                        .statusBarsPadding()
                ) {
                    ReelLogAppContent(
                        dashboardViewModel = dashboardViewModel,
                        historyViewModel = historyViewModel,
                        settingsViewModel = settingsViewModel
                    )
                }
            }
        }
    }
}
