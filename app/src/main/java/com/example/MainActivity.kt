package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModelProvider
import com.example.ui.AppRole
import com.example.ui.AzanViewModel
import com.example.ui.ViewModelFactory
import com.example.ui.maulana.MaulanaDashboard
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.user.UserDashboard

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Initialize our Azan and notice board centralized state
        val viewModel = ViewModelProvider(
            this,
            ViewModelFactory(application)
        )[AzanViewModel::class.java]

        setContent {
            val currentTheme by viewModel.currentTheme.collectAsState()
            MyApplicationTheme(theme = currentTheme) {
                MainAppScaffold(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun MainAppScaffold(viewModel: AzanViewModel) {
    val activeRole by viewModel.appRole.collectAsState()
    val isBroadcasting by viewModel.isBroadcasting.collectAsState()
    val bType by viewModel.broadcastType.collectAsState()

    // Map screen tab index to corresponding enum role
    val currentTab = when (activeRole) {
        AppRole.USER -> 0
        AppRole.MAULANA -> 1
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background)
                    .statusBarsPadding()
            ) {
                // Header Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = if (isBroadcasting) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(34.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = if (isBroadcasting) "📡" else "🕌",
                                    fontSize = 18.sp
                                )
                            }
                        }
                        Column {
                            Text(
                                text = "Azan & Mahalla",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                letterSpacing = (-0.5).sp
                            )
                            Text(
                                text = "Local Transmit Control Nodes",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Blinking notification bar if Live is active globally
                    if (isBroadcasting) {
                        Surface(
                            shape = MaterialTheme.shapes.extraSmall,
                            color = MaterialTheme.colorScheme.errorContainer,
                            modifier = Modifier.padding(start = 8.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .background(Color.Red, shape = MaterialTheme.shapes.extraSmall)
                                )
                                Text(
                                    text = bType?.uppercase() ?: "LIVE",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            }
        },
        bottomBar = {
            // Elegant Navigation bar matching M3 standards with visual indicators & safety padding
            NavigationBar(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .testTag("app_navigation_bar")
            ) {
                NavigationBarItem(
                    selected = currentTab == 0,
                    onClick = { viewModel.selectRole(AppRole.USER) },
                    icon = { Icon(Icons.Default.PlayArrow, contentDescription = null) },
                    label = { Text("Listener Node", fontWeight = FontWeight.Bold) },
                    modifier = Modifier.testTag("nav_listener_tab")
                )

                NavigationBarItem(
                    selected = currentTab == 1,
                    onClick = { viewModel.selectRole(AppRole.MAULANA) },
                    icon = { Icon(Icons.Default.Lock, contentDescription = null) },
                    label = { Text("Mosque Admin", fontWeight = FontWeight.Bold) },
                    modifier = Modifier.testTag("nav_admin_tab")
                )
            }
        }
    ) { innerPadding ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            color = MaterialTheme.colorScheme.background
        ) {
            AnimatedContent(
                targetState = activeRole,
                transitionSpec = {
                    val slideDirection = if (targetState == AppRole.MAULANA) {
                        AnimatedContentTransitionScope.SlideDirection.Left
                    } else {
                        AnimatedContentTransitionScope.SlideDirection.Right
                    }
                    slideIntoContainer(
                        towards = slideDirection,
                        animationSpec = tween(durationMillis = 300)
                    ) togetherWith slideOutOfContainer(
                        towards = slideDirection,
                        animationSpec = tween(durationMillis = 300)
                    )
                },
                label = "roleScreenTransition"
            ) { role ->
                when (role) {
                    AppRole.USER -> {
                        UserDashboard(viewModel = viewModel)
                    }
                    AppRole.MAULANA -> {
                        MaulanaDashboard(viewModel = viewModel)
                    }
                }
            }
        }
    }
}
