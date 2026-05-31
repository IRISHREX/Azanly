package com.example.ui.user

import android.os.Bundle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.local.NoticeEntity
import com.example.data.local.IqamahTimingEntity
import com.example.data.local.DonationRecordEntity
import com.example.ui.AzanViewModel
import com.example.ui.shared.SoundWaveVisualizer
import com.example.ui.theme.AppTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

@Composable
fun UserDashboard(
    viewModel: AzanViewModel,
    modifier: Modifier = Modifier
) {
    val isBroadcasting by viewModel.isBroadcasting.collectAsState()
    val bType by viewModel.broadcastType.collectAsState()
    val bTitle by viewModel.broadcastTitle.collectAsState()
    val bAmplitude by viewModel.audioAmplitude.collectAsState()
    val activeNotices by viewModel.allNotices.collectAsState()
    val activeTimings by viewModel.allIqamahTimings.collectAsState()
    val overrideSilence by viewModel.overrideSilentMode.collectAsState()
    val muteDuringWork by viewModel.muteDuringWorkHours.collectAsState()
    val activeLogs by viewModel.systemAlertLogs.collectAsState()
    val streamLatencyMs by viewModel.streamLatencyMs.collectAsState()
    val synthBaseFrequency by viewModel.synthBaseFrequency.collectAsState()
    val vibrateOnAlert by viewModel.vibrateOnAlert.collectAsState()

    val isRadioTransmitting by viewModel.radioTransmitting.collectAsState()
    val radioTransmittedFreq by viewModel.radioTransmittedFrequency.collectAsState()
    val userRadioReceiverEnabled by viewModel.userRadioReceiverEnabled.collectAsState()
    val userRadioTunedFreq by viewModel.userRadioTunedFrequency.collectAsState()
    val isMeshRepeaterActive by viewModel.isMeshRepeaterActive.collectAsState()
    val activeMeshNodes by viewModel.activeMeshNodes.collectAsState()
    val meshTotalCoverage by viewModel.meshTotalCoverageMeters.collectAsState()

    val currentTheme by viewModel.currentTheme.collectAsState()
    val totalDonationsList by viewModel.allDonations.collectAsState()

    val userLat by viewModel.userLatitude.collectAsState()
    val userLng by viewModel.userLongitude.collectAsState()
    val locationName by viewModel.locationName.collectAsState()
    val locationMethod by viewModel.locationMethod.collectAsState()
    val qiblaBearing by viewModel.qiblaBearing.collectAsState()
    val systemTimezone by viewModel.systemTimezone.collectAsState()

    val context = LocalContext.current

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[android.Manifest.permission.ACCESS_FINE_LOCATION] == true
        val coarseGranted = permissions[android.Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (fineGranted || coarseGranted) {
            viewModel.addSystemLog("GPS location permissions successfully updated.")
            requestSystemLocation(context, viewModel)
        } else {
            viewModel.addSystemLog("⚠️ GPS activation declined. Using cached preset location coords.")
        }
    }

    var showDonationDialog by rememberSaveable { mutableStateOf(false) }
    var soundVolume by rememberSaveable { mutableStateOf(0.8f) }
    var isStreamMutedLocal by rememberSaveable { mutableStateOf(false) }

    var profileName by rememberSaveable { mutableStateOf("Irish Rex") }
    var profileEmail by rememberSaveable { mutableStateOf("irishrex1@gmail.com") }
    var profileMemberId by rememberSaveable { mutableStateOf("MH-2026-8947") }

    var userSelectedTab by rememberSaveable { mutableStateOf(0) }
    var clockTrigger by rememberSaveable { mutableStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            clockTrigger++
        }
    }

    val today = remember(clockTrigger) { java.time.LocalDate.now() }
    val formattedGregorianDate = remember(today) {
        today.format(java.time.format.DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", java.util.Locale.US))
    }
    
    val formattedHijriDate = remember(today) {
        val jd = today.toEpochDay() + 2440588
        val l = jd - 1948440 + 10632
        val n = ((l - 1) / 10631).toInt()
        val lMod = (l - 1) % 10631
        val calendarYear = (30 * n + (11 * lMod + 3) / 10631) + 1
        var day = lMod - (11 * (lMod * 30 + 3) / 10631) / 30
        var month = 1
        val monthLengths = listOf(30, 29, 30, 29, 30, 29, 30, 29, 30, 29, 30, 30)
        for (m in 0 until 12) {
            val length = if (m == 11 && (11 * calendarYear + 14) % 30 < 11) 30 else monthLengths[m]
            if (day > length) {
                day -= length
                month++
            } else break
        }
        val hijriMonths = listOf(
            "Muharram", "Safar", "Rabi' al-Awwal", "Rabi' al-Thani",
            "Jumada al-Awwal", "Jumada al-Thani", "Rajab", "Sha'ban",
            "Ramadan", "Shawwal", "Dhu al-Qadah", "Dhu al-Hijjah"
        )
        "${day.coerceAtLeast(1)} ${hijriMonths.getOrElse(month - 1) { "Ramadan" }} 1447 AH"
    }

    var simHeading by rememberSaveable { mutableStateOf(105f) }
    var lastLoggedAlignment by rememberSaveable { mutableStateOf<Boolean?>(null) }
    var mutedPrayers by rememberSaveable(
        stateSaver = Saver<Set<String>, List<String>>(
            save = { it.toList() },
            restore = { it.toSet() }
        )
    ) { mutableStateOf(setOf<String>()) }

    val nextPrayerCountdown = remember(activeTimings, clockTrigger) {
        viewModel.calculateNextPrayerCountdown(activeTimings)
    }

    val currentNextPrayer = nextPrayerCountdown?.prayerName
    val isCurrentPrayerMuted = currentNextPrayer != null && mutedPrayers.contains(currentNextPrayer)
    val shouldMuteLocally = isStreamMutedLocal || muteDuringWork || (isBroadcasting && bType == "azan" && isCurrentPrayerMuted)

    LaunchedEffect(soundVolume, shouldMuteLocally) {
        viewModel.setPlaybackVolume(if (shouldMuteLocally) 0f else soundVolume)
    }

    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val subTabs = listOf(
                Triple("Home & Prayers", Icons.Default.Home, "home"),
                Triple("Audio Channel", Icons.Default.PlayArrow, "audio"),
                Triple("Qibla Compass", Icons.Default.LocationOn, "qibla"),
                Triple("Notices & Sadaqah", Icons.Default.Notifications, "community"),
                Triple("Profile & Settings", Icons.Default.Person, "profile")
            )
            subTabs.forEachIndexed { index, tab ->
                val isSelected = userSelectedTab == index
                val capBgColor by animateColorAsState(
                    targetValue = if (isSelected) MaterialTheme.colorScheme.primary 
                                  else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                    label = "capBg"
                )
                val capFgColor by animateColorAsState(
                    targetValue = if (isSelected) Color.White 
                                  else MaterialTheme.colorScheme.onSurfaceVariant,
                    label = "capFg"
                )

                Surface(
                    onClick = { userSelectedTab = index },
                    shape = RoundedCornerShape(20.dp),
                    color = capBgColor,
                    contentColor = capFgColor,
                    modifier = Modifier.testTag("user_sub_tab_${tab.third}")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(tab.second, contentDescription = tab.first, modifier = Modifier.size(15.dp))
                        Text(tab.first, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            when (userSelectedTab) {
                0 -> UserHomeScreen(
                    formattedGregorianDate = formattedGregorianDate,
                    formattedHijriDate = formattedHijriDate,
                    nextPrayerCountdown = nextPrayerCountdown,
                    activeTimings = activeTimings,
                    viewModel = viewModel,
                    onSupportClick = { showDonationDialog = true }
                )
                1 -> UserBroadcastScreen(
                    isBroadcasting = isBroadcasting,
                    bType = bType,
                    bTitle = bTitle,
                    bAmplitude = bAmplitude,
                    muteDuringWork = muteDuringWork,
                    isStreamMutedLocal = isStreamMutedLocal,
                    soundVolume = soundVolume,
                    streamLatencyMs = streamLatencyMs,
                    onMuteToggle = { isStreamMutedLocal = !isStreamMutedLocal },
                    onVolumeChange = { soundVolume = it },
                    userRadioReceiverEnabled = userRadioReceiverEnabled,
                    userRadioTunedFreq = userRadioTunedFreq,
                    isRadioTransmitting = isRadioTransmitting,
                    radioTransmittedFreq = radioTransmittedFreq,
                    isMeshRepeaterActive = isMeshRepeaterActive,
                    activeMeshNodes = activeMeshNodes,
                    meshTotalCoverage = meshTotalCoverage,
                    activeLogs = activeLogs,
                    viewModel = viewModel
                )
                2 -> UserCompassScreen(
                    qiblaBearing = qiblaBearing,
                    simHeading = simHeading,
                    vibrateOnAlert = vibrateOnAlert,
                    lastLoggedAlignment = lastLoggedAlignment,
                    onHeadingChange = { simHeading = it },
                    onLoggedAlignmentChange = { lastLoggedAlignment = it },
                    locationName = locationName,
                    locationMethod = locationMethod,
                    userLat = userLat,
                    userLng = userLng,
                    onSyncGPS = {
                        locationPermissionLauncher.launch(
                            arrayOf(
                                android.Manifest.permission.ACCESS_FINE_LOCATION,
                                android.Manifest.permission.ACCESS_COARSE_LOCATION
                            )
                        )
                    },
                    viewModel = viewModel
                )
                3 -> UserCommunityScreen(
                    activeNotices = activeNotices,
                    totalDonationsList = totalDonationsList,
                    onSupportClick = { showDonationDialog = true }
                )
                4 -> UserProfileSettingsScreen(
                    profileName = profileName,
                    profileEmail = profileEmail,
                    profileMemberId = profileMemberId,
                    onProfileNameChange = { profileName = it },
                    onProfileEmailChange = { profileEmail = it },
                    onProfileMemberIdChange = { profileMemberId = it },
                    totalDonationsList = totalDonationsList,
                    currentTheme = currentTheme,
                    overrideSilence = overrideSilence,
                    muteDuringWork = muteDuringWork,
                    mutedPrayers = mutedPrayers,
                    onMutedPrayersChange = { mutedPrayers = it },
                    synthBaseFrequency = synthBaseFrequency,
                    viewModel = viewModel
                )
            }
        }
    }

    if (showDonationDialog) {
        DonationGatewayDialog(
            onDismiss = { showDonationDialog = false },
            onSubmitDonation = { name, valAmt, note ->
                viewModel.processDonation(name, valAmt, note)
                showDonationDialog = false
            }
        )
    }
}

@Composable
private fun UserHomeScreen(
    formattedGregorianDate: String,
    formattedHijriDate: String,
    nextPrayerCountdown: com.example.ui.PrayerCountdown?,
    activeTimings: List<IqamahTimingEntity>,
    viewModel: AzanViewModel,
    onSupportClick: () -> Unit
) {
    val quotes = remember {
        listOf(
            "Quran 2:153" to "Seek help through patience and prayer. Indeed, Allah is with the patient.",
            "Ahmad" to "The best of people are those who are most beneficial to others.",
            "Quran 94:6" to "Indeed, with hardship, there is ease.",
            "Bukhari" to "Allah does not show mercy to those who do not show mercy to people.",
            "Quran 2:186" to "When My servants ask you concerning Me, indeed I am near."
        )
    }
    var quoteIndex by rememberSaveable { mutableStateOf(0) }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(top = 10.dp, bottom = 24.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Assalamu Alaikum 🙏",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                    Text(
                        text = "My Mahalla",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary,
                        letterSpacing = (-0.5).sp
                    )
                    Spacer(Modifier.height(4.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(Icons.Default.DateRange, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(12.dp))
                            Text(formattedGregorianDate, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                            Text(" • ", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                            Icon(Icons.Default.Star, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(12.dp))
                            Text(formattedHijriDate, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Button(
                    onClick = onSupportClick,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                        contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.testTag("contribute_quick_btn_home")
                ) {
                    Icon(Icons.Default.Favorite, contentDescription = "Support", modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Support", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.2f)),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f))
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(14.dp))
                            Text("DAILY HADITH & REFLECTION", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                        }
                        IconButton(
                            onClick = {
                                quoteIndex = (quoteIndex + 1) % quotes.size
                                viewModel.addSystemLog("Hadith rotation synchronized.")
                            },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = "Refresh Hadith", tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(14.dp))
                        }
                    }
                    val quote = quotes[quoteIndex]
                    Text("“${quote.second}”", style = MaterialTheme.typography.bodyMedium, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic, color = MaterialTheme.colorScheme.onSecondaryContainer)
                    Text("— ${quote.first}", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary, modifier = Modifier.align(Alignment.End))
                }
            }
        }

        item {
            nextPrayerCountdown?.let { countdown ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.25f)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Next Congregational Prayer", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.tertiary)
                        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(countdown.prayerName, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold)
                            Text("Jama'at Countdown:", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(bottom = 2.dp, start = 4.dp))
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                            TimeBox(value = countdown.hours, label = "hrs")
                            Text(":", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.tertiary)
                            TimeBox(value = countdown.minutes, label = "mins")
                        }
                        Text("Iqamah scheduled at ${countdown.iqamahTime} via live transmission sync.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Congregation (Iqamah) Schedule", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    activeTimings.forEach { timing ->
                        val isNext = nextPrayerCountdown?.prayerName == timing.prayerName
                        val containerColor by animateColorAsState(
                            targetValue = if (isNext) MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.35f)
                                          else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
                            label = "color"
                        )
                        Card(
                            modifier = Modifier.weight(1f).testTag("timing_card_${timing.prayerName.lowercase()}"),
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(containerColor = containerColor),
                            border = if (isNext) BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.4f)) else null
                        ) {
                            Column(
                                modifier = Modifier.padding(6.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Text(timing.prayerName, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = if (isNext) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onBackground)
                                Text(timing.iqamahTime.replace(" ", ""), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.ExtraBold, fontSize = 10.sp)
                                Text("A:${timing.adhanTime.replace(" ", "")}", style = MaterialTheme.typography.bodySmall, fontSize = 8.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun UserBroadcastScreen(
    isBroadcasting: Boolean,
    bType: String?,
    bTitle: String,
    bAmplitude: Float,
    muteDuringWork: Boolean,
    isStreamMutedLocal: Boolean,
    soundVolume: Float,
    streamLatencyMs: Int,
    onMuteToggle: () -> Unit,
    onVolumeChange: (Float) -> Unit,
    userRadioReceiverEnabled: Boolean,
    userRadioTunedFreq: Double,
    isRadioTransmitting: Boolean,
    radioTransmittedFreq: Double,
    isMeshRepeaterActive: Boolean,
    activeMeshNodes: Int,
    meshTotalCoverage: Int,
    activeLogs: List<String>,
    viewModel: AzanViewModel
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(top = 10.dp, bottom = 24.dp)
    ) {
        item {
            val liveCardBorderColor by animateColorAsState(
                targetValue = if (isBroadcasting) MaterialTheme.colorScheme.primary else Color.Transparent,
                label = "color"
            )

            Card(
                modifier = Modifier.fillMaxWidth().testTag("user_audio_player"),
                colors = CardDefaults.cardColors(
                    containerColor = if (isBroadcasting) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
                                  else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
                ),
                border = BorderStroke(1.dp, liveCardBorderColor.copy(alpha = 0.4f)),
                shape = RoundedCornerShape(18.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(if (isBroadcasting) MaterialTheme.colorScheme.primary else Color.Gray))
                            Text(if (isBroadcasting) "LIVE BROADCAST SECURE" else "MASJID DORMANT", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.ExtraBold, color = if (isBroadcasting) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        if (isBroadcasting) {
                            Box(modifier = Modifier.clip(RoundedCornerShape(4.dp)).background(MaterialTheme.colorScheme.primary).padding(horizontal = 5.dp, vertical = 2.dp)) {
                                Text(bType?.uppercase() ?: "AUDIO", style = MaterialTheme.typography.labelSmall, color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    if (isBroadcasting) {
                        Text(bTitle, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        SoundWaveVisualizer(amplitude = if (isStreamMutedLocal || muteDuringWork) 0.01f else bAmplitude, isLive = !(isStreamMutedLocal || muteDuringWork))
                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = onMuteToggle, modifier = Modifier.testTag("mute_volume_toggle")) {
                                Icon(imageVector = if (isStreamMutedLocal || soundVolume == 0f) Icons.Default.Clear else Icons.Default.PlayArrow, contentDescription = "Mute Toggle")
                            }
                            Slider(value = if (isStreamMutedLocal) 0f else soundVolume, onValueChange = onVolumeChange, modifier = Modifier.weight(1f))
                            Text("Latency: ${streamLatencyMs}ms", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(start = 6.dp))
                        }
                    } else {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp)) {
                            Text("No Active Satellite Feeds", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            Text("Your terminal stays dormant to save power. When Maulana broadcasts live, custom low-latency cellular signals awaken playback instantly.", style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center, modifier = Modifier.padding(vertical = 4.dp))
                            SoundWaveVisualizer(amplitude = 0.05f, isLive = false)
                        }
                    }
                }
            }
        }

        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Card(
                    modifier = Modifier.weight(1f).testTag("fm_radio_tuner_card"),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, if (userRadioReceiverEnabled) MaterialTheme.colorScheme.primary.copy(alpha = 0.35f) else Color.Transparent)
                ) {
                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("FM Tuner Link", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                            Switch(checked = userRadioReceiverEnabled, onCheckedChange = { viewModel.toggleUserRadioReceiver(it) }, modifier = Modifier.testTag("user_fm_receiver_switch"))
                        }
                        Text("${String.format(java.util.Locale.US, "%.1f", userRadioTunedFreq)} MHz", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold)
                        Slider(
                            value = userRadioTunedFreq.toFloat(),
                            onValueChange = { viewModel.updateUserRadioTunedFrequency(it.toDouble()) },
                            valueRange = 87.5f..108.0f,
                            enabled = userRadioReceiverEnabled
                        )
                    }
                }

                Card(
                    modifier = Modifier.weight(1f).testTag("mesh_network_repeater_card"),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, if (isMeshRepeaterActive) MaterialTheme.colorScheme.secondary.copy(alpha = 0.35f) else Color.Transparent)
                ) {
                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("P2P Mesh Link", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                            Switch(checked = isMeshRepeaterActive, onCheckedChange = { viewModel.toggleMeshRepeater(it) }, modifier = Modifier.testTag("mesh_switch"))
                        }
                        Text("$activeMeshNodes Nodes Active", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold)
                        Text("Coverage: ${meshTotalCoverage}m Relay", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Process Daemon System Logs", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.04f))
                ) {
                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        if (activeLogs.isEmpty()) {
                            Text("Process pending FCM push inputs...", style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace)
                        } else {
                            activeLogs.takeLast(4).forEach { log ->
                                Text(log, style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun UserCompassScreen(
    qiblaBearing: Double,
    simHeading: Float,
    vibrateOnAlert: Boolean,
    lastLoggedAlignment: Boolean?,
    onHeadingChange: (Float) -> Unit,
    onLoggedAlignmentChange: (Boolean?) -> Unit,
    locationName: String,
    locationMethod: String,
    userLat: Double,
    userLng: Double,
    onSyncGPS: () -> Unit,
    viewModel: AzanViewModel
) {
    val qiblaTargetAngle = qiblaBearing.toFloat()
    val difference = Math.abs((simHeading - qiblaTargetAngle)) % 360f
    val normalizedDiff = if (difference > 180f) 360f - difference else difference
    val isAligned = normalizedDiff <= 6f

    LaunchedEffect(isAligned, qiblaTargetAngle) {
        if (isAligned && lastLoggedAlignment != true) {
            viewModel.addSystemLog("Perfect Qibla alignment matched at ${String.format(java.util.Locale.US, "%.1f", qiblaTargetAngle)}°.")
            onLoggedAlignmentChange(true)
        } else if (!isAligned) {
            onLoggedAlignmentChange(false)
        }
    }

    val compassBackground by animateColorAsState(
        targetValue = if (isAligned) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                      else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
        label = "compassBg"
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(top = 10.dp, bottom = 24.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth().testTag("qibla_compass_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = compassBackground)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(if (isAligned) "True Kaaba Alignment matched!" else "Interactive Qibla Tracker", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = if (isAligned) Color(0xFF10B981) else Color.Unspecified)
                    
                    Box(modifier = Modifier.size(130.dp), contentAlignment = Alignment.Center) {
                        androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                            val cp = androidx.compose.ui.geometry.Offset(size.width / 2f, size.height / 2f)
                            val r = size.width / 2f

                            drawCircle(color = Color.Gray.copy(alpha = 0.25f), radius = r, center = cp, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx()))
                            
                            val angleRad = Math.toRadians((simHeading - 90f).toDouble())
                            val needleX = cp.x + r * 0.72f * Math.cos(angleRad).toFloat()
                            val needleY = cp.y + r * 0.72f * Math.sin(angleRad).toFloat()
                            drawLine(color = if (isAligned) Color(0xFF10B981) else Color.Red, start = cp, end = androidx.compose.ui.geometry.Offset(needleX, needleY), strokeWidth = 3.dp.toPx())
                            drawCircle(color = if (isAligned) Color(0xFF10B981) else Color.Red, radius = 4.dp.toPx(), center = cp)

                            val kaabaRelRad = Math.toRadians((qiblaTargetAngle - 90f).toDouble())
                            val kX = cp.x + r * 0.8f * Math.cos(kaabaRelRad).toFloat()
                            val kY = cp.y + r * 0.8f * Math.sin(kaabaRelRad).toFloat()
                            drawCircle(color = Color(0xFFFBBF24), radius = 7.dp.toPx(), center = androidx.compose.ui.geometry.Offset(kX, kY))
                        }
                    }

                    Text("Device Compass Rotation Alignment Angle:", style = MaterialTheme.typography.bodySmall)
                    Slider(
                        value = simHeading,
                        onValueChange = onHeadingChange,
                        valueRange = 0f..360f,
                        modifier = Modifier.fillMaxWidth().testTag("compass_heading_slider")
                    )
                    Text("Kaaba Target direction is static at (${qiblaTargetAngle.toInt()}° E) relative to coordinates.", style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f))
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Satellites Location Parameters", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Text("Coordinates: ${locationName} [Manual Overwrite / Auto-GPS Tracker]", style = MaterialTheme.typography.bodySmall)

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = String.format(java.util.Locale.US, "%.4f", userLat),
                            onValueChange = { it.toDoubleOrNull()?.let { l -> viewModel.setCoordinates(l, userLng, "Overridden Coords", "Manual Entry") } },
                            label = { Text("Latitude", fontSize = 10.sp) },
                            modifier = Modifier.weight(1f).testTag("lat_manual_input"),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = String.format(java.util.Locale.US, "%.4f", userLng),
                            onValueChange = { it.toDoubleOrNull()?.let { l -> viewModel.setCoordinates(userLat, l, "Overridden Coords", "Manual Entry") } },
                            label = { Text("Longitude", fontSize = 10.sp) },
                            modifier = Modifier.weight(1f).testTag("lng_manual_input"),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true
                        )
                    }

                    Button(
                        onClick = onSyncGPS,
                        modifier = Modifier.fillMaxWidth().testTag("sync_gps_btn"),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(Icons.Default.Place, contentDescription = "Run GPS Sync", modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Run sat GPS coordinates sync", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun UserCommunityScreen(
    activeNotices: List<NoticeEntity>,
    totalDonationsList: List<DonationRecordEntity>,
    onSupportClick: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(top = 10.dp, bottom = 24.dp)
    ) {
        item {
            Row(modifier = Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Mahalla Bulletins notice Board", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Text("${activeNotices.size} Dispatched", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
            }
        }

        if (activeNotices.isEmpty()) {
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Box(modifier = Modifier.fillMaxWidth().padding(20.dp), contentAlignment = Alignment.Center) {
                        Text("notice bulletin is empty. Postings will appear instantly.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        } else {
            items(activeNotices, key = { it.id }) { notice ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(notice.category.uppercase(), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            if (notice.isPinned) {
                                Icon(Icons.Default.Star, contentDescription = "Pinned", tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(14.dp))
                            }
                        }
                        Text(notice.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(notice.content, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }

        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Recent Sadaqah Stream", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Button(onClick = onSupportClick, shape = RoundedCornerShape(8.dp)) {
                    Text("Donate Now", fontSize = 11.sp)
                }
            }
        }

        if (totalDonationsList.isEmpty()) {
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Box(modifier = Modifier.fillMaxWidth().padding(14.dp), contentAlignment = Alignment.Center) {
                        Text("No donation records registered today. Secure yours by supporting the welfare fund.", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        } else {
            items(totalDonationsList, key = { it.id }) { donation ->
                Card(
                    modifier = Modifier.fillMaxWidth().testTag("donation_record_${donation.id}"),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Row(modifier = Modifier.fillMaxWidth().padding(10.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column {
                            Text(donation.contributorName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                            donation.message?.let { msg ->
                                if (msg.isNotBlank()) Text("“$msg”", style = MaterialTheme.typography.bodySmall, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
                            }
                        }
                        Text("+$${String.format(java.util.Locale.US, "%.0f", donation.amount)}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.tertiary)
                    }
                }
            }
        }
    }
}

@Composable
private fun UserProfileSettingsScreen(
    profileName: String,
    profileEmail: String,
    profileMemberId: String,
    onProfileNameChange: (String) -> Unit,
    onProfileEmailChange: (String) -> Unit,
    onProfileMemberIdChange: (String) -> Unit,
    totalDonationsList: List<DonationRecordEntity>,
    currentTheme: AppTheme,
    overrideSilence: Boolean,
    muteDuringWork: Boolean,
    mutedPrayers: Set<String>,
    onMutedPrayersChange: (Set<String>) -> Unit,
    synthBaseFrequency: Double,
    viewModel: AzanViewModel
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(top = 10.dp, bottom = 24.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp))
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(
                            modifier = Modifier.size(54.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = profileName.split(" ").map { it.take(1).uppercase() }.joinToString(""),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        Column {
                            Text(profileName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text(profileEmail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Surface(shape = RoundedCornerShape(4.dp), color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.4f), modifier = Modifier.padding(top = 2.dp)) {
                                Text("Active Supporter", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.tertiary, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                            }
                        }
                    }

                    HorizontalDivider()

                    Text("Account and Member Management", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    OutlinedTextField(value = profileName, onValueChange = onProfileNameChange, label = { Text("Display Name") }, singleLine = true, modifier = Modifier.fillMaxWidth().testTag("profile_name_edit_input"))
                    OutlinedTextField(value = profileEmail, onValueChange = onProfileEmailChange, label = { Text("Email Address") }, singleLine = true, modifier = Modifier.fillMaxWidth().testTag("profile_email_edit_input"))
                    OutlinedTextField(value = profileMemberId, onValueChange = onProfileMemberIdChange, label = { Text("Mosque Member ID") }, singleLine = true, modifier = Modifier.fillMaxWidth().testTag("profile_member_id_edit_input"))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Card(modifier = Modifier.weight(1f), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))) {
                            Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Donated Sum", style = MaterialTheme.typography.labelSmall)
                                Text("$${totalDonationsList.sumOf { it.amount }.toInt()}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold)
                            }
                        }
                        Card(modifier = Modifier.weight(1f), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))) {
                            Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Sadaqah Streak", style = MaterialTheme.typography.labelSmall)
                                Text("${totalDonationsList.size} Days", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold)
                            }
                        }
                    }

                    Button(
                        onClick = { viewModel.addSystemLog("UserProfile saved: $profileName (ID: $profileMemberId). Updated successfully.") },
                        modifier = Modifier.fillMaxWidth().testTag("profile_save_btn")
                    ) {
                        Text("Save Profile Changes", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Aesthetics Visual Theme Customizer", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        com.example.ui.theme.AppTheme.values().forEach { theme ->
                            val isSelected = currentTheme == theme
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .border(BorderStroke(1.dp, if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray.copy(alpha = 0.3f)), RoundedCornerShape(8.dp))
                                    .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f) else Color.Transparent)
                                    .clickable { viewModel.changeTheme(theme) }
                                    .padding(vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(theme.icon, contentDescription = theme.displayName, modifier = Modifier.size(16.dp), tint = if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray)
                                    Text(theme.displayName.split(" ").last(), fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Resource Sound & Alerts Settings", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Silent Alert Bypass (High-Priority Adhan)", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                            Text("Audio plays even on silent / DnD.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(checked = overrideSilence, onCheckedChange = { viewModel.setOverrideSilent(it) }, modifier = Modifier.testTag("override_silence_switch"))
                    }

                    HorizontalDivider()

                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Workplace Suppression Mute", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                            Text("Quiet status bar warnings replace live audio waves.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(checked = muteDuringWork, onCheckedChange = { viewModel.setMuteDuringWork(it) }, modifier = Modifier.testTag("mute_during_work_switch"))
                    }

                    HorizontalDivider()

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Selective Prayer Mute Exclusions", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            listOf("Fajr", "Dhuhr", "Asr", "Maghrib", "Isha").forEach { prayer ->
                                val isMuted = mutedPrayers.contains(prayer)
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .border(BorderStroke(1.dp, if (isMuted) Color.Red else Color.Gray.copy(alpha = 0.3f)), RoundedCornerShape(8.dp))
                                        .background(if (isMuted) Color.Red.copy(alpha = 0.08f) else Color.Transparent)
                                        .clickable { onMutedPrayersChange(if (isMuted) mutedPrayers - prayer else mutedPrayers + prayer) }
                                        .padding(vertical = 6.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(prayer, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = if (isMuted) Color.Red else Color.Unspecified)
                                }
                            }
                        }
                    }

                    HorizontalDivider()

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Acoustic Chime base Frequency Pitch Tones", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            listOf(
                                261.63 to "Mellow C4",
                                330.00 to "Peaceful E4",
                                392.00 to "Meditate G4",
                                440.00 to "Bright A4"
                            ).forEach { (freq, name) ->
                                val isActive = synthBaseFrequency == freq
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .border(BorderStroke(1.dp, if (isActive) MaterialTheme.colorScheme.secondary else Color.Gray.copy(alpha = 0.3f)), RoundedCornerShape(8.dp))
                                        .background(if (isActive) MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f) else Color.Transparent)
                                        .clickable { viewModel.setSynthBaseFrequency(freq) }
                                        .padding(vertical = 6.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(name, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = if (isActive) MaterialTheme.colorScheme.secondary else Color.Unspecified)
                                }
                            }
                        }
                        
                        var previewActive by remember { mutableStateOf(false) }
                        val scope = rememberCoroutineScope()
                        Button(
                            onClick = {
                                if (previewActive) {
                                    previewActive = false
                                    viewModel.stopLiveBroadcast()
                                } else {
                                    previewActive = true
                                    scope.launch {
                                        viewModel.triggerLiveAzan("Chime Audition")
                                        delay(2200)
                                        viewModel.stopLiveBroadcast()
                                        previewActive = false
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth().testTag("preview_chime_btn")
                        ) {
                            Text(if (previewActive) "Stop Audition Chime..." else "Audition Chime Synth Tone Preview", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TimeBox(value: Long, label: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.tertiary.copy(alpha = 0.1f))
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(
            text = value.toString().padStart(2, '0'),
            fontSize = 20.sp,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.tertiary
        )
        Text(
            text = label,
            fontSize = 8.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun DonationGatewayDialog(
    onDismiss: () -> Unit,
    onSubmitDonation: (String, Double, String?) -> Unit
) {
    var contributor by remember { mutableStateOf("") }
    var presetAmount by remember { mutableDoubleStateOf(10.0) }
    var customAmountText by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var showQRState by remember { mutableStateOf(false) }

    val resolvedAmount = if (customAmountText.isNotBlank()) customAmountText.toDoubleOrNull() ?: 10.0 else presetAmount

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .testTag("donation_card"),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (!showQRState) {
                    Text(
                        text = "Friday Sadaqah & Zakat Gateway",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center
                    )

                    Text(
                        text = "100% of donations are sent securely to the verified local mosque bank account for maintenance and feeding the poor.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )

                    OutlinedTextField(
                        value = contributor,
                        onValueChange = { contributor = it },
                        label = { Text("Donor Full Name (Optional)") },
                        placeholder = { Text("e.g. Abdullah S.") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Select Amount Preset:", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf(5.0, 10.0, 25.0, 50.0).forEach { amt ->
                                val isSelected = presetAmount == amt && customAmountText.isEmpty()
                                val chipContainerColor by animateColorAsState(
                                    targetValue = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                    label = "color"
                                )
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(chipContainerColor)
                                        .clickable {
                                            presetAmount = amt
                                            customAmountText = ""
                                        }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "$${amt.toInt()}",
                                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                }
                            }
                        }
                    }

                    OutlinedTextField(
                        value = customAmountText,
                        onValueChange = { customAmountText = it },
                        label = { Text("Or Enter Custom Amount ($)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = note,
                        onValueChange = { note = it },
                        label = { Text("Add Blessing Note (Optional)") },
                        placeholder = { Text("e.g. Sadqah Jariyah") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                            Text("Cancel")
                        }
                        Button(
                            onClick = { showQRState = true },
                            modifier = Modifier.weight(1.5f).testTag("generate_qr_btn")
                        ) {
                            Text("Secure Payment")
                        }
                    }
                } else {
                    Text(
                        text = "Scan QR to Complete Donation",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center
                    )

                    Text(
                        text = "Complete the secure gateway transaction of ${NumberFormat.getCurrencyInstance(Locale.US).format(resolvedAmount)}:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )

                    Box(
                        modifier = Modifier
                            .size(160.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White)
                            .padding(12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        androidx.compose.foundation.Canvas(modifier = Modifier.size(140.dp)) {
                            val dWidth = size.width
                            val step = dWidth / 8
                            drawRect(color = Color.Black, topLeft = androidx.compose.ui.geometry.Offset(0f, 0f), size = androidx.compose.ui.geometry.Size(step * 2.5f, step * 2.5f))
                            drawRect(color = Color.White, topLeft = androidx.compose.ui.geometry.Offset(step * 0.4f, step * 0.4f), size = androidx.compose.ui.geometry.Size(step * 1.7f, step * 1.7f))
                            drawRect(color = Color.Black, topLeft = androidx.compose.ui.geometry.Offset(step * 0.7f, step * 0.7f), size = androidx.compose.ui.geometry.Size(step * 1.1f, step * 1.1f))

                            drawRect(color = Color.Black, topLeft = androidx.compose.ui.geometry.Offset(dWidth - step * 2.5f, 0f), size = androidx.compose.ui.geometry.Size(step * 2.5f, step * 2.5f))
                            drawRect(color = Color.White, topLeft = androidx.compose.ui.geometry.Offset(dWidth - step * 2.1f, step * 0.4f), size = androidx.compose.ui.geometry.Size(step * 1.7f, step * 1.7f))
                            drawRect(color = Color.Black, topLeft = androidx.compose.ui.geometry.Offset(dWidth - step * 1.8f, step * 0.7f), size = androidx.compose.ui.geometry.Size(step * 1.1f, step * 1.1f))

                            drawRect(color = Color.Black, topLeft = androidx.compose.ui.geometry.Offset(0f, dWidth - step * 2.5f), size = androidx.compose.ui.geometry.Size(step * 2.5f, step * 2.5f))
                            drawRect(color = Color.White, topLeft = androidx.compose.ui.geometry.Offset(step * 0.4f, dWidth - step * 2.1f), size = androidx.compose.ui.geometry.Size(step * 1.7f, step * 1.7f))
                            drawRect(color = Color.Black, topLeft = androidx.compose.ui.geometry.Offset(step * 0.7f, dWidth - step * 1.8f), size = androidx.compose.ui.geometry.Size(step * 1.1f, step * 1.1f))

                            drawRect(color = Color.Black, topLeft = androidx.compose.ui.geometry.Offset(step * 4f, step * 1f), size = androidx.compose.ui.geometry.Size(step * 0.8f, step * 1.1f))
                            drawRect(color = Color.Black, topLeft = androidx.compose.ui.geometry.Offset(step * 3.5f, step * 4f), size = androidx.compose.ui.geometry.Size(step * 1.5f, step * 0.8f))
                            drawRect(color = Color.Black, topLeft = androidx.compose.ui.geometry.Offset(step * 1f, step * 4f), size = androidx.compose.ui.geometry.Size(step * 1.2f, step * 1.2f))
                            drawRect(color = Color.Black, topLeft = androidx.compose.ui.geometry.Offset(step * 5f, step * 4.5f), size = androidx.compose.ui.geometry.Size(step * 1f, step * 1.6f))
                            drawRect(color = Color.Black, topLeft = androidx.compose.ui.geometry.Offset(step * 4.5f, step * 2.5f), size = androidx.compose.ui.geometry.Size(step * 1.1f, step * 0.7f))

                            drawCircle(color = Color(0xFF0F766E), radius = step * 0.8f, center = androidx.compose.ui.geometry.Offset(dWidth / 2f, dWidth / 2f))
                        }
                    }

                    Text(
                        text = "UPI Code: pay@mahalla-masjid.mosque",
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Button(
                        onClick = {
                            val verifiedName = contributor.ifBlank { "Generous Contributor" }
                            onSubmitDonation(verifiedName, resolvedAmount, note.ifBlank { null })
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("simulate_payment_confirm"),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text("Simulate Payment Success", fontWeight = FontWeight.Bold)
                    }

                    TextButton(onClick = { showQRState = false }) {
                        Text("Back to Config")
                    }
                }
            }
        }
    }
}

fun requestSystemLocation(context: android.content.Context, viewModel: com.example.ui.AzanViewModel) {
    try {
        val locationManager = context.getSystemService(android.content.Context.LOCATION_SERVICE) as android.location.LocationManager
        val hasGps = locationManager.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER)
        val hasNetwork = locationManager.isProviderEnabled(android.location.LocationManager.NETWORK_PROVIDER)

        if (!hasGps && !hasNetwork) {
            viewModel.addSystemLog("[Warning] Location sensors (GPS/Network) are disabled.")
            return
        }

        val locationListener = object : android.location.LocationListener {
            override fun onLocationChanged(location: android.location.Location) {
                val lat = location.latitude
                val lng = location.longitude
                var cityLabel = "Active GPS Coordinate"
                try {
                    val geocoder = android.location.Geocoder(context, java.util.Locale.getDefault())
                    val addresses = geocoder.getFromLocation(lat, lng, 1)
                    if (!addresses.isNullOrEmpty()) {
                        val address = addresses[0]
                        cityLabel = address.locality ?: address.subAdminArea ?: address.adminArea ?: "Located Area"
                        val country = address.countryName
                        if (country != null) {
                            cityLabel = "$cityLabel, $country"
                        }
                    }
                } catch (e: Exception) {
                    cityLabel = "Active GPS Location"
                }
                viewModel.setCoordinates(lat, lng, cityLabel, "Auto-GPS Tracker")
                locationManager.removeUpdates(this)
            }
            override fun onStatusChanged(provider: String?, status: Int, extras: android.os.Bundle?) {}
            override fun onProviderEnabled(provider: String) {}
            override fun onProviderDisabled(provider: String) {}
        }

        if (androidx.core.content.ContextCompat.checkSelfPermission(
                context, android.Manifest.permission.ACCESS_FINE_LOCATION
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED ||
            androidx.core.content.ContextCompat.checkSelfPermission(
                context, android.Manifest.permission.ACCESS_COARSE_LOCATION
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            var lastLocation: android.location.Location? = null
            if (hasNetwork) {
                lastLocation = locationManager.getLastKnownLocation(android.location.LocationManager.NETWORK_PROVIDER)
            }
            if (lastLocation == null && hasGps) {
                lastLocation = locationManager.getLastKnownLocation(android.location.LocationManager.GPS_PROVIDER)
            }

            if (lastLocation != null) {
                val lat = lastLocation.latitude
                val lng = lastLocation.longitude
                var cityLabel = "GPS Location (Cached)"
                try {
                    val geocoder = android.location.Geocoder(context, java.util.Locale.getDefault())
                    val addresses = geocoder.getFromLocation(lat, lng, 1)
                    if (!addresses.isNullOrEmpty()) {
                        val address = addresses[0]
                        cityLabel = (address.locality ?: address.subAdminArea ?: "Cached Place") + ", " + (address.countryName ?: "")
                    }
                } catch (e: Exception) {}
                viewModel.setCoordinates(lat, lng, cityLabel, "Auto-GPS (Last Known)")
            }

            viewModel.addSystemLog("Synchronizing satellites for live positioning...")
            if (hasNetwork) {
                locationManager.requestLocationUpdates(
                    android.location.LocationManager.NETWORK_PROVIDER,
                    0L, 0f, locationListener, android.os.Looper.getMainLooper()
                )
            } else if (hasGps) {
                locationManager.requestLocationUpdates(
                    android.location.LocationManager.GPS_PROVIDER,
                    0L, 0f, locationListener, android.os.Looper.getMainLooper()
                )
            }
        } else {
            viewModel.addSystemLog("[Warning] Location synchronization asks Location permissions check.")
        }
    } catch (e: Exception) {
        viewModel.addSystemLog("Satellite synchronization failed: ${e.message}")
    }
}
