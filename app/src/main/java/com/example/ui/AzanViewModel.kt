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
    fun setPlaybackVolume(volume: Float) {
        repository.updatePlaybackVolume(volume)
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

        // Calculate hours/minutes difference
        var diffMinutes = ChronoUnit.MINUTES.between(now, targetTime)
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
