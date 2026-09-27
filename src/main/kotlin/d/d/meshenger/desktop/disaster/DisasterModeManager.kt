/*
 * Copyright (C) 2026 Meshenger Contributors
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package d.d.meshenger.desktop.disaster

import kotlinx.coroutines.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.net.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.SourceDataLine

object DisasterModeManager {
    private const val DISASTER_PORT = 19451
    private const val MULTICAST_ADDR = "239.255.255.250"

    private val isDisasterModeActive = AtomicBoolean(false)
    private val isWhistleActive = AtomicBoolean(false)
    private val isStrobeActive = AtomicBoolean(false)

    var currentStatus = DisasterSignal.StatusType.SAFE
    var medicalNotes: String = "Windows Desktop Node"
    var senderName: String = try { InetAddress.getLocalHost().hostName } catch (e: Exception) { "Windows-PC" }

    private var socket: DatagramSocket? = null
    private var broadcastJob: Job? = null
    private var listenerJob: Job? = null
    private var whistleThread: Thread? = null

    private val activeSignalsMap = ConcurrentHashMap<String, DisasterSignal>()

    interface OnSignalReceivedListener {
        fun onSignalsUpdated(signals: List<DisasterSignal>)
    }

    private val listeners = mutableListOf<OnSignalReceivedListener>()

    fun registerListener(listener: OnSignalReceivedListener) {
        synchronized(listeners) { listeners.add(listener) }
        listener.onSignalsUpdated(getActiveSignals())
    }

    fun unregisterListener(listener: OnSignalReceivedListener) {
        synchronized(listeners) { listeners.remove(listener) }
    }

    private fun notifyListeners() {
        val signalList = getActiveSignals()
        synchronized(listeners) {
            listeners.forEach { it.onSignalsUpdated(signalList) }
        }
    }

    fun isDisasterModeActive(): Boolean = isDisasterModeActive.get()
    fun isWhistleActive(): Boolean = isWhistleActive.get()
    fun isStrobeActive(): Boolean = isStrobeActive.get()

    @Synchronized
    fun startDisasterMode() {
        if (isDisasterModeActive.get()) return
        isDisasterModeActive.set(true)

        runCatching {
            socket = DatagramSocket(DISASTER_PORT).apply {
                broadcast = true
                reuseAddress = true
            }
        }.onFailure {
            // Port already bound fallback socket
            socket = DatagramSocket().apply { broadcast = true }
        }

        startBroadcastLoop()
        startListenerLoop()
    }

    @Synchronized
    fun stopDisasterMode() {
        if (!isDisasterModeActive.get()) return
        isDisasterModeActive.set(false)

        broadcastJob?.cancel()
        listenerJob?.cancel()

        socket?.close()
        socket = null

        stopWhistle()
        stopStrobe()
    }

    private fun startBroadcastLoop() {
        broadcastJob = CoroutineScope(Dispatchers.IO).launch {
            while (isActive && isDisasterModeActive.get()) {
                sendBeacon()
                delay(3000) // Send beacon every 3 seconds
            }
        }
    }

    private fun sendBeacon() {
        try {
            val signal = DisasterSignal(
                senderName = senderName,
                status = currentStatus,
                latitude = null,
                longitude = null,
                timestamp = System.currentTimeMillis(),
                medicalNotes = medicalNotes,
                ipAddress = getLocalIpAddress(),
                macAddress = "WINDOWS-MAC",
                hopCount = 0
            )

            val jsonStr = Json.encodeToString(signal)
            val packetBytes = jsonStr.toByteArray(Charsets.UTF_8)

            // Broadcast to 255.255.255.255
            val broadcastAddr = InetAddress.getByName("255.255.255.255")
            val packet = DatagramPacket(packetBytes, packetBytes.size, broadcastAddr, DISASTER_PORT)
            socket?.send(packet)

            // Also send to Multicast address
            val multicastAddr = InetAddress.getByName(MULTICAST_ADDR)
            val multiPacket = DatagramPacket(packetBytes, packetBytes.size, multicastAddr, DISASTER_PORT)
            socket?.send(multiPacket)
        } catch (e: Exception) {
            System.err.println("Error sending Windows disaster beacon: $e")
        }
    }

    private fun startListenerLoop() {
        listenerJob = CoroutineScope(Dispatchers.IO).launch {
            val buffer = ByteArray(4096)
            while (isActive && isDisasterModeActive.get()) {
                try {
                    val packet = DatagramPacket(buffer, buffer.size)
                    socket?.receive(packet)

                    val jsonStr = String(packet.data, 0, packet.length, Charsets.UTF_8)
                    val signal = Json.decodeFromString<DisasterSignal>(jsonStr)
                    signal.ipAddress = packet.address.hostAddress

                    if (signal.ipAddress != getLocalIpAddress()) {
                        activeSignalsMap[signal.ipAddress] = signal
                        notifyListeners()
                    }
                } catch (e: Exception) {
                    if (!isActive) break
                }
            }
        }
    }

    fun getActiveSignals(): List<DisasterSignal> {
        val now = System.currentTimeMillis()
        // Filter out signals older than 30 seconds
        return activeSignalsMap.values
            .filter { (now - it.timestamp) < 30000 }
            .sortedByDescending { it.timestamp }
    }

    @Synchronized
    fun startWhistle() {
        if (isWhistleActive.get()) return
        isWhistleActive.set(true)

        whistleThread = Thread {
            val sampleRate = 44100.0f
            val format = AudioFormat(sampleRate, 16, 1, true, false)
            val line: SourceDataLine = AudioSystem.getSourceDataLine(format)
            line.open(format, 4410)
            line.start()

            val freq = 3500.0 // 3.5 kHz Acoustic Siren whistle tone
            val buffer = ByteArray(8820)

            while (isWhistleActive.get() && !Thread.currentThread().isInterrupted) {
                for (i in 0 until buffer.size / 2) {
                    val time = i / sampleRate
                    val angle = 2.0 * Math.PI * freq * time
                    val sample = (Math.sin(angle) * Short.MAX_VALUE * 0.8).toInt().toShort()
                    buffer[i * 2] = (sample.toInt() and 0xFF).toByte()
                    buffer[i * 2 + 1] = ((sample.toInt() shr 8) and 0xFF).toByte()
                }
                line.write(buffer, 0, buffer.size)
            }
            line.drain()
            line.stop()
            line.close()
        }.apply {
            isDaemon = true
            start()
        }
    }

    @Synchronized
    fun stopWhistle() {
        isWhistleActive.set(false)
        whistleThread?.interrupt()
        whistleThread = null
    }

    @Synchronized
    fun startStrobe() {
        isStrobeActive.set(true)
    }

    @Synchronized
    fun stopStrobe() {
        isStrobeActive.set(false)
    }

    private fun getLocalIpAddress(): String {
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val iface = interfaces.nextElement()
                if (iface.isLoopback || !iface.isUp) continue
                val addrs = iface.inetAddresses
                while (addrs.hasMoreElements()) {
                    val addr = addrs.nextElement()
                    if (addr is Inet4Address && !addr.isLoopbackAddress) {
                        return addr.hostAddress
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return "127.0.0.1"
    }
}
