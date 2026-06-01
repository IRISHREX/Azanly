package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.AzanDatabase
import com.example.data.local.DonationRecordEntity
import com.example.data.local.IqamahTimingEntity
import com.example.data.local.NoticeEntity
import com.example.data.repository.AzanRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import com.example.ui.theme.AppTheme
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

enum class AppRole {
    USER,
    MAULANA
}

class AzanViewModel(
    application: Application,
    private val repository: AzanRepository
) : AndroidViewModel(application) {

    // --- Dynamic Theme Selection state ---
    private val _currentTheme = MutableStateFlow(AppTheme.EMERALD_SPIRIT)
    val currentTheme: StateFlow<AppTheme> = _currentTheme.asStateFlow()

    fun changeTheme(theme: AppTheme) {
        _currentTheme.value = theme
        addSystemLog("Aesthetic active theme changed to: ${theme.displayName}")
    }

    // --- Spiritual Geo-Location and Time Tracker state ---
    private val _userLatitude = MutableStateFlow<Double>(30.0444) // Default Cairo
    val userLatitude: StateFlow<Double> = _userLatitude.asStateFlow()

    private val _userLongitude = MutableStateFlow<Double>(31.2357) // Default Cairo
    val userLongitude: StateFlow<Double> = _userLongitude.asStateFlow()

    private val _locationName = MutableStateFlow<String>("Cairo, Egypt")
    val locationName: StateFlow<String> = _locationName.asStateFlow()

    private val _locationMethod = MutableStateFlow<String>("Preset Default")
    val locationMethod: StateFlow<String> = _locationMethod.asStateFlow()

    private val _qiblaBearing = MutableStateFlow<Double>(135.25) // Accurate bearing for Cairo
    val qiblaBearing: StateFlow<Double> = _qiblaBearing.asStateFlow()

    private val _systemTimezone = MutableStateFlow<String>(java.util.TimeZone.getDefault().id)
    val systemTimezone: StateFlow<String> = _systemTimezone.asStateFlow()

    // Sets location and calculates accurate mathematical Qibla bearing using spherical trigonometry
    fun setCoordinates(lat: Double, lng: Double, name: String, method: String) {
        _userLatitude.value = lat
        _userLongitude.value = lng
        _locationName.value = name
        _locationMethod.value = method
        
        val bearing = calculateCorrectQibla(lat, lng)
        _qiblaBearing.value = bearing
        
        addSystemLog("Location: $name ($method) [${String.format(java.util.Locale.US, "%.4f", lat)}°N, ${String.format(java.util.Locale.US, "%.4f", lng)}°E]. Qibla bearing: ${String.format(java.util.Locale.US, "%.1f", bearing)}°")
    }

    private fun calculateCorrectQibla(lat: Double, lng: Double): Double {
        val userLatRad = Math.toRadians(lat)
        val userLonRad = Math.toRadians(lng)
        
        // Kaaba latitude and longitude
        val kaabaLatRad = Math.toRadians(21.422487)
        val kaabaLonRad = Math.toRadians(39.826206)
        
        val deltaLon = kaabaLonRad - userLonRad
        
        val y = Math.sin(deltaLon)
        val x = Math.cos(userLatRad) * Math.tan(kaabaLatRad) - Math.sin(userLatRad) * Math.cos(deltaLon)
        
        var bearing = Math.toDegrees(Math.atan2(y, x))
        bearing = (bearing + 360) % 360
        return bearing
    }

    fun syncSystemTimezone() {
        val detectedZone = java.util.TimeZone.getDefault().id
        _systemTimezone.value = detectedZone
        addSystemLog("Timezone auto-detected: $detectedZone (${java.util.TimeZone.getDefault().displayName})")
    }

    // --- Active App Role & Setting states ---
    private val _appRole = MutableStateFlow(AppRole.USER)
    val appRole: StateFlow<AppRole> = _appRole.asStateFlow()

    private val _isAdminAuthenticated = MutableStateFlow(false)
    val isAdminAuthenticated: StateFlow<Boolean> = _isAdminAuthenticated.asStateFlow()

    private val _overrideSilentMode = MutableStateFlow(true)
    val overrideSilentMode: StateFlow<Boolean> = _overrideSilentMode.asStateFlow()

    private val _muteDuringWorkHours = MutableStateFlow(false)
    val muteDuringWorkHours: StateFlow<Boolean> = _muteDuringWorkHours.asStateFlow()

    // --- State Streams ---
    val allNotices: StateFlow<List<NoticeEntity>> = repository.allNotices
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val allDonations: StateFlow<List<DonationRecordEntity>> = repository.allDonations
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val allIqamahTimings: StateFlow<List<IqamahTimingEntity>> = repository.allIqamahTimings
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // --- Live Audio Stream Info ---
    val isBroadcasting: StateFlow<Boolean> = repository.isBroadcasting
    val broadcastType: StateFlow<String?> = repository.broadcastType
    val broadcastTitle: StateFlow<String> = repository.broadcastTitle
    val streamLatencyMs: StateFlow<Int> = repository.streamLatencyMs
    val audioAmplitude: StateFlow<Float> = repository.audioAmplitude

    // --- Dynamic Alarm Manager Action Logs (Background Override Triggers) ---
    private val _systemAlertLogs = MutableStateFlow<List<String>>(emptyList())
    val systemAlertLogs: StateFlow<List<String>> = _systemAlertLogs.asStateFlow()

    // --- Setup Role ---
    fun selectRole(role: AppRole) {
        _appRole.value = role
        // If switching back to user, strip auth token
        if (role == AppRole.USER) {
            _isAdminAuthenticated.value = false
        }
    }

    fun authenticateMaulana(pinCode: String): Boolean {
        return if (pinCode == "786") {
            _isAdminAuthenticated.value = true
            true
        } else {
            false
        }
    }

    fun logoutMaulana() {
        _isAdminAuthenticated.value = false
    }

    // --- Live Broadcast Triggers (Admin) ---
    fun triggerLiveAzan(title: String) {
        repository.startAzanLive(title)
        addSystemLog("FCM high-priority wake-up payload triggered for title: '$title'")
    }

    fun triggerLiveAnnouncement(title: String) {
        repository.startMicLive(title)
        addSystemLog("FCM priority data message dispatched: Live Mic Announcement.")
    }

    fun stopLiveBroadcast() {
        repository.stopBroadcast()
        addSystemLog("Broadcast stream disconnected. Inactive state synchronized.")
    }

    // --- Settings and Logs ---
    val synthBaseFrequency: StateFlow<Double> = repository.synthBaseFrequency
    val calculationOffsetMinutes: StateFlow<Int> = repository.calculationOffsetMinutes
    val vibrateOnAlert: StateFlow<Boolean> = repository.vibrateOnAlert

    // --- Radio Frequency & P2P Repeating Mesh ---
    val radioTransmitting: StateFlow<Boolean> = repository.radioTransmitting
    val radioTransmittedFrequency: StateFlow<Double> = repository.radioTransmittedFrequency
    val userRadioReceiverEnabled: StateFlow<Boolean> = repository.userRadioReceiverEnabled
    val userRadioTunedFrequency: StateFlow<Double> = repository.userRadioTunedFrequency
    val isMeshRepeaterActive: StateFlow<Boolean> = repository.isMeshRepeaterActive
    val activeMeshNodes: StateFlow<Int> = repository.activeMeshNodes
    val meshTotalCoverageMeters: StateFlow<Int> = repository.meshTotalCoverageMeters

    // --- Noise Reduction & Squelch Controls ---
    val dspNoiseFilterActive: StateFlow<Boolean> = repository.dspNoiseFilterActive
    val squelchThresholdDb: StateFlow<Int> = repository.squelchThresholdDb

    // --- Walkie-Talkie P2P Intercom ---
    val isWalkieTalkieModeEnabled: StateFlow<Boolean> = repository.isWalkieTalkieModeEnabled
    val isPttPressed: StateFlow<Boolean> = repository.isPttPressed
    val walkieTalkieChannel: StateFlow<Int> = repository.walkieTalkieChannel

    fun toggleDspNoiseFilter(enabled: Boolean) {
        repository.toggleDspNoiseFilter(enabled)
        addSystemLog("DSP Noise attenuation / static filter " + (if (enabled) "activated" else "bypassed"))
    }

    fun updateSquelchThreshold(db: Int) {
        repository.updateSquelchThreshold(db)
        if (db == -80) {
            addSystemLog("Squelch gate fully opened (-80dB, raw static hiss allowed)")
        } else {
            addSystemLog("Squelch gate calibrated to: ${db} dB")
        }
    }

    fun toggleWalkieTalkieMode(enabled: Boolean) {
        repository.toggleWalkieTalkieMode(enabled)
        if (enabled) {
            addSystemLog("📻 Walkie-Talkie Mode initiated. Hold button to speak.")
        } else {
            addSystemLog("Walkie-Talkie Mode closed.")
        }
    }

    fun updateWalkieTalkieChannel(channel: Int) {
        repository.updateWalkieTalkieChannel(channel)
        addSystemLog("Walkie-Talkie sub-channel selected: Channel $channel")
    }

    fun startPttTransmission(channel: Int) {
        repository.toggleWalkieTalkieMode(true)
        repository.updateWalkieTalkieChannel(channel)
        repository.startPttTransmission(channel)
        addSystemLog("🎙️ [Walkie-Talkie] TX started on Sub-Channel $channel. *Chirp-in static beep-on*")
    }

    fun stopPttTransmission() {
        repository.stopPttTransmission()
        addSystemLog("🔇 [Walkie-Talkie] TX completed. *Roger beep, back to RX listening*")
    }

    fun toggleRadioTransmitting(enabled: Boolean) {
        repository.toggleRadioTransmitting(enabled)
        addSystemLog("Mosque FM RF Transmitter " + (if (enabled) "enabled" else "disabled"))
    }

    fun updateRadioTransmittedFrequency(freq: Double) {
        repository.updateRadioTransmittedFrequency(freq)
        addSystemLog("Mosque FM frequency calibrated to: ${freq} MHz")
    }

    fun toggleUserRadioReceiver(enabled: Boolean) {
        repository.toggleUserRadioReceiver(enabled)
        addSystemLog("Mobile FM Antenna Receiver " + (if (enabled) "activated" else "deactivated"))
    }

    fun updateUserRadioTunedFrequency(freq: Double) {
        repository.updateUserRadioTunedFrequency(freq)
    }

    fun toggleMeshRepeater(enabled: Boolean) {
        repository.toggleMeshRepeater(enabled)
        if (enabled) {
            addSystemLog("🚀 Mesh signal relay repeater activated! Rebroadcasting subnetwork frequency.")
        } else {
            addSystemLog("Mesh signal relay repeater deactivated.")
        }
    }

    fun setPlaybackVolume(volume: Float) {
        repository.updatePlaybackVolume(volume)
    }

    fun setSynthBaseFrequency(freq: Double) {
        repository.updateSynthBaseFrequency(freq)
        val hzLabel = when (freq) {
            261.63 -> "Mellow (C4 261Hz)"
            330.0 -> "Peaceful Chime (E4 330Hz)"
            392.0 -> "Meditative (G4 392Hz)"
            440.0 -> "Bright Acoustic (A4 440Hz)"
            else -> "${freq}Hz"
        }
        addSystemLog("Tone profile alternative updated: $hzLabel")
    }

    fun setCalculationOffsetMinutes(offset: Int) {
        repository.updateCalculationOffsetMinutes(offset)
        addSystemLog("Juristic countdown offset modified to: ${if (offset >= 0) "+$offset" else offset} minutes")
    }

    fun setVibrateOnAlert(enabled: Boolean) {
        repository.updateVibrateOnAlert(enabled)
        addSystemLog("Vibe haptics alert setting updated to: $enabled")
    }

    fun setOverrideSilent(value: Boolean) {
        _overrideSilentMode.value = value
        addSystemLog("Override Silent Mode configuration changed to: $value")
    }

    fun setMuteDuringWork(value: Boolean) {
        _muteDuringWorkHours.value = value
        addSystemLog("Workplace mute override: $value")
    }

    fun addSystemLog(logMsg: String) {
        val timeNow = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"))
        _systemAlertLogs.value = listOf("[$timeNow] $logMsg") + _systemAlertLogs.value.take(4)
    }

    // --- Database Management Ops ---
    fun addNotice(title: String, category: String, content: String, isPinned: Boolean) {
        viewModelScope.launch {
            repository.publishNotice(title, category, content, isPinned)
            addSystemLog("Notice added locally and synced with cloud index: '$title'")
        }
    }

    fun deleteNotice(notice: NoticeEntity) {
        viewModelScope.launch {
            repository.removeNotice(notice)
            addSystemLog("Notice removed index: '${notice.title}'")
        }
    }

    fun updateIqamahTimes(prayer: String, adhan: String, iqamah: String, offset: Int) {
        viewModelScope.launch {
            repository.updateIqamah(prayer, adhan, iqamah, offset)
            addSystemLog("$prayer Iqamah timing set to $iqamah")
        }
    }

    fun processDonation(name: String, amount: Double, note: String?) {
        viewModelScope.launch {
            repository.submitDonation(name, amount, note)
            addSystemLog("UPI Donation of $$amount verified from '$name'")
        }
    }

    // --- Live Prayer Countdown Calculation Utility ---
    fun calculateNextPrayerCountdown(timings: List<IqamahTimingEntity>): PrayerCountdown? {
        if (timings.isEmpty()) return null
        val now = LocalTime.now()

        // Define flexible formatters for 12 hours AM/PM text (both double and single-digit hours)
        val formatters = listOf(
            DateTimeFormatter.ofPattern("hh:mm a", java.util.Locale.US),
            DateTimeFormatter.ofPattern("h:mm a", java.util.Locale.US),
            DateTimeFormatter.ofPattern("hh:mma", java.util.Locale.US),
            DateTimeFormatter.ofPattern("h:mma", java.util.Locale.US)
        )

        val prayerTimeMap = timings.mapNotNull { entity ->
            val cleanTime = entity.iqamahTime.trim().uppercase()
            var parsedTime: LocalTime? = null
            for (formatter in formatters) {
                try {
                    parsedTime = LocalTime.parse(cleanTime, formatter)
                    break
                } catch (e: Exception) {
                    // try next formatter
                }
            }
            if (parsedTime != null) {
                entity to parsedTime
            } else {
                null
            }
        }.sortedBy { it.second }

        if (prayerTimeMap.isEmpty()) return null

        // Find first prayer that occurs after current time
        val nextPrayer = prayerTimeMap.firstOrNull { it.second.isAfter(now) }
            ?: prayerTimeMap.first() // If none after, it's Fajr of next day

        val targetTime = nextPrayer.second
        val entity = nextPrayer.first

        // Calculate hours/minutes difference with safety/juristic offset
        val offsetMinsVal = calculationOffsetMinutes.value.toLong()
        var diffMinutes = ChronoUnit.MINUTES.between(now, targetTime) + offsetMinsVal
        if (diffMinutes < 0) {
            // Target is next day (e.g. past Isha, counting down to tomorrow's Fajr)
            diffMinutes += 24 * 60
        }

        val hours = diffMinutes / 60
        val mins = diffMinutes % 60

        return PrayerCountdown(
            prayerName = entity.prayerName,
            iqamahTime = entity.iqamahTime,
            hours = hours,
            minutes = mins,
            totalMinutesLeft = diffMinutes
        )
    }
}

// Data class to easily represent count down timers
data class PrayerCountdown(
    val prayerName: String,
    val iqamahTime: String,
    val hours: Long,
    val minutes: Long,
    val totalMinutesLeft: Long
)

// Factory Provider
class ViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AzanViewModel::class.java)) {
            val database = AzanDatabase.getDatabase(application)
            val repository = AzanRepository(database.azanDao())
            @Suppress("UNCHECKED_CAST")
            return AzanViewModel(application, repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
