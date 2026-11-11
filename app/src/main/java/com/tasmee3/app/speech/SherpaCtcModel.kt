package com.tasmee3.app.speech

import android.content.res.AssetManager
import com.k2fsa.sherpa.onnx.FeatureConfig
import com.k2fsa.sherpa.onnx.OfflineModelConfig
import com.k2fsa.sherpa.onnx.OfflineRecognizer
import com.k2fsa.sherpa.onnx.OfflineRecognizerConfig
import com.k2fsa.sherpa.onnx.OfflineZipformerCtcModelConfig

/**
 * Abdou/quran-asr-small (zipformer CTC, int8) running on the phone through sherpa-onnx.
 * It writes the Arabic LETTERS of the recitation (no vowel marks), which is exactly what the matcher compares.
 * Fast: it decodes a phrase in a fraction of the time it took to say it.
 */
class SherpaCtcModel(assets: AssetManager, numThreads: Int = 2) : SpeechModel {
    private val recognizer: OfflineRecognizer
    private val chunks = ArrayList<FloatArray>()

    init {
        val config = OfflineRecognizerConfig(
            featConfig = FeatureConfig(sampleRate = 16_000, featureDim = 80),
            modelConfig = OfflineModelConfig(
                zipformerCtc = OfflineZipformerCtcModelConfig(model = "$DIR/model.int8.onnx"),
                tokens = "$DIR/tokens.txt",
                numThreads = numThreads,
                provider = "cpu",
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
        val all = FloatArray(chunks.sumOf { it.size })
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
        const val DIR = "models/quran-asr"
        val REQUIRED_FILES = listOf("model.int8.onnx", "tokens.txt")

        fun isInstalled(assets: AssetManager): Boolean {
            val present = assets.list(DIR)?.toSet().orEmpty()
            return REQUIRED_FILES.all { it in present }
        }
    }
}
