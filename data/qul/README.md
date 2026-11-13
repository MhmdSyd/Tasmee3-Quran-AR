# Official QUL files (used unmodified)

| File | What it is | From |
|---|---|---|
| `qpc-v1-15-lines.db` | Layout "Quran Complex V1 ( 1405 print )": 604 pages, 15 lines, line type, centered flag, word-id range per line | QUL > Mushaf layouts (Mushaf Al Madinah V1) |
| `qpc-hafs.db` | Uthmani Hafs script, one row per verse (6,236) | QUL > Scripts (QPC Hafs, ayah by ayah) |
| `UthmanicHafs1Ver18.ttf` | KFGQPC Uthmanic Script Hafs font, the one this script is made for | QUL > Fonts (QPC Hafs) |

They were downloaded by the app owner from a QUL account. `tools/build_layout.py` turns the two databases into the
per-page files the app reads; `scripts/fetch_assets.sh` checks their SHA-256 first, so a changed copy fails the build.

Licence: the font carries the KFGQPC end-user licence (`third_party_licenses/KFGQPC_EULA.txt`). QUL says its resources can
be used in commercial projects but each resource has its own licence page, which needs a QUL login to read: check those
pages before a public release and keep the attribution to QUL / Tarteel and KFGQPC.
