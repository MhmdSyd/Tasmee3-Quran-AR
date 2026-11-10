#!/usr/bin/env python3
"""Convert zonetecde/mushaf-layout page files into compact per-page assets for the app.

Usage: build_layout.py <source dir with page-001.json ...> <output dir>

Output: p001.json ... p604.json and meta.json. No Quran text is typed here; everything comes from the source.
Per page: {"p":page,"s":first surah,"j":juz,"l":[line,...]}
  header line  {"n":1,"t":"h","x":"<title>","s":<surah>}
  basmala line {"n":2,"t":"b"}
  text line    {"n":3,"t":"w","v":[[location,text,verseEndNumber(0 if none)],...]}
  A token with text "" is a verse-end mark that wrapped onto this line (not a spoken word).
"""
import json
import pathlib
import re
import sys

# Standard juz starts as (surah, ayah). Page numbers are derived from the data, not typed.
JUZ_STARTS = [(1, 1), (2, 142), (2, 253), (3, 93), (4, 24), (4, 148), (5, 82), (6, 111), (7, 88), (8, 41),
              (9, 93), (11, 6), (12, 53), (15, 1), (17, 1), (18, 75), (21, 1), (23, 1), (25, 21), (27, 56),
              (29, 46), (33, 31), (36, 28), (39, 32), (41, 47), (46, 1), (51, 31), (58, 1), (67, 1), (78, 1)]
EXPECTED_JUZ_PAGES = [1, 22, 42, 62, 82, 102, 121, 142, 162, 182, 201, 222, 242, 262, 282, 302, 322, 342,
                      362, 382, 402, 422, 442, 462, 482, 502, 522, 542, 562, 582]
VERSE_END = re.compile(r"^(.*?)\s*([\u0660-\u0669]+)$")


def juz_of(surah, ayah):
    return sum(1 for s in JUZ_STARTS if s <= (surah, ayah))


def to_int(arabic_digits):
    return int("".join(str(ord(c) - 0x660) for c in arabic_digits))


def dump(path, obj):
    path.write_text(json.dumps(obj, ensure_ascii=False, separators=(",", ":")), encoding="utf-8")


def main(src, out):
    src, out = pathlib.Path(src), pathlib.Path(out)
    out.mkdir(parents=True, exist_ok=True)
    surah_names, basmala = {}, None
    verse_ends = words_total = lines_total = 0
    max_words = (0, 0, 0)
    page_of_verse = {}
    for p in range(1, 605):
        d = json.loads((src / f"page-{p:03d}.json").read_text(encoding="utf-8"))
        assert d["page"] == p, f"page number mismatch in {p}"
        lines, first_loc = [], None
        for expected_n, l in enumerate(d["lines"], start=1):
            assert l["line"] == expected_n, f"page {p}: line numbers not contiguous"
            t = l["type"]
            if t == "surah-header":
                s = int(l["surah"])
                surah_names[s] = l["text"]
                lines.append({"n": expected_n, "t": "h", "x": l["text"], "s": s})
            elif t == "basmala":
                lines.append({"n": expected_n, "t": "b"})
            elif t == "text":
                items = []
                for w in l["words"]:
                    m = VERSE_END.match(w["word"])
                    text, end = (m.group(1), to_int(m.group(2))) if m else (w["word"], 0)
                    # A verse-end number can wrap to the start of the next line as its own token:
                    # then text is "" and end > 0 (a mark-only item). Matchers must skip empty text.
                    assert text or end, f"empty token at {w['location']}"
                    items.append([w["location"], text, end])
                    verse_ends += 1 if end else 0
                    s, a, wi = (int(x) for x in w["location"].split(":"))
                    page_of_verse.setdefault((s, a), p)
                    first_loc = first_loc or w["location"]
                words_total += len(items)
                lines.append({"n": expected_n, "t": "w", "v": items})
                max_words = max(max_words, (len(items), p, expected_n))
                if p == 1 and expected_n == 2:
                    basmala = " ".join(i[1] for i in items)
            else:
                raise SystemExit(f"unknown line type {t!r} on page {p}")
        lines_total += len(lines)
        s0, a0, _ = (int(x) for x in first_loc.split(":"))
        dump(out / f"p{p:03d}.json", {"p": p, "s": s0, "j": juz_of(s0, a0), "l": lines})
    assert len(surah_names) == 114, f"expected 114 surah names, got {len(surah_names)}"
    assert verse_ends == 6236, f"expected 6236 verse ends, got {verse_ends}"
    assert basmala, "basmala text not found"
    dump(out / "meta.json", {"basmala": basmala, "surahs": {str(k): v for k, v in sorted(surah_names.items())}})
    juz_pages = [page_of_verse[s] for s in JUZ_STARTS]
    if juz_pages != EXPECTED_JUZ_PAGES:
        print("WARNING: juz start pages differ from the common table:", juz_pages)
    print(f"OK pages=604 lines={lines_total} words={words_total} verses={verse_ends} "
          f"maxWordsInLine={max_words[0]} (page {max_words[1]} line {max_words[2]})")


if __name__ == "__main__":
    if len(sys.argv) != 3:
        raise SystemExit(__doc__)
    main(sys.argv[1], sys.argv[2])
