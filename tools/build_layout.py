#!/usr/bin/env python3
"""Build the app's per-page assets from the official QUL (Tarteel Quran Universal Library) files:

  data/qul/qpc-v1-15-lines.db   layout "Quran Complex V1 (1405 print)": 604 pages, 15 lines
  data/qul/qpc-hafs.db          Uthmani Hafs script, verse by verse (the text the KFGQPC Hafs font is made for)
  data/surah-names-ar.json      the 114 surah names

Usage: build_layout.py <data dir> <output dir>      Output: p001.json ... p604.json and meta.json.

How the files fit together: the layout lists, for every line, a range of WORD IDS. Word ids count every
space-separated word of the verse text in order, the verse-end number as a word of its own, and not the rub-el-hizb
sign. Four verses are segmented differently by the layout (SEGMENTATION_FIXES); the total of 83,668 ids is checked.

Per page: {"p":page,"s":first surah,"j":juz,"l":[line,...]}
  header line  {"n":1,"t":"h","x":"<surah name>","s":<surah>}
  basmala line {"n":2,"t":"b"}
  text line    {"n":3,"t":"w","c":1 (only if centered),"v":[[location,text,verseEndNumber(0 if none)],...]}
  A token with text "" is a verse-end number that starts a line by itself (not a spoken word).
"""
import json
import pathlib
import sqlite3
import sys

JUZ_STARTS = [(1, 1), (2, 142), (2, 253), (3, 93), (4, 24), (4, 148), (5, 82), (6, 111), (7, 88), (8, 41),
              (9, 93), (11, 6), (12, 53), (15, 1), (17, 1), (18, 75), (21, 1), (23, 1), (25, 21), (27, 56),
              (29, 46), (33, 31), (36, 28), (39, 32), (41, 47), (46, 1), (51, 31), (58, 1), (67, 1), (78, 1)]
EXPECTED_JUZ_PAGES = [1, 22, 42, 62, 82, 102, 121, 142, 162, 182, 201, 222, 242, 262, 282, 302, 322, 342,
                      362, 382, 402, 422, 442, 462, 482, 502, 522, 542, 562, 582]

# The layout's word ids segment these verses differently from plain spaces.
#   ("split", verse, token index (0-based), letters kept in the first part)  -> one token becomes two ids
#   ("merge", verse, token index)                                           -> two tokens become one id
SEGMENTATION_FIXES = [
    ("split", (15, 7), 0, 2),    # لَّوۡمَا      -> لَّوۡ + مَا
    ("split", (27, 20), 3, 2),   # مَالِيَ       -> مَا + لِيَ
    ("split", (36, 22), 0, 3),   # وَمَالِيَ     -> وَمَا + لِيَ
    ("merge", (37, 130), 2),     # إِلۡ + يَاسِينَ -> one word with a space
]
RUB_EL_HIZB = "\u06DE"          # ۞ is attached to the word after it and is not a word id of its own


def is_letter(c):
    return "\u0621" <= c <= "\u064A" or c == "\u0671"


def is_number(token):
    return all("\u0660" <= c <= "\u0669" for c in token)


def to_int(arabic_digits):
    return int("".join(str(ord(c) - 0x660) for c in arabic_digits))


def split_after_letters(token, n):
    """Cut [token] after its n-th base letter, keeping the marks that belong to that letter in the first part."""
    seen = 0
    for i, c in enumerate(token):
        if is_letter(c):
            seen += 1
            if seen == n:
                j = i + 1
                while j < len(token) and not is_letter(token[j]):
                    j += 1
                return token[:j], token[j:]
    raise ValueError(f"{token!r} has fewer than {n} letters")


def verse_words(key, text):
    """Words of one verse as the layout counts them, plus the verse number. Returns (words, number)."""
    tokens = text.split()
    number = None
    if tokens and is_number(tokens[-1]):
        number = to_int(tokens.pop())
    words, prefix = [], ""
    for t in tokens:
        if t == RUB_EL_HIZB:
            prefix = RUB_EL_HIZB + " "
        else:
            words.append(prefix + t)
            prefix = ""
    assert not prefix, f"{key}: rub-el-hizb sign without a word after it"
    for fix in SEGMENTATION_FIXES:
        if fix[1] != key:
            continue
        i = fix[2]
        if fix[0] == "split":
            a, b = split_after_letters(words[i], fix[3])
            words[i:i + 1] = [a, b]
        else:
            words[i:i + 2] = [words[i] + " " + words[i + 1]]
    return words, number


def dump(path, obj):
    path.write_text(json.dumps(obj, ensure_ascii=False, separators=(",", ":")), encoding="utf-8")


