package com.example.ui.user

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.local.NoticeEntity
import com.example.ui.AzanViewModel
import com.example.ui.shared.SoundWaveVisualizer
import kotlinx.coroutines.delay
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

    var showDonationDialog by rememberSaveable { mutableStateOf(false) }
    var soundVolume by rememberSaveable { mutableFloatStateOf(0.8f) }
    var isStreamMutedLocal by rememberSaveable { mutableStateOf(false) }

    // Countdown clock state
    var clockTrigger by rememberSaveable { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            clockTrigger++
        }
    }

    // Interactive Islamic Reflections & Highlights list
    val quotes = remember {
        listOf(
            "Quran 2:153" to "O you who believe, seek help through patience and prayer. Indeed, Allah is with the patient.",
            "Ahmad" to "The best of people are those who are most beneficial to others.",
            "Quran 94:6" to "Indeed, with hardship, there is ease.",
            "Bukhari" to "Allah does not show mercy to those who do not show mercy to people.",
            "Quran 2:186" to "When My servants ask you concerning Me, indeed I am near. I respond to the call of every supplicant."
        )
    }
    var quoteIndex by rememberSaveable { mutableIntStateOf(0) }

    // Sleek Interactive compass simulation states
    var simHeading by rememberSaveable { mutableFloatStateOf(105f) }
    var compassExpanded by rememberSaveable { mutableStateOf(false) }
    var lastLoggedAlignment by rememberSaveable { mutableStateOf<Boolean?>(null) }
    var mutedPrayers by rememberSaveable(
        saver = Saver<MutableState<Set<String>>, List<String>>(
            save = { it.value.toList() },
            restore = { mutableStateOf(it.toSet()) }
        )
    ) { mutableStateOf(setOf<String>()) }

    val nextPrayerCountdown = remember(activeTimings, clockTrigger) {
        viewModel.calculateNextPrayerCountdown(activeTimings)
    }

    // Real-time synchronization of local system options to the media physical playback engine
    val currentNextPrayer = nextPrayerCountdown?.prayerName
    val isCurrentPrayerMuted = currentNextPrayer != null && mutedPrayers.contains(currentNextPrayer)
    
    // Check if the stream should be completely muted locally
    val shouldMuteLocally = isStreamMutedLocal || muteDuringWork || (isBroadcasting && bType == "azan" && isCurrentPrayerMuted)

    LaunchedEffect(soundVolume, shouldMuteLocally) {
        val targetVolume = if (shouldMuteLocally) 0f else soundVolume
        viewModel.setPlaybackVolume(targetVolume)
    }

    // Diagnostic event tracer and state notifier for the User Dashboard log display
    var lastReportedAzanSuppressionState by rememberSaveable { mutableStateOf<String?>(null) }
    LaunchedEffect(isBroadcasting, bType, currentNextPrayer, isCurrentPrayerMuted) {
        if (isBroadcasting && bType == "azan") {
            val key = "$currentNextPrayer:$isCurrentPrayerMuted"
            if (lastReportedAzanSuppressionState != key) {
                lastReportedAzanSuppressionState = key
                if (isCurrentPrayerMuted) {
                    viewModel.addSystemLog("[FCM Suppressed] Live $currentNextPrayer Adhan alert blocked locally by alert exclusion rules.")
                } else if (!isCurrentPrayerMuted && currentNextPrayer != null) {
                    viewModel.addSystemLog("[FCM Played] Incoming live $currentNextPrayer Adhan stream active on device.")
                }
            }
        } else {
            lastReportedAzanSuppressionState = null
        }
    }

    val currentHour = remember(clockTrigger) {
        java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
    }
    val islamicGreeting = remember(currentHour) {
        when (currentHour) {
            in 4..11 -> "Assalamu Alaikum ☀️ Morning Light"
            in 12..15 -> "Assalamu Alaikum 🌤️ Blessed Noontide"
            in 16..20 -> "Assalamu Alaikum 🌅 Peaceful Dusk"
            else -> "Assalamu Alaikum 🌙 Tranquil Night"
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 8.dp, bottom = 32.dp)
    ) {
        // --- 1. Header welcome banner ---
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = islamicGreeting,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                    Text(
                        text = "Mahalla Sync",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary,
                        letterSpacing = (-0.5).sp
                    )
                    Text(
                        text = "Real-time Azan & Live Announcements",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium
                    )
                }

                // Quick Donation shortcut
                Button(
                    onClick = { showDonationDialog = true },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                        contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("contribute_quick_btn"),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Favorite,
                        contentDescription = "Donate",
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text("Support", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // --- Spiritual Daily Reflection Widget ---
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.25f)
                ),
                border = androidx.compose.foundation.BorderStroke(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = "Reflection Icon",
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "DAILY REFLECTION & HADITH",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.secondary,
                                letterSpacing = 1.sp
                            )
                        }

                        IconButton(
                            onClick = {
                                quoteIndex = (quoteIndex + 1) % quotes.size
                                viewModel.addSystemLog("Spiritual reflection rotated dynamically.")
                            },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Next Quote",
                                tint = MaterialTheme.colorScheme.secondary.copy(alpha = 0.8f),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    val quote = quotes[quoteIndex]
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "“${quote.second}”",
                            style = MaterialTheme.typography.bodyMedium,
                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            lineHeight = 18.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "— ${quote.first}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.align(Alignment.End)
                        )
                    }
                }
            }
        }

        // --- 2. Live Audio Streaming Panel (Interactive WebRTC player) ---
        item {
            val liveCardBorderColor by animateColorAsState(
                targetValue = if (isBroadcasting) MaterialTheme.colorScheme.primary else Color.Transparent,
                label = "color"
            )

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("user_audio_player"),
                colors = CardDefaults.cardColors(
                    containerColor = if (isBroadcasting) {
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    }
                ),
                border = androidx.compose.foundation.BorderStroke(1.dp, liveCardBorderColor.copy(alpha = 0.4f)),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Header inside card
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(RoundedCornerShape(5.dp))
                                    .background(if (isBroadcasting) MaterialTheme.colorScheme.primary else Color.Gray)
                            )
                            Text(
                                text = if (isBroadcasting) "LIVE BROADCST ACTIVE" else "MASJID TRANSMITTER OFFLINE",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (isBroadcasting) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        if (isBroadcasting) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(MaterialTheme.colorScheme.primary)
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = bType?.uppercase() ?: "AUDIO",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    if (isBroadcasting) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = bTitle,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            if (muteDuringWork) {
                                Text(
                                    text = "⚠️ Live stream audio is currently muted due to your Workplace discretion rules.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error,
                                    fontWeight = FontWeight.Medium
                                )
                            } else {
                                Text(
                                    text = "Streaming live via low-latency cellular sync. Connected.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        SoundWaveVisualizer(
                            amplitude = if (isStreamMutedLocal || muteDuringWork) 0.01f else bAmplitude,
                            isLive = !(isStreamMutedLocal || muteDuringWork)
                        )

                        // Interactive player parameters
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = { isStreamMutedLocal = !isStreamMutedLocal },
                                modifier = Modifier.testTag("mute_volume_toggle")
                            ) {
                                Icon(
                                    imageVector = if (isStreamMutedLocal || soundVolume == 0f) {
                                        Icons.Default.Clear
                                    } else {
                                        Icons.Default.PlayArrow
                                    },
                                    contentDescription = "Mute or play audio"
                                )
                            }

                            Slider(
                                value = if (isStreamMutedLocal) 0f else soundVolume,
                                onValueChange = {
                                    soundVolume = it
                                    if (it > 0f) isStreamMutedLocal = false
                                },
                                modifier = Modifier.weight(1f)
                            )

                            Text(
                                text = "Latency: ${streamLatencyMs}ms",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                    } else {
                        // Resting state message
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)
                        ) {
                            Text(
                                text = "Loudspeaker Feed Dormant",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Your app stays completely dormant to conserve device battery. When the transmission starts, low-latency cellular sync automatically wakes the live stream player instantly.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                lineHeight = 16.sp
                            )
                            Spacer(Modifier.height(4.dp))
                            SoundWaveVisualizer(amplitude = 0.05f, isLive = false)
                        }
                    }
                }
            }
        }

        // --- 3. Dynamic Jama'at Countdown Module ---
        item {
            nextPrayerCountdown?.let { countdown ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f)
                    ),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Next Congregational Prayer",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.tertiary
                        )

                        Row(
                            verticalAlignment = Alignment.Bottom,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = countdown.prayerName,
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                text = "Jama'at Countdown:",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(bottom = 4.dp, start = 4.dp)
                            )
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TimeBox(value = countdown.hours, label = "hrs")
                            Text(":", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                            TimeBox(value = countdown.minutes, label = "mins")
                        }

                        Text(
                            text = "Iqamah scheduled at ${countdown.iqamahTime} (local mosque sync).",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // --- 4. Subscriptions / Settings Controls (Automatic Wake-up / Mutes) ---
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Resource & Device Integration Settings",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    // Setting 1: Overriding Silent Mode
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                            Text(
                                text = "Override Silent Mode (High-Priority Azan)",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Plays live Azan voice stream at full volume even if device is set on silent/do-not-disturb.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 14.sp
                            )
                        }

                        Switch(
                            checked = overrideSilence,
                            onCheckedChange = { viewModel.setOverrideSilent(it) },
                            modifier = Modifier.testTag("override_silence_switch")
                        )
                    }

                    HorizontalDivider()

                    // Setting 2: Mute during work
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                            Text(
                                text = "Mute Audio During Workplace Hours",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Temporarily suppresses all live sound waves. Replaces broadcast audio with silent, heads-up notices.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 14.sp
                            )
                        }

                        Switch(
                            checked = muteDuringWork,
                            onCheckedChange = { viewModel.setMuteDuringWork(it) },
                            modifier = Modifier.testTag("mute_during_work_switch")
                        )
                    }

                    HorizontalDivider()

                    // Prayer Selective Suppress Alert Configuration
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Selective Prayer Alert Exclusions",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Suppress sound waves and high priority push wake-ups for specific periodic prayer times:",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 14.sp
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf("Fajr", "Dhuhr", "Asr", "Maghrib", "Isha").forEach { prayerName ->
                                val isMuted = mutedPrayers.contains(prayerName)
                                val chipBorderColor = if (isMuted) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                                val chipBgColor = if (isMuted) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f) else Color.Transparent
                                val contentColor = if (isMuted) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(chipBgColor)
                                        .clickable {
                                            if (isMuted) {
                                                mutedPrayers = mutedPrayers - prayerName
                                                viewModel.addSystemLog("Alarm notifications re-enabled for $prayerName.")
                                            } else {
                                                mutedPrayers = mutedPrayers + prayerName
                                                viewModel.addSystemLog("Notification alerts locally muted for $prayerName.")
                                            }
                                        }
                                        .border(BorderStroke(1.dp, chipBorderColor), RoundedCornerShape(10.dp))
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            text = prayerName,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = contentColor
                                        )
                                        Text(
                                            text = if (isMuted) "MUTED" else "SOUND",
                                            fontSize = 7.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isMuted) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // --- 5. Mosque Timetable card list ---
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Congregation (Iqamah) Schedule",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    activeTimings.forEach { timing ->
                        val isNext = nextPrayerCountdown?.prayerName == timing.prayerName
                        val containerColor by animateColorAsState(
                            targetValue = if (isNext) MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.4f)
                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                            label = "color"
                        )

                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .testTag("timing_card_${timing.prayerName.lowercase()}"),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = containerColor),
                            border = if (isNext) androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.5f)) else null
                        ) {
                            Column(
                                modifier = Modifier.padding(8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = timing.prayerName,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isNext) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onBackground
                                )
                                Text(
                                    text = timing.iqamahTime.replace(" ", ""),
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 11.sp
                                )
                                Text(
                                    text = "A:${timing.adhanTime.replace(" ", "")}",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontSize = 9.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }

        // --- Interactive Qibla Compass Simulator Module ---
        item {
            val qiblaTargetAngle = 135f // 135 degrees East as perfect Qibla bearing
            val difference = Math.abs((simHeading - qiblaTargetAngle)) % 360f
            val normalizedDiff = if (difference > 180f) 360f - difference else difference
            val isAligned = normalizedDiff <= 6f
            
            // Periodically log perfect alignment once
            LaunchedEffect(isAligned) {
                if (isAligned && lastLoggedAlignment != true) {
                    viewModel.addSystemLog("Qibla compass target verified: Signal aligned at 135° East.")
                    lastLoggedAlignment = true
                } else if (!isAligned) {
                    lastLoggedAlignment = false
                }
            }

            val compassBackground by animateColorAsState(
                targetValue = if (isAligned) Color(0xFF0F766E).copy(alpha = 0.15f)
                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                label = "compassBg"
            )

            val arrowColor by animateColorAsState(
                targetValue = if (isAligned) Color(0xFF10B981) else MaterialTheme.colorScheme.primary,
                label = "arrowColor"
            )

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("qibla_compass_card"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = compassBackground)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { compassExpanded = !compassExpanded },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = "Qibla Direction Icon",
                                tint = if (isAligned) Color(0xFF10B981) else MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Column {
                                Text(
                                    text = "Interactive Qibla Compass",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isAligned) Color(0xFF0F766E) else MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = if (isAligned) "Perfect Alignment Achieved! 🕋" else "Rotate slider to align with Kaaba (135°)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (isAligned) Color(0xFF0F766E).copy(alpha = 0.9f) else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        Icon(
                            imageVector = if (compassExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = "Expand/Collapse Compass",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (compassExpanded) {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // The custom styled canvas compass pointer dial
                            Box(
                                modifier = Modifier
                                    .size(130.dp)
                                    .padding(8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                                    val center = androidx.compose.ui.geometry.Offset(size.width / 2f, size.height / 2f)
                                    val r = size.width / 2f

                                    // Compass Dial ring background
                                    drawCircle(
                                        color = Color.Gray.copy(alpha = 0.2f),
                                        radius = r,
                                        center = center,
                                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx())
                                    )

                                    // North/South reference ticks
                                    val tickLength = 8.dp.toPx()
                                    // North tick
                                    drawLine(
                                        color = Color.Red.copy(alpha = 0.7f),
                                        start = androidx.compose.ui.geometry.Offset(center.x, 0f),
                                        end = androidx.compose.ui.geometry.Offset(center.x, tickLength),
                                        strokeWidth = 3.dp.toPx()
                                    )
                                    // South tick
                                    drawLine(
                                        color = Color.Gray,
                                        start = androidx.compose.ui.geometry.Offset(center.x, size.height),
                                        end = androidx.compose.ui.geometry.Offset(center.x, size.height - tickLength),
                                        strokeWidth = 2.dp.toPx()
                                    )
                                    // East tick
                                    drawLine(
                                        color = Color.Gray,
                                        start = androidx.compose.ui.geometry.Offset(size.width, center.y),
                                        end = androidx.compose.ui.geometry.Offset(size.width - tickLength, center.y),
                                        strokeWidth = 2.dp.toPx()
                                    )
                                    // West tick
                                    drawLine(
                                        color = Color.Gray,
                                        start = androidx.compose.ui.geometry.Offset(0f, center.y),
                                        end = androidx.compose.ui.geometry.Offset(tickLength, center.y),
                                        strokeWidth = 2.dp.toPx()
                                    )

                                    // 1. Draw Simulated phone's primary orientation alignment vector
                                    // Rotate coordinates based on simHeading parameter
                                    val angleRad = Math.toRadians((simHeading - 90f).toDouble())
                                    val needleX = center.x + r * 0.75f * Math.cos(angleRad).toFloat()
                                    val needleY = center.y + r * 0.75f * Math.sin(angleRad).toFloat()

                                    drawLine(
                                        color = arrowColor,
                                        start = center,
                                        end = androidx.compose.ui.geometry.Offset(needleX, needleY),
                                        strokeWidth = 4.dp.toPx()
                                    )
                                    drawCircle(
                                        color = arrowColor,
                                        radius = 5.dp.toPx(),
                                        center = center
                                    )

                                    // 2. Draw Kaaba vector position mark
                                    // Kaaba sits statically at qiblaTargetAngle Eastern bearing, relative to our current dial.
                                    val kaabaRelAngleRad = Math.toRadians((qiblaTargetAngle - 90).toDouble())
                                    val kX = center.x + r * 0.82f * Math.cos(kaabaRelAngleRad).toFloat()
                                    val kY = center.y + r * 0.82f * Math.sin(kaabaRelAngleRad).toFloat()

                                    // Yellow gold dot indicating pristine holy direction coordinates
                                    drawCircle(
                                        color = Color(0xFFFBBF24),
                                        radius = 8.dp.toPx(),
                                        center = androidx.compose.ui.geometry.Offset(kX, kY)
                                    )
                                    drawCircle(
                                        color = Color.White,
                                        radius = 4.dp.toPx(),
                                        center = androidx.compose.ui.geometry.Offset(kX, kY)
                                    )
                                }
                            }

                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "Current: ${simHeading.toInt()}°",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Qibla: ${qiblaTargetAngle.toInt()}° E",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                Spacer(modifier = Modifier.height(4.dp))
                                Button(
                                    onClick = {
                                        simHeading = qiblaTargetAngle
                                        viewModel.addSystemLog("Quick-realigned simulator with perfect mosque coordinates.")
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isAligned) Color(0xFF10B981) else MaterialTheme.colorScheme.primary
                                    ),
                                    modifier = Modifier.fillMaxWidth(),
                                    contentPadding = PaddingValues(vertical = 4.dp, horizontal = 8.dp)
                                ) {
                                    Text("Perfect Align", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "Simulate Phone Orientation / Azimuth Drift:",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Bold
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Slider(
                                    value = simHeading,
                                    onValueChange = { simHeading = it },
                                    valueRange = 0f..360f,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = "${simHeading.toInt()}°",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.width(32.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // --- 6. Local Announcement Bulletin Board ---
        item {
            Text(
                text = "Mahalla Notice Board",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }

        if (activeNotices.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                    )
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Notice board is empty. Updates dispatched by Mosque admin will show up here instantly.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
            items(activeNotices, key = { it.id }) { notice ->
                val categoryColor = remember(notice.category) {
                    when (notice.category.lowercase()) {
                        "jama'at", "iqamah" -> Color(0xFFD97706) // Warm Amber
                        "khutbah" -> Color(0xFF0EA5E9)          // Blue
                        "funeral" -> Color(0xFFEF4444)          // Soft Red
                        "donation" -> Color(0xFF8B5CF6)         // Purple
                        else -> Color(0xFF0F766E)               // Spiritual Emerald Teal
                    }
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(IntrinsicSize.Max)
                    ) {
                        // Left-side category bar accent
                        Box(
                            modifier = Modifier
                                .width(6.dp)
                                .fillMaxHeight()
                                .heightIn(min = 96.dp)
                                .background(categoryColor)
                        )

                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    if (notice.isPinned) {
                                        Icon(
                                            imageVector = Icons.Default.Star,
                                            contentDescription = "Pinned Announcement",
                                            tint = MaterialTheme.colorScheme.tertiary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(categoryColor.copy(alpha = 0.15f))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = notice.category.uppercase(),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = categoryColor,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                Text(
                                    text = "Synced",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp
                                )
                            }

                            Text(
                                text = notice.title,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )

                            Text(
                                text = notice.content,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // --- 7. System alerts and Daemon Logs logs ---
        item {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Active FCM & AlarmManager Diagnostics Logger",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (activeLogs.isEmpty()) {
                            Text(
                                text = "Listener process awaiting High Priority FCM messages...",
                                style = MaterialTheme.typography.bodySmall,
                                fontFamily = FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            activeLogs.forEach { log ->
                                Text(
                                    text = log,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontFamily = FontFamily.Monospace,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Donation Portals
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
                    // QR Portal mode
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

                    // Draw a super high-fidelity, customized QR shape diagram on the canvas!
                    Box(
                        modifier = Modifier
                            .size(160.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White)
                            .padding(12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        androidx.compose.foundation.Canvas(modifier = Modifier.size(140.dp)) {
                            // Paint a simulated high fidelity QR block layout with a centered mosque dome indicator!
                            val dWidth = size.width
                            val step = dWidth / 8
                            // Corner markers
                            drawRect(color = Color.Black, topLeft = androidx.compose.ui.geometry.Offset(0f, 0f), size = androidx.compose.ui.geometry.Size(step * 2.5f, step * 2.5f))
                            drawRect(color = Color.White, topLeft = androidx.compose.ui.geometry.Offset(step * 0.4f, step * 0.4f), size = androidx.compose.ui.geometry.Size(step * 1.7f, step * 1.7f))
                            drawRect(color = Color.Black, topLeft = androidx.compose.ui.geometry.Offset(step * 0.7f, step * 0.7f), size = androidx.compose.ui.geometry.Size(step * 1.1f, step * 1.1f))

                            drawRect(color = Color.Black, topLeft = androidx.compose.ui.geometry.Offset(dWidth - step * 2.5f, 0f), size = androidx.compose.ui.geometry.Size(step * 2.5f, step * 2.5f))
                            drawRect(color = Color.White, topLeft = androidx.compose.ui.geometry.Offset(dWidth - step * 2.1f, step * 0.4f), size = androidx.compose.ui.geometry.Size(step * 1.7f, step * 1.7f))
                            drawRect(color = Color.Black, topLeft = androidx.compose.ui.geometry.Offset(dWidth - step * 1.8f, step * 0.7f), size = androidx.compose.ui.geometry.Size(step * 1.1f, step * 1.1f))

                            drawRect(color = Color.Black, topLeft = androidx.compose.ui.geometry.Offset(0f, dWidth - step * 2.5f), size = androidx.compose.ui.geometry.Size(step * 2.5f, step * 2.5f))
                            drawRect(color = Color.White, topLeft = androidx.compose.ui.geometry.Offset(step * 0.4f, dWidth - step * 2.1f), size = androidx.compose.ui.geometry.Size(step * 1.7f, step * 1.7f))
                            drawRect(color = Color.Black, topLeft = androidx.compose.ui.geometry.Offset(step * 0.7f, dWidth - step * 1.8f), size = androidx.compose.ui.geometry.Size(step * 1.1f, step * 1.1f))

                            // Inner random matrix particles representing dynamic UPI data
                            drawRect(color = Color.Black, topLeft = androidx.compose.ui.geometry.Offset(step * 4f, step * 1f), size = androidx.compose.ui.geometry.Size(step * 0.8f, step * 1.1f))
                            drawRect(color = Color.Black, topLeft = androidx.compose.ui.geometry.Offset(step * 3.5f, step * 4f), size = androidx.compose.ui.geometry.Size(step * 1.5f, step * 0.8f))
                            drawRect(color = Color.Black, topLeft = androidx.compose.ui.geometry.Offset(step * 1f, step * 4f), size = androidx.compose.ui.geometry.Size(step * 1.2f, step * 1.2f))
                            drawRect(color = Color.Black, topLeft = androidx.compose.ui.geometry.Offset(step * 5f, step * 4.5f), size = androidx.compose.ui.geometry.Size(step * 1f, step * 1.6f))
                            drawRect(color = Color.Black, topLeft = androidx.compose.ui.geometry.Offset(step * 4.5f, step * 2.5f), size = androidx.compose.ui.geometry.Size(step * 1.1f, step * 0.7f))

                            // Center Emerald Mosque Dot
                            drawCircle(
                                color = Color(0xFF0F766E),
                                radius = step * 0.8f,
                                center = androidx.compose.ui.geometry.Offset(dWidth / 2f, dWidth / 2f)
                            )
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
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
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
