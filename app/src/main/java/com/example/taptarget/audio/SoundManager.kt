package com.example.taptarget.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.sin

/**
 * High-performance, 100% offline procedural sound synthesizer.
 * Uses AudioTrack with generated PCM 16-bit audio to avoid any external audio assets.
 */
class SoundManager {

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val sampleRate = 44100

    @Volatile
    var isEnabled: Boolean = true

    fun playSpawn() {
        if (!isEnabled) return
        scope.launch {
            val audio = generateTone(startFreq = 520f, endFreq = 640f, durationMs = 35)
            playPcm(audio)
        }
    }

    fun playHit() {
        if (!isEnabled) return
        scope.launch {
            val audio = generateTone(startFreq = 620f, endFreq = 980f, durationMs = 50)
            playPcm(audio)
        }
    }

    fun playMiss() {
        if (!isEnabled) return
        scope.launch {
            val audio = generateTone(startFreq = 220f, endFreq = 130f, durationMs = 180)
            playPcm(audio)
        }
    }

    fun playMilestone() {
        if (!isEnabled) return
        scope.launch {
            val chord = generateChord(listOf(523.25f, 659.25f, 783.99f), durationMs = 140)
            playPcm(chord)
        }
    }

    fun playCountdown(isGo: Boolean = false) {
        if (!isEnabled) return
        scope.launch {
            val freq = if (isGo) 880f else 440f
            val audio = generateTone(startFreq = freq, endFreq = freq, durationMs = 70)
            playPcm(audio)
        }
    }

    fun playTimerWarning() {
        if (!isEnabled) return
        scope.launch {
            val audio = generateTone(startFreq = 780f, endFreq = 740f, durationMs = 25)
            playPcm(audio)
        }
    }

    fun playGameOver() {
        if (!isEnabled) return
        scope.launch {
            val tone1 = generateTone(startFreq = 440f, endFreq = 392f, durationMs = 90)
            val tone2 = generateTone(startFreq = 349f, endFreq = 261f, durationMs = 160)
            playPcm(tone1 + tone2)
        }
    }

    private fun generateTone(startFreq: Float, endFreq: Float, durationMs: Int): ShortArray {
        val totalSamples = (sampleRate * (durationMs / 1000f)).toInt().coerceAtLeast(1)
        val buffer = ShortArray(totalSamples)
        var phase = 0.0

        for (i in 0 until totalSamples) {
            val progress = i.toFloat() / totalSamples
            val freq = startFreq + (endFreq - startFreq) * progress
            phase += 2.0 * PI * freq / sampleRate

            // Envelope: linear attack and release to eliminate clicking
            val attackSamples = (totalSamples * 0.1f).toInt().coerceAtLeast(1)
            val releaseSamples = (totalSamples * 0.25f).toInt().coerceAtLeast(1)
            val amplitude = when {
                i < attackSamples -> i.toFloat() / attackSamples
                i > totalSamples - releaseSamples -> (totalSamples - i).toFloat() / releaseSamples
                else -> 1f
            }

            val sample = (sin(phase) * amplitude * 0.75f * Short.MAX_VALUE).toInt()
            buffer[i] = sample.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        return buffer
    }

    private fun generateChord(freqs: List<Float>, durationMs: Int): ShortArray {
        val totalSamples = (sampleRate * (durationMs / 1000f)).toInt().coerceAtLeast(1)
        val buffer = ShortArray(totalSamples)
        val phases = DoubleArray(freqs.size)

        for (i in 0 until totalSamples) {
            val progress = i.toFloat() / totalSamples
            val attack = (totalSamples * 0.1f).toInt().coerceAtLeast(1)
            val release = (totalSamples * 0.3f).toInt().coerceAtLeast(1)
            val envelope = when {
                i < attack -> i.toFloat() / attack
                i > totalSamples - release -> (totalSamples - i).toFloat() / release
                else -> 1f
            }

            var sum = 0.0
            for (f in freqs.indices) {
                phases[f] += 2.0 * PI * freqs[f] / sampleRate
                sum += sin(phases[f])
            }
            val mixed = (sum / freqs.size) * envelope * 0.8f * Short.MAX_VALUE
            buffer[i] = mixed.toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        return buffer
    }

    private fun playPcm(buffer: ShortArray) {
        try {
            val track = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_GAME)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(buffer.size * 2)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()

            track.write(buffer, 0, buffer.size)
            track.play()

            // Release after playback finishes
            val durationMs = (buffer.size * 1000L) / sampleRate + 50L
            Thread.sleep(durationMs)
            track.stop()
            track.release()
        } catch (_: Exception) {
            // Audio output device busy or unavailable
        }
    }
}
