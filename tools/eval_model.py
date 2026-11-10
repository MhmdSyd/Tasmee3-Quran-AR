#!/usr/bin/env python3
"""Compare the fp32 reference model (openai-whisper format) with the sherpa-onnx int8 export on real recitation clips.

Usage: eval_model.py --pt model.pt --encoder enc.int8.onnx --decoder dec.int8.onnx --tokens tokens.txt \
                     --clips clips_dir --layout app/src/main/assets/mushaf --out report.md
Clips are 16 kHz mono wav named like 001001.wav (surah+ayah) or 001005-001007.wav (a verse range).
Everything is normalised to letters only before comparing (diacritics, waqf marks and tatweel removed).
"""
import argparse
import glob
import json
import os
import re
import time

import numpy as np
import soundfile as sf

DIAC = re.compile(r"[\u064B-\u065F\u0670\u06D6-\u06ED\u0640\u200E\u200F]")
HAS_TASHKEEL = re.compile(r"[\u064B-\u0652]")


def norm(s: str) -> str:
    s = DIAC.sub("", s)
    s = re.sub("[\u0623\u0625\u0622\u0671]", "\u0627", s)  # alef variants -> alef
    s = s.replace("\u0649", "\u064A")                        # alef maksura -> ya
    s = re.sub(r"[^\u0621-\u064A ]", " ", s)
    return " ".join(s.split())


def wer(ref: list, hyp: list) -> float:
    d = list(range(len(hyp) + 1))
    for i, r in enumerate(ref, 1):
        prev, d[0] = d[0], i
        for j, h in enumerate(hyp, 1):
            prev, d[j] = d[j], min(d[j] + 1, d[j - 1] + 1, prev + (r != h))
    return d[len(hyp)] / max(1, len(ref))


def load_verses(layout_dir: str) -> dict:
    verses = {}
    for path in sorted(glob.glob(os.path.join(layout_dir, "p*.json"))):
        for line in json.load(open(path, encoding="utf-8"))["l"]:
            if line["t"] != "w":
                continue
            for loc, text, _ in line["v"]:
                if text:
                    s, a, _w = loc.split(":")
                    verses.setdefault((int(s), int(a)), []).append(text)
    return verses


def expected_for(name: str, verses: dict) -> str:
    parts = name.split("-")
    s1, a1 = int(parts[0][:3]), int(parts[0][3:])
    s2, a2 = (int(parts[-1][:3]), int(parts[-1][3:]))
    assert s1 == s2
    words = []
    for a in range(a1, a2 + 1):
        words += verses[(s1, a)]
    return norm(" ".join(words))


def main() -> None:
    ap = argparse.ArgumentParser()
    for k in ("pt", "encoder", "decoder", "tokens", "clips", "layout", "out"):
        ap.add_argument(f"--{k}", required=True)
    args = ap.parse_args()

    import sherpa_onnx
    import whisper

    ref_model = whisper.load_model(args.pt, device="cpu")
    rec = sherpa_onnx.OfflineRecognizer.from_whisper(
        encoder=args.encoder, decoder=args.decoder, tokens=args.tokens,
        language="ar", task="transcribe", num_threads=2, decoding_method="greedy_search",
    )
    verses = load_verses(args.layout)
    rows, w_ref, w_int8 = [], [], []
    for path in sorted(glob.glob(os.path.join(args.clips, "*.wav"))):
        name = os.path.splitext(os.path.basename(path))[0]
        samples, sr = sf.read(path, dtype="float32")
        assert sr == 16000 and samples.ndim == 1, (path, sr, samples.shape)
        secs = len(samples) / sr
        exp = expected_for(name, verses)

        r = ref_model.transcribe(samples, language="ar", task="transcribe", temperature=0.0, fp16=False,
                                 condition_on_previous_text=False)["text"].strip()
        t0 = time.time()
        try:
            st = rec.create_stream()
            st.accept_waveform(sr, samples)
            rec.decode_stream(st)
            q = st.result.text.strip()
            err = ""
        except Exception as e:  # clips longer than 30 s may fail
            q, err = "", f"ERROR {e}"
        dt = time.time() - t0
        wr, wq = wer(exp.split(), norm(r).split()), wer(exp.split(), norm(q).split())
        if secs <= 30:
            w_ref.append(wr)
            w_int8.append(wq)
        rows.append((name, secs, exp, r, q, wr, wq, dt, err))

    out = ["# Model evaluation (Tarteel whisper-base-ar-quran)\n",
           "Letters-only comparison against the Quran text from the layout data. Reciter: Alafasy (EveryAyah), CPU of a CI runner.\n",
           f"Clips <= 30 s: mean WER fp32 reference = {100*np.mean(w_ref):.1f}%, int8 ONNX = {100*np.mean(w_int8):.1f}% (n={len(w_ref)})\n",
           "| clip | sec | WER fp32 | WER int8 | int8 decode s | tashkeel in output |", "|---|---|---|---|---|---|"]
    for name, secs, exp, r, q, wr, wq, dt, err in rows:
        out.append(f"| {name} | {secs:.1f} | {100*wr:.0f}% | {100*wq:.0f}% | {dt:.2f} | "
                   f"{'yes' if HAS_TASHKEEL.search(q) else 'no'} {err} |")
    out.append("\n## Texts\n")
    for name, secs, exp, r, q, wr, wq, dt, err in rows:
        out += [f"### {name}", f"- expected: {exp}", f"- fp32:     {r}", f"- int8:     {q}", ""]
    open(args.out, "w", encoding="utf-8").write("\n".join(out))
    print("\n".join(out))


if __name__ == "__main__":
    main()
