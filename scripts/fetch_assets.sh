#!/usr/bin/env bash
# Fetches third-party assets (NOT committed to git) and builds the app's data files.
# Runs locally (Linux/macOS/Git Bash) and in CI. Every download is pinned to a commit and checksum-verified.
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
# Official QUL (Tarteel Quran Universal Library) files, stored in git under data/qul/ (downloaded from a QUL account):
#   qpc-v1-15-lines.db   layout "Quran Complex V1 (1405 print)", 604 pages, 15 lines
#   qpc-hafs.db          Uthmani Hafs script, verse by verse
#   UthmanicHafs1Ver18.ttf  the KFGQPC Uthmanic Hafs font that script is made for
# Their checksums are pinned here so a changed or damaged copy fails the build.
LAYOUT_DB_SHA256="54e5cf604da95f9e65db3fb9dce0c75c17d197e41543ba846ed49264f9c0442a"
SCRIPT_DB_SHA256="bde64c73ec174bd163d21af2f973bf81a85eef50fd279ebde637f8d7c012fc36"
FONT_SHA256="a0636e68e375af9552470d67773936f54d536e6586ce2608311b2fe7f9cbec3a"
NAMES_SHA256="575417a57423a3084e0bbf01cdf8411e688b00e660f125407563e115be73f2ab"
OUT_SHA256="b201ec26bc695564877c6bd8b5118e02759a035c886ec5d09d1e9ba199a1d9bb"   # the pages built from the files above
CAIRO_COMMIT="5bb28733cec313b63fbd415b574b5ae35c8aa8b8"
CAIRO_SHA256="667c987182391c91f4e57a2f455b1794fb5e3ee6ca4ef3383e86bb690fa9c964"
OFL_SHA256="a4554e1799d42e1405924b61eb0e0722ae1623b1f1f07f995348f96c496362a9"

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
check() { echo "$2  $1" | sha256sum -c --status || { echo "Checksum mismatch for $1 (is it the original QUL file?)" >&2; exit 1; }; }
check "$ROOT/data/qul/UthmanicHafs1Ver18.ttf" "$FONT_SHA256"
cp "$ROOT/data/qul/UthmanicHafs1Ver18.ttf" "$FONTS/uthmanic_hafs.ttf"
fetch "https://raw.githubusercontent.com/google/fonts/$CAIRO_COMMIT/ofl/cairo/Cairo%5Bslnt%2Cwght%5D.ttf" "$FONTS/cairo.ttf" "$CAIRO_SHA256"
fetch "https://raw.githubusercontent.com/google/fonts/$CAIRO_COMMIT/ofl/cairo/OFL.txt" "$ASSETS/licenses/Cairo_OFL.txt" "$OFL_SHA256"
cp "$ROOT"/third_party_licenses/*.txt "$ASSETS/licenses/"

echo "== Mushaf pages from the official QUL layout and text (Madani mushaf, 1405 H print)"
check "$ROOT/data/qul/qpc-v1-15-lines.db" "$LAYOUT_DB_SHA256"
check "$ROOT/data/qul/qpc-hafs.db" "$SCRIPT_DB_SHA256"
check "$ROOT/data/surah-names-ar.json" "$NAMES_SHA256"
rm -rf "$ASSETS/mushaf"
python3 "$ROOT/tools/build_layout.py" "$ROOT/data" "$ASSETS/mushaf"
ACTUAL="$(cd "$ASSETS/mushaf" && find . -type f -name '*.json' | LC_ALL=C sort | xargs sha256sum | sha256sum | cut -d' ' -f1)"
if [ "$ACTUAL" != "$OUT_SHA256" ]; then
  echo "Layout output checksum mismatch: expected $OUT_SHA256 got $ACTUAL" >&2; exit 1
fi

echo "== Speech runtime (sherpa-onnx) and voice-activity model"
SHERPA_VERSION="2.0.1"
SHERPA_AAR_SHA256="a8d09d1d32f3eb8520482c6d3d43fae674681c5b050385967c70a3c2c9bf0ee7"
VAD_SHA256="9e2449e1087496d8d4caba907f23e0bd3f78d91fa552479bb9c23ac09cbb1fd6"
mkdir -p "$ROOT/app/libs" "$ASSETS/models"
fetch "https://github.com/k2-fsa/sherpa-onnx/releases/download/v$SHERPA_VERSION/sherpa-onnx-$SHERPA_VERSION.aar" \
  "$ROOT/app/libs/sherpa-onnx-$SHERPA_VERSION.aar" "$SHERPA_AAR_SHA256"
fetch "https://github.com/k2-fsa/sherpa-onnx/releases/download/asr-models/silero_vad.onnx" \
  "$ASSETS/models/silero_vad.onnx" "$VAD_SHA256"

echo "== Quran speech model (Hugging Face, pinned to a commit and verified)"
# Abdou/quran-asr-small: 33M parameters, 36 MB int8, MIT. For very weak phones use QURAN_MODEL=Abdou/quran-asr-tiny (19 MB, less accurate).
QURAN_MODEL="${QURAN_MODEL:-Abdou/quran-asr-small}"
HF_ENDPOINT="${HF_ENDPOINT:-https://huggingface.co}"
MODEL_DIR="$ASSETS/models/quran-asr"
rm -rf "$ASSETS/models/whisper"   # old Whisper folder from earlier steps, no longer used
mkdir -p "$MODEL_DIR"
export QURAN_MODEL HF_ENDPOINT MODEL_DIR ROOT CACHE
python3 - <<'PY'
import hashlib, json, os, pathlib, shutil, sys, urllib.request

repo = os.environ["QURAN_MODEL"]
endpoint = os.environ["HF_ENDPOINT"]
out = pathlib.Path(os.environ["MODEL_DIR"])
root = pathlib.Path(os.environ["ROOT"])
cache = pathlib.Path(os.environ["CACHE"])
lock_path = root / "scripts" / "model.lock"
FILES = ["model.int8.onnx", "tokens.txt"]


def get(url):
    return urllib.request.urlopen(urllib.request.Request(url, headers={"User-Agent": "tasmee3-fetch"}), timeout=300)


lock = json.loads(lock_path.read_text()) if lock_path.exists() else None
if lock and lock["repo"] != repo:
    sys.exit(f"scripts/model.lock is for {lock['repo']}, not {repo}. Delete the lock file to switch models.")
rev = os.environ.get("MODEL_REV") or (lock["rev"] if lock else json.load(get(f"{endpoint}/api/models/{repo}"))["sha"])
tree = {e["path"]: e for e in json.load(get(f"{endpoint}/api/models/{repo}/tree/{rev}"))}

found = {}
for name in FILES:
    entry = tree.get(name)
    if entry is None:
        sys.exit(f"{repo}@{rev} has no file {name}")
    dest = out / name
    with get(f"{endpoint}/{repo}/resolve/{rev}/{name}") as r, open(dest, "wb") as f:
        shutil.copyfileobj(r, f)
    data = dest.read_bytes()
    sha256 = hashlib.sha256(data).hexdigest()
    if "lfs" in entry:   # large file: Hugging Face publishes its SHA-256
        if sha256 != entry["lfs"]["oid"]:
            sys.exit(f"{name}: SHA-256 differs from Hugging Face's ({sha256} vs {entry['lfs']['oid']})")
    else:                # small file: Hugging Face publishes the git blob id
        blob = hashlib.sha1(b"blob %d\0" % len(data) + data).hexdigest()
        if blob != entry["oid"]:
            sys.exit(f"{name}: git blob id differs from Hugging Face's")
    if lock and lock["files"].get(name) != sha256:
        sys.exit(f"{name}: differs from the pinned scripts/model.lock")
    found[name] = sha256
    print(f"  {name}: {len(data) / 1048576:.1f} MB sha256 {sha256[:16]}...")

if (out / "model.int8.onnx").stat().st_size < 5 * 1048576:
    sys.exit("model.int8.onnx is suspiciously small")
if not lock:
    new_lock = {"repo": repo, "rev": rev, "files": found}
    (cache / "model.lock").write_text(json.dumps(new_lock, indent=2) + "\n")
    print(f"NOTE: model is not pinned yet. Copy {cache}/model.lock to scripts/model.lock and commit it.")
print(f"model {repo}@{rev[:10]} verified")
PY

echo "All assets fetched and verified."
