package com.tasmee3.app.ui.modeltest

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.tasmee3.app.speech.ModelStatus
import com.tasmee3.app.speech.SpeechPipeline
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

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

/** Developer screen (Settings > developer test): microphone -> voice gate -> model -> text, with timings. */
class ModelTestViewModel(app: Application) : AndroidViewModel(app) {
    private val pipeline = SpeechPipeline(app)
    private val _state = MutableStateFlow(ModelTestUiState())
    val state: StateFlow<ModelTestUiState> = _state.asStateFlow()

    private val listener = object : SpeechPipeline.Listener {
        override fun onAudio(level: Float, speaking: Boolean, audioMs: Long) {
            _state.update { it.copy(level = level, speaking = speaking, pendingDecodes = pipeline.pending.get()) }
        }

        override fun onRecognized(result: SpeechPipeline.Recognized) {
            _state.update {
                it.copy(
                    segments = it.segments + Segment(result.text, result.audioSeconds, result.decodeMillis),
                    pendingDecodes = (pipeline.pending.get() - 1).coerceAtLeast(0)
                )
            }
        }

        override fun onPartial(text: String, utteranceStartMs: Long, audioMs: Long) = Unit

        override fun onError(message: String) {
            _state.update { it.copy(error = message) }
        }
    }

    init {
        viewModelScope.launch { pipeline.status.collect { s -> _state.update { it.copy(model = s) } } }
        viewModelScope.launch(Dispatchers.IO) { pipeline.load() }
    }

    fun start() {
        pipeline.start(viewModelScope, listener)
        _state.update { it.copy(recording = true, error = null) }
    }

    fun stop() {
        viewModelScope.launch {
            pipeline.stop()
            _state.update { it.copy(recording = false, speaking = false, level = 0f) }
        }
    }

    fun onPermissionDenied(message: String) = _state.update { it.copy(error = message) }

    fun clear() = _state.update { it.copy(segments = emptyList()) }

    override fun onCleared() {
        pipeline.release()
        super.onCleared()
    }
}