def main(data, out):
    data, out = pathlib.Path(data), pathlib.Path(out)
    out.mkdir(parents=True, exist_ok=True)
    layout = sqlite3.connect(str(data / "qul" / "qpc-v1-15-lines.db"))
    script = sqlite3.connect(str(data / "qul" / "qpc-hafs.db"))
    names = {int(k): v for k, v in json.loads((data / "surah-names-ar.json").read_text(encoding="utf-8")).items()}
    assert len(names) == 114, "need 114 surah names"
    info = layout.execute("select name, number_of_pages, lines_per_page from info").fetchone()
    assert info[1] == 604 and info[2] == 15 and "1405" in info[0], f"unexpected layout {info}"

    # --- the global id stream: every word and every verse number, in reading order
    stream = []                     # entries: ("w", surah, ayah, index, text) | ("m", surah, ayah, wordCount, number)
    for s, a, text in script.execute("select surah, ayah, text from verses order by id"):
        words, number = verse_words((s, a), text)
        assert number == a, f"{s}:{a} ends with number {number}"
        stream.extend(("w", s, a, i, w) for i, w in enumerate(words, 1))
        stream.append(("m", s, a, len(words), number))
    max_id = layout.execute("select max(last_word_id) from pages where last_word_id != ''").fetchone()[0]
    assert len(stream) == int(max_id), f"{len(stream)} word ids from the text but the layout uses {max_id}"

    basmala = " ".join(e[4] for e in stream[:4])
    assert all(e[0] == "w" and (e[1], e[2]) == (1, 1) for e in stream[:4]), "first four ids must be 1:1 words"

    rows = layout.execute(
        "select page_number, line_number, line_type, is_centered, first_word_id, last_word_id, surah_number "
        "from pages order by page_number, line_number").fetchall()
    by_page = {}
    for r in rows:
        by_page.setdefault(r[0], []).append(r)
    assert sorted(by_page) == list(range(1, 605)), "need pages 1..604"

    used = 0                         # ids must be covered once each, in order, with no gaps
    verse_ends = words_total = lines_total = 0
    page_of_verse = {}
    max_items = (0, 0, 0)
    for p in range(1, 605):
        lines, first_word = [], None
        for n, (_, line_no, kind, centered, f, l, surah) in enumerate(by_page[p], start=1):
            assert line_no == n, f"page {p}: line numbers not contiguous"
            if kind == "surah_name":
                lines.append({"n": n, "t": "h", "x": names[int(surah)], "s": int(surah)})
            elif kind == "basmallah":
                lines.append({"n": n, "t": "b"})
            elif kind == "ayah":
                f, l = int(f), int(l)
                assert f == used + 1, f"page {p} line {n}: ids start at {f}, expected {used + 1}"
                used = l
                items = []
                for e in stream[f - 1:l]:
                    if e[0] == "w":
                        items.append([f"{e[1]}:{e[2]}:{e[3]}", e[4], 0])
                        first_word = first_word or e
                        page_of_verse.setdefault((e[1], e[2]), p)
                        words_total += 1
                    elif items and items[-1][2] == 0 and items[-1][1] != "":
                        items[-1][2] = e[4]          # verse number follows its word on the same line
                        verse_ends += 1
                    else:                            # the number starts the line: a mark of its own
                        items.append([f"{e[1]}:{e[2]}:{e[3] + 1}", "", e[4]])
                        verse_ends += 1
                line = {"n": n, "t": "w"}
                if centered:
                    line["c"] = 1
                line["v"] = items
                lines.append(line)
                max_items = max(max_items, (len(items), p, n))
            else:
                raise SystemExit(f"unknown line type {kind!r} on page {p}")
        lines_total += len(lines)
        s0, a0 = first_word[1], first_word[2]
        juz = sum(1 for st in JUZ_STARTS if st <= (s0, a0))
        dump(out / f"p{p:03d}.json", {"p": p, "s": s0, "j": juz, "l": lines})
    assert used == len(stream), f"layout covers {used} of {len(stream)} ids"
    assert verse_ends == 6236, f"expected 6236 verse numbers, got {verse_ends}"
    dump(out / "meta.json", {"basmala": basmala, "surahs": {str(k): v for k, v in sorted(names.items())}})
    juz_pages = [page_of_verse[s] for s in JUZ_STARTS]
    if juz_pages != EXPECTED_JUZ_PAGES:
        print("WARNING: juz start pages differ from the common table:", juz_pages)
    print(f"OK {info[0]}: pages=604 lines={lines_total} words={words_total} verses={verse_ends} "
          f"maxItemsInLine={max_items[0]} (page {max_items[1]} line {max_items[2]})")


if __name__ == "__main__":
    if len(sys.argv) != 3:
        raise SystemExit(__doc__)
    main(sys.argv[1], sys.argv[2])
