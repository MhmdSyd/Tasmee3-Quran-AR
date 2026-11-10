# Speech model decision record

**2026-11-10: the owner chose `tarteel-ai/whisper-base-ar-quran`** (Apache-2.0), replacing `Abdou/quran-asr-small` from the handoff.
The model sits behind the `SpeechModel` interface, so the choice can be reversed without touching the matcher or screens.

## What is known (from the model card and file list)
- Fine-tune of openai/whisper-base (about 74M parameters). Card reports WER 5.75 on its own evaluation set; the dataset is not documented ("None dataset"), so this number is not comparable with other models.
- Files: PyTorch weights only (`pytorch_model.bin`, 290 MB fp32). **No ONNX.** We convert it ourselves (`tools/`, workflow *Export Tarteel Whisper model*).
- The card says nothing about diacritics, mobile use, or streaming.

## Consequences accepted with this choice
- Whisper is an offline model: it decodes a whole clip (up to 30 s) at once, with no word-by-word streaming. Words appear after the reciter pauses, not as they are spoken. The design's "real time" feel will be reduced.
- It can invent or repeat words on unclear audio or silence. The matcher checks against the expected text to limit this.
- Size: expect roughly 80-100 MB for the int8 files (the design said about 36 MB). To be measured in the first export run.
- Strictness "with diacritics" stays disabled until a test shows the model outputs diacritics (the export workflow reports this).

## Checks done by the export workflow (CI only; Hugging Face is not reachable from the dev sandbox)
1. HF to openai-whisper weight conversion, strict key load and a numeric logit comparison against the HF implementation (fails the run if different).
2. ONNX export + int8 quantization with the sherpa-onnx exporter (pinned commit).
3. Recitation clips (EveryAyah, Alafasy; used only for the test, not stored): word error rate against the real Quran text, fp32 vs int8, decode time, whether tashkeel appears.

Fallback if speed or accuracy is not acceptable: `Abdou/quran-asr-small` (see `DEVELOPER_HANDOFF.md` section 6).
