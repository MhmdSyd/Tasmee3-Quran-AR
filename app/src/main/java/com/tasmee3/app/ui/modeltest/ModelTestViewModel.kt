package com.tasmee3.app.ui.modeltest

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.k2fsa.sherpa.onnx.SileroVadModelConfig
import com.k2fsa.sherpa.onnx.Vad
import com.k2fsa.sherpa.onnx.VadModelConfig
import com.tasmee3.app.speech.FrameBuffer
import com.tasmee3.app.speech.ModelStatus
import com.tasmee3.app.speech.MicRecorder
import com.tasmee3.app.speech.SherpaWhisperModel
import com.tasmee3.app.speech.SpeechModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.concurrent.Executors
import kotlin.math.abs

data class Segment(val text: String, val audioSeconds: Float, val decodeMillis: Long)

data class ModelTestUiState(
    val model: ModelStatus = ModelStatus.Loading,
    val recording: Boolean = false,
    val speaking: Boolean = false,
    val level: Float = 0f,
    val pendingDecodes: Int = 0,
    val segments: List<Segment> = emptyList(),
    val error: String? = null,
)

/** Developer screen: microphone -> voice gate -> Whisper -> text list. Measures decode time per segment. */
class ModelTestViewModel(app: Application) : AndroidViewModel(app) {
    private val _state = MutableStateFlow(ModelTestUiState())
    val state: StateFlow<ModelTestUiState> = _state.asStateFlow()

    private var model: SpeechModel? = null
    private var vad: Vad? = null
    private var recordJob: Job? = null
    private val frames = FrameBuffer(VAD_WINDOW)

    private val decodeExecutor = Executors.newSingleThreadExecutor()
    private val decodeScope = CoroutineScope(SupervisorJob() + decodeExecutor.asCoroutineDispatcher())

    init {
        viewModelScope.launch(Dispatchers.IO) { load() }
    }

    private fun load() {
        val assets = getApplication<Application>().assets
        if (!SherpaWhisperModel.isInstalled(assets)) {
            _state.update { it.copy(model = ModelStatus.Missing) }
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
                        minSilenceDuration = 0.6f,   // pause that ends a segment
                        minSpeechDuration = 0.25f,
                        windowSize = VAD_WINDOW,
                        maxSpeechDuration = 25f,     // Whisper reads at most 30 s
                    ),
                    sampleRate = 16_000,
                    numThreads = 1,
                )
            )
            _state.update { it.copy(model = ModelStatus.Ready(System.currentTimeMillis() - t0)) }
        } catch (e: Throwable) {
            _state.update { it.copy(model = ModelStatus.Failed(e.message ?: e.javaClass.simpleName)) }
        }
    }

    fun start() {
        val v = vad ?: return
        if (recordJob?.isActive == true) return
        _state.update { it.copy(recording = true, error = null) }
        recordJob = viewModelScope.launch(Dispatchers.Default) {
            try {
                MicRecorder().chunks().collect { chunk ->
                    var peak = 0f
                    for (x in chunk) peak = maxOf(peak, abs(x))
                    for (frame in frames.push(chunk)) v.acceptWaveform(frame)
                    drain(v)
                    _state.update { it.copy(level = minOf(1f, peak * 3f), speaking = v.isSpeechDetected()) }
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Throwable) {
                _state.update { it.copy(error = e.message ?: e.javaClass.simpleName) }
            }
        }
    }

    fun stop() {
        val job = recordJob ?: return
        viewModelScope.launch(Dispatchers.Default) {
            job.cancelAndJoin()
            vad?.let { v ->
                v.flush()          // finish a segment that was still being spoken
                drain(v)
                v.reset()
            }
            frames.clear()
            _state.update { it.copy(recording = false, speaking = false, level = 0f) }
        }
    }

    fun onPermissionDenied(message: String) = _state.update { it.copy(error = message) }

    fun clear() = _state.update { it.copy(segments = emptyList()) }

    private fun drain(v: Vad) {
        while (!v.empty()) {
            val samples = v.front().samples
            v.pop()
            enqueueDecode(samples)
        }
    }

    private fun enqueueDecode(samples: FloatArray) {
        val m = model ?: return
        _state.update { it.copy(pendingDecodes = it.pendingDecodes + 1) }
        decodeScope.launch {
            val t0 = System.currentTimeMillis()
            val text = try {
                m.reset()
                m.accept(samples)
                m.result()
            } catch (e: Throwable) {
                "(${e.message ?: e.javaClass.simpleName})"
            }
            val segment = Segment(text, samples.size / 16_000f, System.currentTimeMillis() - t0)
            _state.update { it.copy(pendingDecodes = it.pendingDecodes - 1, segments = it.segments + segment) }
        }
    }

    override fun onCleared() {
        recordJob?.cancel()
        decodeScope.cancel()
        decodeExecutor.shutdown()
        runCatching { vad?.release() }
        runCatching { model?.close() }
        super.onCleared()
    }

    companion object {
        const val VAD_WINDOW = 512
    }
}
