package com.tasmee3.app.speech

import android.content.res.AssetManager
import com.k2fsa.sherpa.onnx.FeatureConfig
import com.k2fsa.sherpa.onnx.OfflineModelConfig
import com.k2fsa.sherpa.onnx.OfflineRecognizer
import com.k2fsa.sherpa.onnx.OfflineRecognizerConfig
import com.k2fsa.sherpa.onnx.OfflineWhisperModelConfig

/** Whisper (Tarteel Quran fine-tune, ONNX int8) running on the phone through sherpa-onnx. Not streaming. */
class SherpaWhisperModel(assets: AssetManager, numThreads: Int = 2) : SpeechModel {
    private val recognizer: OfflineRecognizer
    private val chunks = ArrayList<FloatArray>()

    init {
        val config = OfflineRecognizerConfig(
            featConfig = FeatureConfig(sampleRate = 16_000, featureDim = 80),
            modelConfig = OfflineModelConfig(
                whisper = OfflineWhisperModelConfig(
                    encoder = "$DIR/encoder.int8.onnx",
                    decoder = "$DIR/decoder.int8.onnx",
                    language = "ar",
                    task = "transcribe",
                ),
                tokens = "$DIR/tokens.txt",
                numThreads = numThreads,
                provider = "cpu",
                modelType = "whisper",
            ),
            decodingMethod = "greedy_search",
        )
        recognizer = OfflineRecognizer(assetManager = assets, config = config)
    }

    @Synchronized
    override fun accept(audio: FloatArray) {
        chunks.add(audio)
    }

    @Synchronized
    override fun result(): String {
        val total = chunks.sumOf { it.size }
        val all = FloatArray(total)
        var offset = 0
        for (c in chunks) {
            c.copyInto(all, offset)
            offset += c.size
        }
        val stream = recognizer.createStream()
        try {
            stream.acceptWaveform(all, sampleRate)
            recognizer.decode(stream)
            return recognizer.getResult(stream).text.trim()
        } finally {
            stream.release()
        }
    }

    @Synchronized
    override fun reset() = chunks.clear()

    override fun close() = recognizer.release()

    companion object {
        const val DIR = "models/whisper"
        val REQUIRED_FILES = listOf("encoder.int8.onnx", "decoder.int8.onnx", "tokens.txt")

        fun isInstalled(assets: AssetManager): Boolean {
            val present = assets.list(DIR)?.toSet().orEmpty()
            return REQUIRED_FILES.all { it in present }
        }
    }
}
