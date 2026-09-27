/*
 * Copyright (C) 2026 Meshenger Contributors
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package d.d.meshenger.desktop.disaster

import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.atomic.AtomicBoolean
import javax.sound.sampled.*
import kotlin.math.min
import kotlin.math.sqrt

object RubbleAudioProcessor {
    private const val SAMPLE_RATE = 16000.0f
    private const val SAMPLE_SIZE_IN_BITS = 16
    private const val CHANNELS = 1
    private const val SIGNED = true
    private const val BIG_ENDIAN = false

    private val isListening = AtomicBoolean(false)
    private var targetDataLine: TargetDataLine? = null
    private var sourceDataLine: SourceDataLine? = null
    private var audioThread: Thread? = null

    @Volatile
    var gainMultiplier: Float = 3.0f // Default 3x amplification boost

    interface OnAudioAmplitudeListener {
        fun onAmplitudeChanged(amplitudePercentage: Int, peakDetected: Boolean)
    }

    private var amplitudeListener: OnAudioAmplitudeListener? = null

    fun setOnAudioAmplitudeListener(listener: OnAudioAmplitudeListener?) {
        this.amplitudeListener = listener
    }

    @Synchronized
    fun startListening() {
        if (isListening.get()) return

        isListening.set(true)
        audioThread = Thread {
            runAudioProcessingLoop()
        }.apply {
            priority = Thread.MAX_PRIORITY
            isDaemon = true
            start()
        }
    }

    @Synchronized
    fun stopListening() {
        if (!isListening.get()) return
        isListening.set(false)
        audioThread?.interrupt()
        audioThread = null
        releaseResources()
    }

    private fun runAudioProcessingLoop() {
        val format = AudioFormat(SAMPLE_RATE, SAMPLE_SIZE_IN_BITS, CHANNELS, SIGNED, BIG_ENDIAN)
        val bufferSize = 2048
        val byteBuffer = ByteArray(bufferSize)
        val shortBuffer = ShortArray(bufferSize / 2)

        try {
            val micDataLineInfo = DataLine.Info(TargetDataLine::class.java, format)
            val speakerDataLineInfo = DataLine.Info(SourceDataLine::class.java, format)

            if (!AudioSystem.isLineSupported(micDataLineInfo) || !AudioSystem.isLineSupported(speakerDataLineInfo)) {
                System.err.println("Target or Source Audio Line not supported on Windows")
                isListening.set(false)
                return
            }

            targetDataLine = AudioSystem.getLine(micDataLineInfo) as TargetDataLine
            sourceDataLine = AudioSystem.getLine(speakerDataLineInfo) as SourceDataLine

            targetDataLine?.open(format, bufferSize * 2)
            sourceDataLine?.open(format, bufferSize * 2)

            targetDataLine?.start()
            sourceDataLine?.start()

            var prevSample = 0.0f
            val alpha = 0.85f // High-pass filter coefficient for cutting low rumble

            while (isListening.get() && !Thread.currentThread().isInterrupted) {
                val bytesRead = targetDataLine?.read(byteBuffer, 0, byteBuffer.size) ?: 0
                if (bytesRead <= 0) continue

                val samplesRead = bytesRead / 2
                ByteBuffer.wrap(byteBuffer, 0, bytesRead).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(shortBuffer, 0, samplesRead)

                var sumSquares = 0.0
                for (i in 0 until samplesRead) {
                    val rawSample = shortBuffer[i].toFloat()

                    // High-pass filter (300Hz-3.4kHz bandpass simulation)
                    val filteredSample = rawSample - prevSample + alpha * prevSample
                    prevSample = rawSample

                    // Apply Gain Amplification Boost with Soft Clipping
                    var boostedSample = filteredSample * gainMultiplier
                    if (boostedSample > 32767.0f) boostedSample = 32767.0f
                    if (boostedSample < -32768.0f) boostedSample = -32768.0f

                    shortBuffer[i] = boostedSample.toInt().toShort()
                    sumSquares += (boostedSample * boostedSample).toDouble()
                }

                // RMS Calculation for Amplitude Meter & Peak Detection
                val rms = sqrt(sumSquares / samplesRead)
                val maxRms = 32768.0
                val amplitudePercentage = min(100, ((rms / maxRms) * 350).toInt())
                val peakDetected = amplitudePercentage > 65

                amplitudeListener?.onAmplitudeChanged(amplitudePercentage, peakDetected)

                // Write amplified sound to Speaker Output
                ByteBuffer.wrap(byteBuffer, 0, bytesRead).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().put(shortBuffer, 0, samplesRead)
                sourceDataLine?.write(byteBuffer, 0, bytesRead)
            }
        } catch (e: Exception) {
            System.err.println("Rubble audio loop exception: $e")
        } finally {
            releaseResources()
        }
    }

    private fun releaseResources() {
        try {
            targetDataLine?.stop()
            targetDataLine?.close()
            targetDataLine = null

            sourceDataLine?.stop()
            sourceDataLine?.close()
            sourceDataLine = null
        } catch (e: Exception) {
            System.err.println("Audio resource release error: $e")
        }
    }

    fun isListeningActive(): Boolean = isListening.get()
}
