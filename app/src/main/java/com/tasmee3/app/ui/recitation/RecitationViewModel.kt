package com.tasmee3.app.ui.recitation

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.tasmee3.app.match.LastSession
import com.tasmee3.app.match.MatchEvent
import com.tasmee3.app.match.RecitationSession
import com.tasmee3.app.match.SessionResult
import com.tasmee3.app.match.SessionSnapshot
import com.tasmee3.app.mushaf.MushafMeta
import com.tasmee3.app.mushaf.MushafPage
import com.tasmee3.app.mushaf.MushafRepository
import com.tasmee3.app.speech.ModelStatus
import com.tasmee3.app.speech.SpeechPipeline
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class RecitationUiState(
    val page: MushafPage? = null,
    val meta: MushafMeta? = null,
    val snapshot: SessionSnapshot? = null,
    val model: ModelStatus = ModelStatus.Loading,
    val listening: Boolean = false,
    val speaking: Boolean = false,
    val levels: List<Float> = emptyList(),
    val error: String? = null,
    val finished: Boolean = false,
)

/** One recitation of one page: microphone -> model -> matcher -> screen state. Fully offline. */
class RecitationViewModel(app: Application, handle: SavedStateHandle) : AndroidViewModel(app) {
    private val pageNumber: Int = handle.get<Int>("page") ?: 1
    private val repo = MushafRepository(app)
    private val pipeline = SpeechPipeline(app)
    private val lock = Any()
    private var session: RecitationSession? = null
    private var autoHintJob: Job? = null
    @Volatile private var finishing = false

    private val _state = MutableStateFlow(RecitationUiState())
    val state: StateFlow<RecitationUiState> = _state.asStateFlow()

    private val listener = object : SpeechPipeline.Listener {
        override fun onAudio(level: Float, speaking: Boolean, audioMs: Long) {
            _state.update { it.copy(levels = (it.levels + level).takeLast(WAVE_BARS), speaking = speaking) }
        }

        override fun onRecognized(result: SpeechPipeline.Recognized) = handleRecognized(result)

        override fun onError(message: String) {
            _state.update { it.copy(error = message) }
        }
    }

    init {
        viewModelScope.launch { pipeline.status.collect { s -> _state.update { it.copy(model = s) } } }
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val meta = repo.meta()
                val page = repo.page(pageNumber)
                val s = RecitationSession(page, meta.basmala)
                synchronized(lock) { session = s }
                _state.update { it.copy(page = page, meta = meta, snapshot = s.snapshot()) }
                pipeline.load()
            } catch (e: Throwable) {
                _state.update { it.copy(error = e.message ?: e.javaClass.simpleName) }
            }
        }
    }

    fun startListening() {
        val ready = pipeline.status.value is ModelStatus.Ready
        if (!ready || session == null || _state.value.listening || _state.value.finished) return
        pipeline.start(viewModelScope, listener)
        _state.update { it.copy(listening = true, error = null) }
        startAutoHint()
    }

    fun pauseListening() {
        if (!_state.value.listening) return
        _state.update { it.copy(listening = false, speaking = false) }
        autoHintJob?.cancel()
        viewModelScope.launch { pipeline.stop() }
    }

    fun toggleListening() = if (_state.value.listening) pauseListening() else startListening()

    fun hint() = revealHint()

    fun finish() {
        if (finishing) return
        finishing = true
        viewModelScope.launch {
            autoHintJob?.cancel()
            _state.update { it.copy(listening = false, speaking = false) }
            pipeline.stop()
            pipeline.awaitIdle(20_000)
            val result = synchronized(lock) {
                session?.let { s ->
                    val name = _state.value.meta?.surahNames?.get(_state.value.page?.surah ?: 0).orEmpty()
                    SessionResult(pageNumber, name, s.summary())
                }
            }
            LastSession.result = result
            _state.update { it.copy(finished = true) }
        }
    }

    private fun handleRecognized(r: SpeechPipeline.Recognized) {
        val (events, snap) = synchronized(lock) {
            val s = session ?: return
            s.onSegment(r.text, r.startMs, r.endMs) to s.snapshot()
        }
        _state.update { it.copy(snapshot = snap) }
        if (events.any { it is MatchEvent.Completed }) finish()
    }

    private fun revealHint() {
        val (events, snap) = synchronized(lock) {
            val s = session ?: return
            s.hint(pipeline.audioMs) to s.snapshot()
        }
        _state.update { it.copy(snapshot = snap) }
        if (events.any { it is MatchEvent.Completed }) finish()
    }

    private fun startAutoHint() {
        autoHintJob?.cancel()
        autoHintJob = viewModelScope.launch {
            while (true) {
                delay(500)
                val due = synchronized(lock) {
                    session?.shouldAutoHint(
                        pipeline.audioMs, pipeline.lastSpeechMs, pipeline.speaking, pipeline.pending.get(), AUTO_HINT_MS
                    ) == true
                }
                if (due) revealHint()
            }
        }
    }

    override fun onCleared() {
        autoHintJob?.cancel()
        pipeline.release()
        super.onCleared()
    }

    companion object {
        const val WAVE_BARS = 24
        /** Default of the "auto hint after 5 s of silence" setting. The settings screen (step 6) will change it. */
        const val AUTO_HINT_MS = 5_000L
    }
}
