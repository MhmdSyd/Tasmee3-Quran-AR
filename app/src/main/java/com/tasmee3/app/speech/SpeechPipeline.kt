package com.tasmee3.app.speech

import android.content.Context
import com.k2fsa.sherpa.onnx.SileroVadModelConfig
import com.k2fsa.sherpa.onnx.SpeechSegment
import com.k2fsa.sherpa.onnx.Vad
import com.k2fsa.sherpa.onnx.VadModelConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.abs

/**
 * Microphone -> voice gate (Silero VAD) -> speech model, entirely on the device.
 * Audio is cut at pauses; each segment is recognized separately on one background thread.
 * All times are on the AUDIO clock ([audioMs]) so they do not depend on decode speed. The clock keeps running
 * across pause/resume and never includes time the microphone was off.
 */
class SpeechPipeline(context: Context) {
    data class Recognized(
        val text: String,
        val startMs: Long,
        val endMs: Long,
        val audioSeconds: Float,
        val decodeMillis: Long,
    )

    interface Listener {
        fun onAudio(level: Float, speaking: Boolean, audioMs: Long)
        fun onRecognized(result: Recognized)
        fun onError(message: String)
    }

    private val assets = context.applicationContext.assets
    private val _status = MutableStateFlow<ModelStatus>(ModelStatus.Loading)
    val status: StateFlow<ModelStatus> = _status.asStateFlow()

    private var model: SpeechModel? = null
    private var vad: Vad? = null
    private var listener: Listener? = null
    private var job: Job? = null
    private var sessionOffsetMs = 0L
    private val frames = FrameBuffer(VAD_WINDOW)

    private val decodeExecutor = Executors.newSingleThreadExecutor()
    private val decodeScope = CoroutineScope(SupervisorJob() + decodeExecutor.asCoroutineDispatcher())

    @Volatile var audioMs = 0L
        private set
    @Volatile var lastSpeechMs = 0L
        private set
    @Volatile var speaking = false
        private set
    val pending = AtomicInteger(0)

    /** Blocking (loads about 100 MB of model files): call from a background thread. */
    fun load() {
        if (!SherpaWhisperModel.isInstalled(assets)) {
            _status.value = ModelStatus.Missing
            return
        }
        try {
            val t0 = System.currentTimeMillis()
            model = SherpaWhisperModel(assets)
            vad = Vad(
                assetManager = assets,
                config = VadModelConfig(
                    sileroVadModelConfig = SileroVadModelConfig(
                        model = "models/silero_vad.onnx",
                        threshold = 0.5f,
                        minSilenceDuration = 0.6f,   // a pause this long ends a segment
                        minSpeechDuration = 0.25f,
                        windowSize = VAD_WINDOW,
                        maxSpeechDuration = 25f,     // Whisper reads at most 30 s
                    ),
                    sampleRate = SAMPLE_RATE,
                    numThreads = 1,
                )
            )
            _status.value = ModelStatus.Ready(System.currentTimeMillis() - t0)
        } catch (e: Throwable) {
            _status.value = ModelStatus.Failed(e.message ?: e.javaClass.simpleName)
        }
    }

    fun start(scope: CoroutineScope, listener: Listener) {
        val v = vad ?: return
        if (job?.isActive == true) return
        this.listener = listener
        sessionOffsetMs = audioMs
        frames.clear()
        v.reset()
        job = scope.launch(Dispatchers.Default) {
            try {
                MicRecorder(SAMPLE_RATE).chunks().collect { chunk ->
                    audioMs += chunk.size * 1000L / SAMPLE_RATE
                    var peak = 0f
                    for (x in chunk) peak = maxOf(peak, abs(x))
                    for (frame in frames.push(chunk)) v.acceptWaveform(frame)
                    speaking = v.isSpeechDetected()
                    if (speaking) lastSpeechMs = audioMs
                    drain(v)
                    listener.onAudio(minOf(1f, peak * 3f), speaking, audioMs)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                listener.onError(e.message ?: e.javaClass.simpleName)
            }
        }
    }

    /** Stops the microphone and sends the last half-finished segment to recognition. */
    suspend fun stop() {
        val running = job ?: return
        running.cancelAndJoin()
        job = null
        vad?.let { v ->
            v.flush()
            drain(v)
            v.reset()
        }
        frames.clear()
        speaking = false
    }

    /** Waits until all queued segments have been recognized (or the timeout passes). */
    suspend fun awaitIdle(timeoutMs: Long) {
        var waited = 0L
        while (pending.get() > 0 && waited < timeoutMs) {
            delay(100)
            waited += 100
        }
    }

    fun release() {
        job?.cancel()
        decodeScope.cancel()
        decodeExecutor.shutdown()
        runCatching { vad?.release() }
        runCatching { model?.close() }
    }

    private fun drain(v: Vad) {
        while (!v.empty()) {
            val segment = v.front()
            v.pop()
            enqueue(segment)
        }
    }

    private fun enqueue(segment: SpeechSegment) {
        val m = model ?: return
        val l = listener ?: return
        val startMs = sessionOffsetMs + segment.start * 1000L / SAMPLE_RATE
        val endMs = startMs + segment.samples.size * 1000L / SAMPLE_RATE
        pending.incrementAndGet()
        decodeScope.launch {
            val t0 = System.currentTimeMillis()
            var text = ""
            try {
                m.reset()
                m.accept(segment.samples)
                text = m.result()
            } catch (e: Throwable) {
                l.onError(e.message ?: e.javaClass.simpleName)
            }
            val ms = System.currentTimeMillis() - t0
            try {
                l.onRecognized(Recognized(text, startMs, endMs, segment.samples.size / SAMPLE_RATE.toFloat(), ms))
            } finally {
                pending.decrementAndGet()
            }
        }
    }

    companion object {
        const val SAMPLE_RATE = 16_000
        const val VAD_WINDOW = 512
    }
}
