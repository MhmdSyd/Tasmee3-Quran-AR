#!/usr/bin/env python3
"""Patch sherpa-onnx's scripts/whisper/export-onnx.py so it accepts a custom openai-format checkpoint.
Usage: patch_sherpa_export.py <in export-onnx.py> <out file>. The checkpoint path is taken from $WHISPER_CHECKPOINT
and --model can be any name (used only for output file names)."""
import re
import sys

s = open(sys.argv[1], encoding="utf-8").read()
s, n1 = re.subn(r"\n        choices=\[.*?\n            \],", "", s, count=1, flags=re.S)
s, n2 = re.subn(
    r'(\n    if name == "distil-medium\.en":)',
    '\n    if os.environ.get("WHISPER_CHECKPOINT"):\n        return whisper.load_model(os.environ["WHISPER_CHECKPOINT"])\\1',
    s, count=1)
assert n1 == 1 and n2 == 1, f"patch did not apply ({n1}, {n2}); sherpa-onnx script changed"
open(sys.argv[2], "w", encoding="utf-8").write(s)
print("patched")
