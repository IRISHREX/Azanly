package com.example.data.repository

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.util.Log
import com.example.data.local.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlin.math.sin

class AzanRepository(private val azanDao: AzanDao) {

    // --- Core Database Portals ---
    val allNotices: Flow<List<NoticeEntity>> = azanDao.getAllNotices()
    val allDonations: Flow<List<DonationRecordEntity>> = azanDao.getAllDonations()
    val allIqamahTimings: Flow<List<IqamahTimingEntity>> = azanDao.getAllIqamahTimings()

    // --- Global Realtime PubSub State (Simulating Server-to-Client WebSockets/FCM) ---
    private val _isBroadcasting = MutableStateFlow(false)
    val isBroadcasting: StateFlow<Boolean> = _isBroadcasting.asStateFlow()

    private val _broadcastType = MutableStateFlow<String?>(null) // "azan" or "mic" or null
    val broadcastType: StateFlow<String?> = _broadcastType.asStateFlow()

    private val _broadcastTitle = MutableStateFlow("")
    val broadcastTitle: StateFlow<String> = _broadcastTitle.asStateFlow()

    private val _streamLatencyMs = MutableStateFlow(0)
    val streamLatencyMs: StateFlow<Int> = _streamLatencyMs.asStateFlow()

    private val _audioAmplitude = MutableStateFlow(0f)
    val audioAmplitude: StateFlow<Float> = _audioAmplitude.asStateFlow()

    private val _playbackVolume = MutableStateFlow(0.8f)
    val playbackVolume: StateFlow<Float> = _playbackVolume.asStateFlow()

    fun updatePlaybackVolume(volume: Float) {
        _playbackVolume.value = volume.coerceIn(0f, 1f)
    }

    // Sound Synthesizer control
    private var synthesizerJob: Job? = null
    private var amplitudeAnimatorJob: Job? = null
    private val repositoryScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var audioTrack: AudioTrack? = null

    init {
        // Automatically pre-populate default Iqamah timings on first startup if empty
        repositoryScope.launch {
            allIqamahTimings.first().let { timings ->
                if (timings.isEmpty()) {
                    val defaults = listOf(
                        IqamahTimingEntity("Fajr", "04:30 AM", "05:00 AM", 30),
                        IqamahTimingEntity("Dhuhr", "12:50 PM", "01:15 PM", 25),
                        IqamahTimingEntity("Asr", "04:45 PM", "05:05 PM", 20),
                        IqamahTimingEntity("Maghrib", "07:02 PM", "07:15 PM", 13),
                        IqamahTimingEntity("Isha", "08:35 PM", "08:50 PM", 15)
                    )
                    azanDao.insertIqamahTimings(defaults)
                }
            }
        }
    }

    // --- Maulana Actions ---

    fun startAzanLive(title: String) {
        _broadcastTitle.value = title
        _broadcastType.value = "azan"
        _isBroadcasting.value = true
        _streamLatencyMs.value = 145 // Simulated ultra low raw latency (WebRTC / AAC-LD)
        startSimulatingAudioStream(isAzan = true)
    }

    fun startMicLive(title: String) {
        _broadcastTitle.value = title
        _broadcastType.value = "mic"
        _isBroadcasting.value = true
        _streamLatencyMs.value = 210 // Low-delay RTP live audio
        startSimulatingAudioStream(isAzan = false)
    }

    fun stopBroadcast() {
        _isBroadcasting.value = false
        _broadcastType.value = null
        _broadcastTitle.value = ""
        _streamLatencyMs.value = 0
        _audioAmplitude.value = 0f
        stopSimulatingAudioStream()
    }

    // --- Notice Actions ---

    suspend fun publishNotice(title: String, category: String, content: String, isPinned: Boolean = false) {
        val notice = NoticeEntity(
            title = title,
            category = category,
            content = content,
            timestamp = System.currentTimeMillis(),
            isPinned = isPinned
        )
        azanDao.insertNotice(notice)
    }

    suspend fun removeNotice(notice: NoticeEntity) {
        azanDao.deleteNotice(notice)
    }

