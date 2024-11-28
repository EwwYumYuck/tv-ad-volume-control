package com.example.tvadcontrol

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jtransforms.fft.DoubleFFT_1D
import kotlin.math.abs
import kotlin.math.log10

class AudioAnalyzer {
    private val SAMPLE_RATE = 44100
    private val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
    private val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
    private val BUFFER_SIZE = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT)
    
    private var audioRecord: AudioRecord? = null
    private val fft = DoubleFFT_1D((BUFFER_SIZE / 2).toLong())
    
    private var isInitialized = false
    
    fun initialize() {
        if (!isInitialized) {
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE,
                CHANNEL_CONFIG,
                AUDIO_FORMAT,
                BUFFER_SIZE
            )
            isInitialized = true
        }
    }
    
    suspend fun detectAdvertisement(sensitivityThreshold: Int): Boolean = withContext(Dispatchers.Default) {
        if (!isInitialized) return@withContext false
        
        val buffer = ShortArray(BUFFER_SIZE)
        val fftBuffer = DoubleArray(BUFFER_SIZE)
        
        audioRecord?.startRecording()
        
        // Read audio data
        val readResult = audioRecord?.read(buffer, 0, BUFFER_SIZE) ?: -1
        if (readResult < 0) return@withContext false
        
        // Convert to double for FFT
        for (i in 0 until BUFFER_SIZE) {
            fftBuffer[i] = buffer[i].toDouble()
        }
        
        // Perform FFT
        fft.realForward(fftBuffer)
        
        // Analyze frequency components
        val isAd = analyzeSpectrum(fftBuffer, sensitivityThreshold)
        
        audioRecord?.stop()
        
        return@withContext isAd
    }
    
    private fun analyzeSpectrum(fftData: DoubleArray, sensitivityThreshold: Int): Boolean {
        var commercialProbability = 0.0
        
        // Analyze specific frequency ranges that are common in commercials
        // Typically commercials have higher energy in mid-high frequencies
        val midFreqEnergy = calculateBandEnergy(fftData, 2000, 4000)
        val highFreqEnergy = calculateBandEnergy(fftData, 4000, 8000)
        val lowFreqEnergy = calculateBandEnergy(fftData, 200, 2000)
        
        // Commercials often have higher mid-high frequency content compared to regular content
        if (midFreqEnergy > lowFreqEnergy * 1.5) commercialProbability += 0.3
        if (highFreqEnergy > lowFreqEnergy * 1.2) commercialProbability += 0.3
        
        // Check for sudden volume increases
        val overallLoudness = calculateOverallLoudness(fftData)
        if (overallLoudness > -10) commercialProbability += 0.4
        
        return commercialProbability * 100 > sensitivityThreshold
    }
    
    private fun calculateBandEnergy(fftData: DoubleArray, startFreq: Int, endFreq: Int): Double {
        val startBin = (startFreq * BUFFER_SIZE / SAMPLE_RATE).coerceIn(0, BUFFER_SIZE - 1)
        val endBin = (endFreq * BUFFER_SIZE / SAMPLE_RATE).coerceIn(0, BUFFER_SIZE - 1)
        
        var energy = 0.0
        for (i in startBin until endBin) {
            val magnitude = abs(fftData[i])
            energy += magnitude * magnitude
        }
        return energy
    }
    
    private fun calculateOverallLoudness(fftData: DoubleArray): Double {
        var sum = 0.0
        for (i in 0 until BUFFER_SIZE / 2) {
            val magnitude = abs(fftData[i])
            sum += magnitude * magnitude
        }
        return 10 * log10(sum / (BUFFER_SIZE / 2))
    }
    
    fun release() {
        audioRecord?.release()
        audioRecord = null
        isInitialized = false
    }
}
