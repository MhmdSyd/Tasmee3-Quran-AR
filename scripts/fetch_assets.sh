#!/usr/bin/env bash
# Fetches third-party assets (NOT committed to git) and builds the app's data files.
# Runs locally (Linux/macOS/Git Bash) and in CI. Every download is pinned to a commit and checksum-verified.
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
LAYOUT="${LAYOUT:-QPC1}"   # QPC1 = 1405 Madani print, QPC2 = 1421 Madani print (565 pages break lines differently)

LAYOUT_REPO="zonetecde/mushaf-layout"
LAYOUT_COMMIT="421e9eaa991897ecd50d7abb79160651a45976d3"
FONT_REPO="thetruetruth/quran-data-kfgqpc"
FONT_COMMIT="281dbbe8eed1370daa5a023b6cd81655cbfd6473"
FONT_SHA256="a0636e68e375af9552470d67773936f54d536e6586ce2608311b2fe7f9cbec3a"
CAIRO_COMMIT="5bb28733cec313b63fbd415b574b5ae35c8aa8b8"
CAIRO_SHA256="667c987182391c91f4e57a2f455b1794fb5e3ee6ca4ef3383e86bb690fa9c964"
OFL_SHA256="a4554e1799d42e1405924b61eb0e0722ae1623b1f1f07f995348f96c496362a9"
case "$LAYOUT" in
  QPC1) OUT_SHA256="95e0269a774ac0a1f957ef1144726a7b432c622630dd62d268cf811d667e04e0" ;;
  QPC2) OUT_SHA256="3725724beedcaff5918e66dd7810933649dbb9429cbcdddc2d6ec024aa5a6631" ;;
  *) echo "LAYOUT must be QPC1 or QPC2" >&2; exit 1 ;;
esac

CACHE="$ROOT/.assets-cache"
ASSETS="$ROOT/app/src/main/assets"
FONTS="$ROOT/app/src/main/res/font"
mkdir -p "$CACHE" "$ASSETS/licenses" "$FONTS"

fetch() { # url dest sha256
  local url="$1" dest="$2" sha="$3"
  if [ -f "$dest" ] && echo "$sha  $dest" | sha256sum -c --status; then return; fi
  curl -fsSL --retry 3 -o "$dest" "$url"
  if ! echo "$sha  $dest" | sha256sum -c --status; then
    echo "Checksum mismatch for $dest" >&2; rm -f "$dest"; exit 1
  fi
}

echo "== Fonts"
fetch "https://raw.githubusercontent.com/$FONT_REPO/$FONT_COMMIT/hafs/font/hafs.18.ttf" "$FONTS/uthmanic_hafs.ttf" "$FONT_SHA256"
fetch "https://raw.githubusercontent.com/google/fonts/$CAIRO_COMMIT/ofl/cairo/Cairo%5Bslnt%2Cwght%5D.ttf" "$FONTS/cairo.ttf" "$CAIRO_SHA256"
fetch "https://raw.githubusercontent.com/google/fonts/$CAIRO_COMMIT/ofl/cairo/OFL.txt" "$ASSETS/licenses/Cairo_OFL.txt" "$OFL_SHA256"
cp "$ROOT"/third_party_licenses/*.txt "$ASSETS/licenses/"

echo "== Layout source ($LAYOUT)"
SRC="$CACHE/$LAYOUT"
mkdir -p "$SRC"
if [ "$(ls "$SRC" 2>/dev/null | wc -l)" -ne 604 ]; then
  seq -w 1 604 | xargs -P 8 -I{} curl -fsSL --retry 3 -o "$SRC/page-{}.json" \
    "https://raw.githubusercontent.com/$LAYOUT_REPO/$LAYOUT_COMMIT/mushaf/$LAYOUT/page-{}.json"
fi

echo "== Build compact per-page assets"
rm -rf "$ASSETS/mushaf"
python3 "$ROOT/tools/build_layout.py" "$SRC" "$ASSETS/mushaf"
ACTUAL="$(cd "$ASSETS/mushaf" && find . -type f -name '*.json' | LC_ALL=C sort | xargs sha256sum | sha256sum | cut -d' ' -f1)"
if [ "$ACTUAL" != "$OUT_SHA256" ]; then
  echo "Layout output checksum mismatch: expected $OUT_SHA256 got $ACTUAL" >&2; exit 1
fi

echo "== Speech runtime (sherpa-onnx) and voice-activity model"
SHERPA_VERSION="2.0.1"
SHERPA_AAR_SHA256="a8d09d1d32f3eb8520482c6d3d43fae674681c5b050385967c70a3c2c9bf0ee7"
VAD_SHA256="9e2449e1087496d8d4caba907f23e0bd3f78d91fa552479bb9c23ac09cbb1fd6"
mkdir -p "$ROOT/app/libs" "$ASSETS/models/whisper"
fetch "https://github.com/k2-fsa/sherpa-onnx/releases/download/v$SHERPA_VERSION/sherpa-onnx-$SHERPA_VERSION.aar" \
  "$ROOT/app/libs/sherpa-onnx-$SHERPA_VERSION.aar" "$SHERPA_AAR_SHA256"
fetch "https://github.com/k2-fsa/sherpa-onnx/releases/download/asr-models/silero_vad.onnx" \
  "$ASSETS/models/silero_vad.onnx" "$VAD_SHA256"

echo "== Whisper model (from this repo's GitHub Release, built by the Export workflow)"
MODEL_REPO="${MODEL_REPO:-MhmdSyd/Tasmee3-Quran-AR}"
MODEL_TAG="${MODEL_TAG:-model-tarteel-whisper-base-ar-quran-v1}"
MODEL_URL="https://github.com/$MODEL_REPO/releases/download/$MODEL_TAG"
MODEL_DIR="$ASSETS/models/whisper"
if curl -fsSL --retry 2 -o "$CACHE/model-SHA256SUMS" "$MODEL_URL/SHA256SUMS"; then
  for f in encoder.int8.onnx decoder.int8.onnx tokens.txt NOTICE.txt; do
    curl -fsSL --retry 3 -o "$MODEL_DIR/$f" "$MODEL_URL/$f"
  done
  (cd "$MODEL_DIR" && sha256sum -c "$CACHE/model-SHA256SUMS")
  if [ -f "$ROOT/scripts/model.sha256sums" ]; then
    cmp "$ROOT/scripts/model.sha256sums" "$CACHE/model-SHA256SUMS" || { echo "Model differs from pinned scripts/model.sha256sums" >&2; exit 1; }
  else
    echo "NOTE: model checksums are not pinned yet. Copy $CACHE/model-SHA256SUMS to scripts/model.sha256sums and commit it."
  fi
  cp "$MODEL_DIR/NOTICE.txt" "$ASSETS/licenses/Tarteel_whisper_NOTICE.txt"
  du -sh "$MODEL_DIR"
else
  echo "WARNING: model release '$MODEL_TAG' not found in $MODEL_REPO. The app still builds but will say the model is missing." >&2
  echo "         Run the 'Export Tarteel Whisper model' workflow with publish=true." >&2
  rm -rf "$MODEL_DIR"
fi

echo "All assets fetched and verified."
