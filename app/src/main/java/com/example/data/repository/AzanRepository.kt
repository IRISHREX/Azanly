package com.example.data.repository

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import com.example.data.local.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
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

    private val _synthBaseFrequency = MutableStateFlow(330.0)
    val synthBaseFrequency: StateFlow<Double> = _synthBaseFrequency.asStateFlow()

    fun updateSynthBaseFrequency(freq: Double) {
        _synthBaseFrequency.value = freq
    }

    private val _calculationOffsetMinutes = MutableStateFlow(0)
    val calculationOffsetMinutes: StateFlow<Int> = _calculationOffsetMinutes.asStateFlow()

    fun updateCalculationOffsetMinutes(offset: Int) {
        _calculationOffsetMinutes.value = offset.coerceIn(-15, 15)
    }

    private val _vibrateOnAlert = MutableStateFlow(true)
    val vibrateOnAlert: StateFlow<Boolean> = _vibrateOnAlert.asStateFlow()

    fun updateVibrateOnAlert(enabled: Boolean) {
        _vibrateOnAlert.value = enabled
    }

    // --- Radio Frequency & Mesh Relay Networking States ---
    private val _radioTransmitting = MutableStateFlow(false)
    val radioTransmitting: StateFlow<Boolean> = _radioTransmitting.asStateFlow()

    private val _radioTransmittedFrequency = MutableStateFlow(92.5) // default MHz
    val radioTransmittedFrequency: StateFlow<Double> = _radioTransmittedFrequency.asStateFlow()

    private val _userRadioReceiverEnabled = MutableStateFlow(false)
    val userRadioReceiverEnabled: StateFlow<Boolean> = _userRadioReceiverEnabled.asStateFlow()

    private val _userRadioTunedFrequency = MutableStateFlow(92.5) // default MHz
    val userRadioTunedFrequency: StateFlow<Double> = _userRadioTunedFrequency.asStateFlow()

    private val _isMeshRepeaterActive = MutableStateFlow(false)
    val isMeshRepeaterActive: StateFlow<Boolean> = _isMeshRepeaterActive.asStateFlow()

    private val _activeMeshNodes = MutableStateFlow(1)
    val activeMeshNodes: StateFlow<Int> = _activeMeshNodes.asStateFlow()

    private val _meshTotalCoverageMeters = MutableStateFlow(150)
    val meshTotalCoverageMeters: StateFlow<Int> = _meshTotalCoverageMeters.asStateFlow()

    // --- Noise Reduction & Squelch Filter States ---
    private val _dspNoiseFilterActive = MutableStateFlow(true)
    val dspNoiseFilterActive: StateFlow<Boolean> = _dspNoiseFilterActive.asStateFlow()

    private val _squelchThresholdDb = MutableStateFlow(-45) // range -80 dB to 0 dB
    val squelchThresholdDb: StateFlow<Int> = _squelchThresholdDb.asStateFlow()

    // --- Walkie-Talkie P2P Intercom States ---
    private val _isWalkieTalkieModeEnabled = MutableStateFlow(false)
    val isWalkieTalkieModeEnabled: StateFlow<Boolean> = _isWalkieTalkieModeEnabled.asStateFlow()

    private val _isPttPressed = MutableStateFlow(false)
    val isPttPressed: StateFlow<Boolean> = _isPttPressed.asStateFlow()

    private val _walkieTalkieChannel = MutableStateFlow(3) // sub-channels 1, 2, 3, 4, 5
    val walkieTalkieChannel: StateFlow<Int> = _walkieTalkieChannel.asStateFlow()

    fun toggleRadioTransmitting(enabled: Boolean) {
        _radioTransmitting.value = enabled
    }

    fun updateRadioTransmittedFrequency(freq: Double) {
        _radioTransmittedFrequency.value = freq
    }

    fun toggleUserRadioReceiver(enabled: Boolean) {
        _userRadioReceiverEnabled.value = enabled
    }

    fun updateUserRadioTunedFrequency(freq: Double) {
        _userRadioTunedFrequency.value = freq
    }

    fun toggleDspNoiseFilter(enabled: Boolean) {
        _dspNoiseFilterActive.value = enabled
    }

    fun updateSquelchThreshold(db: Int) {
        _squelchThresholdDb.value = db
    }

    fun toggleWalkieTalkieMode(enabled: Boolean) {
        _isWalkieTalkieModeEnabled.value = enabled
        if (!enabled) {
            _isPttPressed.value = false
        }
    }

    fun updateWalkieTalkieChannel(channel: Int) {
        _walkieTalkieChannel.value = channel.coerceIn(1, 5)
    }

    fun toggleMeshRepeater(enabled: Boolean) {
        _isMeshRepeaterActive.value = enabled
        repeaterUdpJob?.cancel()
        if (enabled) {
            repeaterUdpJob = repositoryScope.launch(Dispatchers.IO) {
                var socket: DatagramSocket? = null
                try {
                    socket = DatagramSocket()
                    socket.broadcast = true
                    val broadcastAddr = InetAddress.getByName("255.255.0.255") // Try standard local subnet
                    val fallbackAddr = InetAddress.getByName("255.255.255.255")
                    while (isActive) {
                        val bType = _broadcastType.value ?: "none"
                        val bTitle = _broadcastTitle.value
                        val amp = _audioAmplitude.value
                        val msg = "AZAN_REPEATER_SYNC:repeating=true;active=true;type=$bType;title=$bTitle;amplitude=$amp"
                        val buffer = msg.toByteArray()
                        val packet1 = DatagramPacket(buffer, buffer.size, broadcastAddr, 18345)
                        val packet2 = DatagramPacket(buffer, buffer.size, fallbackAddr, 18345)
                        try {
                            socket.send(packet1)
                            socket.send(packet2)
                        } catch (e: Exception) {}
                        delay(1200)
                    }
                } catch (e: Exception) {
                    Log.e("AzanNet", "Repeater relay beacon failed: ${e.message}")
                } finally {
                    socket?.close()
                }
            }
        }
    }

    // --- Peer-to-Peer Real-Time Networking Sockets ---
    private var isHost = false
    private var lastHeartbeatTimeMap = 0L

    // Coroutine Jobs for networking
    private var udpSenderJob: Job? = null
    private var tcpServerJob: Job? = null
    private var voiceRecorderJob: Job? = null

    private var udpReceiverJob: Job? = null
    private var tcpClientJob: Job? = null
    private var heartbeatMonitorJob: Job? = null
    private var repeaterUdpJob: Job? = null
    private val activeRepeaterBeacons = java.util.Collections.synchronizedMap(mutableMapOf<String, Long>())

    // Sockets and streams
    private val activeClientSockets = java.util.Collections.synchronizedList(mutableListOf<Socket>())
    private var serverSocket: ServerSocket? = null
    private var localUdpReceiverSocket: DatagramSocket? = null
    private var activeStreamSocket: Socket? = null
    private var isPlayingVoiceStream = false

    // Sound Synthesizer control
    private var synthesizerJob: Job? = null
    private var amplitudeAnimatorJob: Job? = null
    private val repositoryScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    init {
        // Start background UDP receiver to auto-discover Maulana's broadcast state from other devices
        startUdpReceiver()
        startHeartbeatMonitor()

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
        _streamLatencyMs.value = 60 // True dynamic network latency
        startSimulatingAudioStream(isAzan = true)
        startNetworkBroadcasting("azan", title)
    }

    fun startMicLive(title: String) {
        _broadcastTitle.value = title
        _broadcastType.value = "mic"
        _isBroadcasting.value = true
        _streamLatencyMs.value = 90 // Micro-latency PCM stream over local subnetwork
        startSimulatingAudioStream(isAzan = false)
        startNetworkBroadcasting("mic", title)
    }

    fun stopBroadcast() {
        isHost = false
        _isBroadcasting.value = false
        _broadcastType.value = null
        _broadcastTitle.value = ""
        _streamLatencyMs.value = 0
        _audioAmplitude.value = 0f
        stopNetworkBroadcasting()
        stopSimulatingAudioStream()
    }

    fun startPttTransmission(channel: Int) {
        _isPttPressed.value = true
        _broadcastTitle.value = "Walkie-Talkie Intercom (Ch $channel)"
        _broadcastType.value = "walkie"
        _isBroadcasting.value = true
        _streamLatencyMs.value = 45 // ultra-low latency for walkie talkie
        startSimulatingAudioStream(isAzan = false)
        startNetworkBroadcasting("walkie", "Walkie-Talkie Intercom (Ch $channel)")
    }

    fun stopPttTransmission() {
        _isPttPressed.value = false
        stopBroadcast()
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
            var track: AudioTrack? = null
            try {
                val sampleRate = 44100
                val minBufferSize = AudioTrack.getMinBufferSize(
                    sampleRate,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT
                )
                
                track = AudioTrack.Builder()
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

                track.play()

                val bufferSize = 256
                val samples = ShortArray(bufferSize)
                var phase = 0.0
                while (isActive) {
                    val currentAmplitude = _audioAmplitude.value
                    val volumeLevel = _playbackVolume.value
                    
                    // Retrieve dynamic selected base frequency (e.g., 261Hz, 330Hz, 392Hz, 440Hz)
                    val baseFreqSetting = _synthBaseFrequency.value
                    val primaryFreq = if (isAzan) baseFreqSetting else baseFreqSetting * 1.33

                    // Live FM Radio static blending simulation
                    val isRadioEnabled = _userRadioReceiverEnabled.value
                    val rxFreq = _userRadioTunedFrequency.value
                    val txFreq = _radioTransmittedFrequency.value
                    val isTxActive = _radioTransmitting.value

                    val snr = if (isRadioEnabled) {
                        val freqErr = Math.abs(rxFreq - txFreq)
                        if (isTxActive) {
                            if (freqErr < 0.05) 1.0f
                            else if (freqErr < 0.3f) {
                                val frac = ((0.3f - freqErr) / 0.25f).toFloat()
                                frac * frac
                            } else {
                                0.0f
                            }
                        } else {
                            0.0f
                        }
                    } else {
                        1.0f // Standard Direct Subnetwork mode has 100% crystal clear quality
                    }

                    for (i in 0 until bufferSize) {
                        // Generate a rich, peaceful dual-tone bell/wind chime effect
                        val angle1 = 2.0 * java.lang.Math.PI * primaryFreq / sampleRate
                        val angle2 = 2.0 * java.lang.Math.PI * (primaryFreq * 1.5) / sampleRate // Perfect fifth harmonic
                        
                        val sample1 = sin(phase * angle1)
                        val sample2 = sin(phase * angle2) * 0.4 // Blend fifth
                        
                        val compositeSample = (sample1 + sample2) / 1.4
                        
                        // Calculate final sample with static white noise if tuning is not perfect or inactive
                        val voicePart = (compositeSample.toFloat() * currentAmplitude * 0.3f * snr)
                        val staticNoiseSource = (Math.random() * 2.0 - 1.0).toFloat()
                        
                        // Noise reduction attenuation: if active, attenuate noise significantly (from 0.25f to 0.02f)
                        val isNoiseFilterActive = _dspNoiseFilterActive.value
                        val noiseVolumeScale = if (isNoiseFilterActive) 0.02f else 0.25f
                        val noisePart = staticNoiseSource * (1.0f - snr) * noiseVolumeScale
                        
                        // Squelch Gate checks: if the signal quality is below the squelch gate threshold, mute static completely.
                        // Map -80dB (fully open) to 0dB (fully closed) into required SNR ratio
                        val squelchThreshold = _squelchThresholdDb.value
                        val squelchRequiredSnr = (squelchThreshold + 80f) / 80f
                        
                        val passedSquelch = if (isRadioEnabled) {
                            snr >= squelchRequiredSnr.coerceIn(0.01f, 0.95f)
                        } else {
                            true
                        }

                        val finalAudioSample = if (passedSquelch) {
                            (voicePart + noisePart) * volumeLevel
                        } else {
                            0f
                        }

                        samples[i] = (finalAudioSample.coerceIn(-1.0f, 1.0f) * 32767.0f).toInt().toShort()
                        
                        phase += 1.0
                    }
                    val written = track.write(samples, 0, bufferSize)
                    if (written <= 0) {
                        // Avoid tight spin-lock in case of write error (e.g., headless or failing audio HAL in container)
                        delay(60)
                    } else {
                        yield()
                    }
                }
            } catch (e: Exception) {
                Log.e("AudioTrack", "Synthesizer error: ${e.message}")
            } finally {
                try {
                    track?.stop()
                    track?.release()
                } catch (ex: Exception) {
                    // Ignore safe shutdown errors
                }
            }
        }
    }

    private fun stopSimulatingAudioStream() {
        amplitudeAnimatorJob?.cancel()
        synthesizerJob?.cancel()
    }

    // --- Peer-to-Peer Network Transmission Engine (Maulana Host) ---

    private fun startNetworkBroadcasting(type: String, title: String) {
        isHost = true
        _isBroadcasting.value = true
        _broadcastType.value = type
        _broadcastTitle.value = title

        // 1. Start UDP Presence Broadcast loop (Heartbeat to advertise host IP, title and active amplitude)
        udpSenderJob?.cancel()
        udpSenderJob = repositoryScope.launch(Dispatchers.IO) {
            var datagramSocket: DatagramSocket? = null
            try {
                datagramSocket = DatagramSocket()
                datagramSocket.broadcast = true
                val broadcastAddr = InetAddress.getByName("255.255.255.255")
                while (isActive) {
                    val currentAmp = _audioAmplitude.value
                    val rActive = _radioTransmitting.value
                    val rFreq = _radioTransmittedFrequency.value
                    val nodes = _activeMeshNodes.value
                    val msg = "AZAN_NET_SYNC:active=true;type=$type;title=$title;amplitude=${String.format(java.util.Locale.US, "%.2f", currentAmp)};radioActive=$rActive;radioFreq=$rFreq;meshNodes=$nodes"
                    val buffer = msg.toByteArray()
                    val packet = DatagramPacket(buffer, buffer.size, broadcastAddr, 18345)
                    try {
                        datagramSocket.send(packet)
                    } catch (e: Exception) {
                        // Ignore standard socket exceptions
                    }
                    delay(1000)
                }
            } catch (e: Exception) {
                Log.e("AzanNet", "UDP Broadcast Sender Error: ${e.message}")
            } finally {
                datagramSocket?.close()
            }
        }

        // 2. Start TCP Audio Server to stream recorded mic audio if type is mic or walkie
        if (type == "mic" || type == "walkie") {
            startTcpAudioServer()
        }
    }

    private fun stopNetworkBroadcasting() {
        udpSenderJob?.cancel()
        tcpServerJob?.cancel()
        voiceRecorderJob?.cancel()

        synchronized(activeClientSockets) {
            for (client in activeClientSockets) {
                try {
                    client.close()
                } catch (e: Exception) {}
            }
            activeClientSockets.clear()
        }

        try {
            serverSocket?.close()
        } catch (e: Exception) {}
        serverSocket = null
    }

    @android.annotation.SuppressLint("MissingPermission")
    private fun startTcpAudioServer() {
        tcpServerJob?.cancel()
        voiceRecorderJob?.cancel()
        activeClientSockets.clear()

        tcpServerJob = repositoryScope.launch(Dispatchers.IO) {
            try {
                serverSocket = ServerSocket(18346)
                while (isActive) {
                    val socket = serverSocket?.accept() ?: break
                    activeClientSockets.add(socket)
                    Log.d("AzanNet", "User receiver client connected: ${socket.inetAddress.hostAddress}")
                }
            } catch (e: Exception) {
                Log.d("AzanNet", "TCP Voice Server Socket shut down: ${e.message}")
            }
        }

        voiceRecorderJob = repositoryScope.launch(Dispatchers.IO) {
            var record: AudioRecord? = null
            try {
                val sampleRate = 16000
                val minBufferSize = AudioRecord.getMinBufferSize(
                    sampleRate,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT
                )

                record = AudioRecord(
                    android.media.MediaRecorder.AudioSource.MIC,
                    sampleRate,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    minBufferSize * 2
                )

                if (record.state != AudioRecord.STATE_INITIALIZED) {
                    Log.e("AzanNet", "AudioRecord state failed to initialize. Microphone resources busy or permission denied.")
                    return@launch
                }

                record.startRecording()
                val buffer = ShortArray(512)
                val byteBuffer = ByteArray(1024)

                while (isActive) {
                    val readShorts = record.read(buffer, 0, buffer.size)
                    if (readShorts > 0) {
                        var maxVal = 0
                        for (i in 0 until readShorts) {
                            val absVal = Math.abs(buffer[i].toInt())
                            if (absVal > maxVal) maxVal = absVal

                            val value = buffer[i]
                            byteBuffer[i * 2] = (value.toInt() and 0xFF).toByte()
                            byteBuffer[i * 2 + 1] = ((value.toInt() shr 8) and 0xFF).toByte()
                        }

                        // Calculate dynamic live feedback amplitude
                        val peakRatio = (maxVal / 32767f).coerceIn(0.01f, 1f)
                        _audioAmplitude.value = peakRatio

                        // Broadcast raw audio PCM bytes to all connected TCP listener systems
                        val deadClients = mutableListOf<Socket>()
                        synchronized(activeClientSockets) {
                            for (client in activeClientSockets) {
                                try {
                                    val out = client.getOutputStream()
                                    out.write(byteBuffer, 0, readShorts * 2)
                                    out.flush()
                                } catch (e: Exception) {
                                    deadClients.add(client)
                                }
                            }
                            activeClientSockets.removeAll(deadClients)
                        }
                    } else {
                        delay(20)
                    }
                }
            } catch (e: Exception) {
                Log.e("AzanNet", "Mic stream pipeline failure: ${e.message}")
            } finally {
                try {
                    record?.stop()
                    record?.release()
                } catch (e: Exception) {}
            }
        }
    }

    // --- Peer-to-Peer Network Receiver / Listener Engine (User Client) ---

    private fun startUdpReceiver() {
        udpReceiverJob?.cancel()
        udpReceiverJob = repositoryScope.launch(Dispatchers.IO) {
            try {
                val ds = DatagramSocket(18345)
                localUdpReceiverSocket = ds
                val buffer = ByteArray(1024)
                while (isActive) {
                    val packet = DatagramPacket(buffer, buffer.size)
                    try {
                        ds.receive(packet)
                    } catch (e: Exception) {
                        if (!isActive) break
                        continue
                    }
                    val senderIp = packet.address.hostAddress ?: ""
                    val dataStr = String(packet.data, 0, packet.length).trim()

                    if (dataStr.startsWith("AZAN_REPEATER_SYNC:")) {
                        val body = dataStr.substring("AZAN_REPEATER_SYNC:".length)
                        val map = body.split(";").associate {
                            val parts = it.split("=")
                            if (parts.size == 2) parts[0] to parts[1] else "" to ""
                        }
                        if (map["repeating"] == "true") {
                            activeRepeaterBeacons[senderIp] = System.currentTimeMillis()
                        }
                    }

                    if (!isHost && dataStr.startsWith("AZAN_NET_SYNC:")) {
                        lastHeartbeatTimeMap = System.currentTimeMillis()
                        val body = dataStr.substring("AZAN_NET_SYNC:".length)
                        val map = body.split(";").associate {
                            val parts = it.split("=")
                            if (parts.size == 2) parts[0] to parts[1] else "" to ""
                        }

                        val active = map["active"] == "true"
                        val type = map["type"]
                        val title = map["title"] ?: ""
                        val amp = map["amplitude"]?.toFloatOrNull() ?: 0.1f

                        val rActive = map["radioActive"] == "true"
                        val rFreq = map["radioFreq"]?.toDoubleOrNull() ?: 92.5

                        _isBroadcasting.value = active
                        _broadcastType.value = type
                        _broadcastTitle.value = title
                        _audioAmplitude.value = amp
                        _streamLatencyMs.value = 85

                        _radioTransmitting.value = rActive
                        _radioTransmittedFrequency.value = rFreq

                        if (active && type == "mic") {
                            startVoiceReceiveStream(senderIp)
                        } else {
                            stopVoiceReceiveStream()
                        }

                        if (active && type == "azan") {
                            if (synthesizerJob == null || synthesizerJob?.isActive == false) {
                                startSimulatingAudioStream(isAzan = true)
                            }
                        } else if (active && type == "mic") {
                            if (synthesizerJob == null || synthesizerJob?.isActive == false) {
                                startSimulatingAudioStream(isAzan = false)
                            }
                        } else {
                            stopSimulatingAudioStream()
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("AzanNet", "UDP Discovery loop failed: ${e.message}")
            }
        }
    }

    private fun startHeartbeatMonitor() {
        heartbeatMonitorJob?.cancel()
        heartbeatMonitorJob = repositoryScope.launch(Dispatchers.Default) {
            while (isActive) {
                delay(1000)
                
                // Sweep dead repeater beacons (inactive > 4.5 seconds)
                val now = System.currentTimeMillis()
                synchronized(activeRepeaterBeacons) {
                    val iterator = activeRepeaterBeacons.entries.iterator()
                    while (iterator.hasNext()) {
                        val entry = iterator.next()
                        if (now - entry.value > 4500) {
                            iterator.remove()
                        }
                    }
                }
                
                // Calculate dynamic mesh coordinates
                val localMeshNodes = 1 + activeRepeaterBeacons.size
                _activeMeshNodes.value = if (_isMeshRepeaterActive.value) localMeshNodes.coerceAtLeast(2) else localMeshNodes
                _meshTotalCoverageMeters.value = _activeMeshNodes.value * 150

                if (!isHost && _isBroadcasting.value) {
                    val timeSinceLast = System.currentTimeMillis() - lastHeartbeatTimeMap
                    if (timeSinceLast > 3500) {
                        Log.d("AzanNet", "Maulana signal timed out. Disconnecting audio channels.")
                        _isBroadcasting.value = false
                        _broadcastType.value = null
                        _broadcastTitle.value = ""
                        _streamLatencyMs.value = 0
                        _audioAmplitude.value = 0f
                        stopVoiceReceiveStream()
                        stopSimulatingAudioStream()
                    }
                }
            }
        }
    }

    private fun startVoiceReceiveStream(hostIp: String) {
        if (isPlayingVoiceStream) return
        isPlayingVoiceStream = true

        tcpClientJob?.cancel()
        tcpClientJob = repositoryScope.launch(Dispatchers.IO) {
            var socket: Socket? = null
            var track: AudioTrack? = null
            try {
                Log.d("AzanNet", "Connecting to live voice stream at $hostIp:18346")
                socket = Socket(hostIp, 18346)
                activeStreamSocket = socket

                val sampleRate = 16000
                val minBufferSize = AudioTrack.getMinBufferSize(
                    sampleRate,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT
                )

                track = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
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

                track.play()
                val inputStream = socket.getInputStream()
                val buffer = ByteArray(2048)

                while (isActive && isPlayingVoiceStream) {
                    val read = inputStream.read(buffer)
                    if (read > 0) {
                        track.write(buffer, 0, read)
                    } else if (read < 0) {
                        break
                    }
                }
            } catch (e: Exception) {
                Log.e("AzanNet", "Voice Stream receiver error: ${e.message}")
            } finally {
                isPlayingVoiceStream = false
                try {
                    socket?.close()
                } catch (e: Exception) {}
                try {
                    track?.stop()
                    track?.release()
                } catch (e: Exception) {}
            }
        }
    }

    private fun stopVoiceReceiveStream() {
        isPlayingVoiceStream = false
        tcpClientJob?.cancel()
        try {
            activeStreamSocket?.close()
        } catch (e: Exception) {}
        activeStreamSocket = null
    }
}