    suspend fun removeNoticeById(id: Long) {
        azanDao.deleteNoticeById(id)
    }

    // --- Iqamah Actions ---

    suspend fun updateIqamah(prayerName: String, adhanTime: String, iqamahTime: String, offset: Int) {
        val timing = IqamahTimingEntity(prayerName, adhanTime, iqamahTime, offset)
        azanDao.insertIqamahTiming(timing)
    }

    // --- Donation Actions ---

    suspend fun submitDonation(contributor: String, amount: Double, msg: String?) {
        val donation = DonationRecordEntity(
            contributorName = contributor,
            amount = amount,
            timestamp = System.currentTimeMillis(),
            message = msg
        )
        azanDao.insertDonation(donation)
    }

    // --- Sound Synthesis Simulation & Live Wave Energy Animators ---

    private fun startSimulatingAudioStream(isAzan: Boolean) {
        // Animation of live sound wave amplitudes
        amplitudeAnimatorJob?.cancel()
        amplitudeAnimatorJob = repositoryScope.launch {
            var tick = 0.0
            while (isActive) {
                tick += 0.2
                // Create highly organic fluctuations for sound waveform matching voice peaks
                val base = if (isAzan) {
                    // Azan has longer drawn out swells
                    0.5f + (sin(tick).toFloat() * 0.3f) + (sin(tick * 0.4).toFloat() * 0.15f)
                } else {
                    // Speech/Mic has sharper sudden peaks and silences on pauses
                    val speaking = sin(tick * 1.5) * sin(tick * 0.3)
                    if (speaking > 0) speaking.toFloat() * 0.7f else 0.05f
                }
                _audioAmplitude.value = base.coerceIn(0.01f, 1.0f)
                delay(80)
            }
        }

        // Play Synthesized Pure Ambient Chimes (Melodious periodic tones representing Azan)
        synthesizerJob?.cancel()
        synthesizerJob = repositoryScope.launch(Dispatchers.IO) {
            try {
                val sampleRate = 44100
                val minBufferSize = AudioTrack.getMinBufferSize(
                    sampleRate,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT
                )
                
                audioTrack = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(sampleRate)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(minBufferSize)
                    .setTransferMode(AudioTrack.MODE_STREAM)
                    .build()

                audioTrack?.play()

                val bufferSize = 256
                val samples = ShortArray(bufferSize)
                var phase = 0.0
                val primaryFreq = if (isAzan) 330.0 else 440.0 // Primary frequency (E4 or A4 chime)
                
                while (isActive) {
                    val currentAmplitude = _audioAmplitude.value
                    val volumeLevel = _playbackVolume.value
                    for (i in 0 until bufferSize) {
                        // Generate a rich, peaceful dual-tone bell/wind chime effect
                        val angle1 = 2.0 * java.lang.Math.PI * primaryFreq / sampleRate
                        val angle2 = 2.0 * java.lang.Math.PI * (primaryFreq * 1.5) / sampleRate // Perfect fifth harmonic
                        
                        val sample1 = sin(phase * angle1)
                        val sample2 = sin(phase * angle2) * 0.4 // Blend fifth
                        
                        val compositeSample = (sample1 + sample2) / 1.4
                        
                        samples[i] = (compositeSample * 32767.0 * currentAmplitude * 0.3 * volumeLevel).toInt().toShort()
                        phase += 1.0
                    }
                    audioTrack?.write(samples, 0, bufferSize)
                    yield()
                }
            } catch (e: Exception) {
                Log.e("AudioTrack", "Synthesizer error: ${e.message}")
            }
        }
    }

    private fun stopSimulatingAudioStream() {
        amplitudeAnimatorJob?.cancel()
        synthesizerJob?.cancel()
        repositoryScope.launch(Dispatchers.IO) {
            try {
                audioTrack?.stop()
                audioTrack?.release()
                audioTrack = null
            } catch (e: Exception) {
                // Ignore safe shutdown errors
            }
        }
    }
}
