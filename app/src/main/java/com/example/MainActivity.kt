package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Dns
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.ui.screens.CodeEditorDialog
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.FilesScreen
import com.example.ui.screens.LogsScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.BackgroundDark
import com.example.ui.theme.BorderDark
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.EmeraldActive
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.viewmodel.ServerViewModel

enum class ScreenTab(
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
) {
    DASHBOARD("Servidor", Icons.Filled.Dns, Icons.Outlined.Dns, "tab_dashboard"),
    FILES("Archivos", Icons.Filled.FolderOpen, Icons.Outlined.FolderOpen, "tab_files"),
    LOGS("Registros", Icons.Filled.ReceiptLong, Icons.Outlined.ReceiptLong, "tab_logs"),
    SETTINGS("Ajustes", Icons.Filled.Settings, Icons.Outlined.Settings, "tab_settings")
}

class MainActivity : ComponentActivity() {

    private val viewModel: ServerViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                MainAppContent(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun MainAppContent(viewModel: ServerViewModel) {
    var selectedTab by rememberSaveable { mutableStateOf(ScreenTab.DASHBOARD) }
    val editorState by viewModel.editorState.collectAsState()
    val serverState by viewModel.serverState.collectAsState()

    // Request notification permission on Android 13+
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { /* Permission result handled */ }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    // Handle back button when on sub-screens
    BackHandler(enabled = selectedTab != ScreenTab.DASHBOARD && !editorState.isOpen) {
        selectedTab = ScreenTab.DASHBOARD
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = BackgroundDark,
        bottomBar = {
            NavigationBar(
                containerColor = SurfaceDark,
                tonalElevation = 8.dp,
                modifier = Modifier
                    .border(width = 1.dp, color = BorderDark)
                    .testTag("bottom_nav_bar")
            ) {
                ScreenTab.values().forEach { tab ->
                    val isSelected = selectedTab == tab
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { selectedTab = tab },
                        icon = {
                            Box {
                                Icon(
                                    imageVector = if (isSelected) tab.selectedIcon else tab.unselectedIcon,
                                    contentDescription = tab.title,
                                    modifier = Modifier.size(22.dp)
                                )
                                // Active server dot on dashboard tab
                                if (tab == ScreenTab.DASHBOARD && serverState.isRunning) {
                                    Box(
                                        modifier = Modifier
                                            .size(7.dp)
                                            .background(EmeraldActive, CircleShape)
                                            .align(androidx.compose.ui.Alignment.TopEnd)
                                    )
                                }
                            }
                        },
                        label = {
                            Text(
                                text = tab.title,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = CyanNeon,
                            selectedTextColor = CyanNeon,
                            indicatorColor = CyanNeon.copy(alpha = 0.15f),
                            unselectedIconColor = TextSecondary,
                            unselectedTextColor = TextSecondary
                        ),
                        modifier = Modifier.testTag(tab.testTag)
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (selectedTab) {
                ScreenTab.DASHBOARD -> DashboardScreen(viewModel = viewModel)
                ScreenTab.FILES -> FilesScreen(viewModel = viewModel)
                ScreenTab.LOGS -> LogsScreen(viewModel = viewModel)
                ScreenTab.SETTINGS -> SettingsScreen(viewModel = viewModel)
            }
        }
    }

    // Code Editor Dialog
    if (editorState.isOpen) {
        BackHandler {
            viewModel.closeEditor()
        }
        CodeEditorDialog(
            editorState = editorState,
            onContentChange = { viewModel.updateEditorContent(it) },
            onSave = { viewModel.saveEditorFile() },
            onExecuteCode = { viewModel.executeEditorCode() },
            onExecute24x7 = { viewModel.executeEditor24x7() },
            onDismissResult = { viewModel.dismissExecutionResult() },
            onClose = { viewModel.closeEditor() }
        )
    }
}
