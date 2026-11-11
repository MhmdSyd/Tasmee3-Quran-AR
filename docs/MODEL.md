# Speech model decision record

## Current: `Abdou/quran-asr-small` (changed 2026-11-11)
- 33M parameters, **36 MB** int8 ONNX (`model.int8.onnx` + `tokens.txt`), MIT licence, zipformer CTC loaded by sherpa-onnx.
- Quran-Lab benchmark (600 clips): 4.49% WER / 2.17% CER, #2 of 33 at submission; weakest on phone recordings (10.45% WER).
- It writes the Arabic LETTERS of the recitation in Uthmani spelling, no vowel marks. This matches the letters-only matcher.
- Fast: a CTC model decodes a phrase far quicker than real time. Words appear shortly after each pause.
- Fetched at build time by `scripts/fetch_assets.sh` straight from Hugging Face, pinned to a commit and checked against
  Hugging Face's own SHA-256 / blob ids. After the first good build copy `.assets-cache/model.lock` (printed in the log)
  to `scripts/model.lock` and commit it: later builds are then byte-for-byte pinned.
- Lower-end phones: `QURAN_MODEL=Abdou/quran-asr-tiny bash scripts/fetch_assets.sh` (19 MB, about 7% WER). Same file names and format.

## Why the switch
The earlier choice, `tarteel-ai/whisper-base-ar-quran`, ships only PyTorch weights. Our export release was never published,
so the app contained no model. Whisper is also slower, can invent words, needs about 3x the storage, and its output
format was never verified. The files for `quran-asr-small` already exist in the phone format, so no conversion step is needed.

## Limits (unchanged)
- No vowel marks in the output: "with diacritics" correction stays disabled. A streaming phoneme model (for example
  `Quran-Lab/zipformer_p-arabic-v3`) would be the upgrade path, but its licence is non-commercial.
- Training data includes Tarteel "tlog" (CC-BY-4.0): attribution is in `THIRD_PARTY.md` and shipped in `assets/licenses/`.

## Whisper export tooling (not used)
`tools/hf_to_openai_whisper.py`, `tools/patch_sherpa_export.py`, `tools/eval_model.py` and the disabled workflow
`.github/workflows/export-model.yml.disabled` convert and test the Tarteel Whisper model. Kept only for reference.
