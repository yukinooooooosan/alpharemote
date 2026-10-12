#!/usr/bin/env python3
"""Subset Mochiy Pop One (SIL OFL 1.1) to the glyphs the kawaii screen uses.

Usage: python3 tools/subset_kawaii_font.py path/to/MochiyPopOne-Regular.ttf
Source: https://github.com/google/fonts/tree/main/ofl/mochiypopone (no Reserved Font Name).
Re-run after adding kawaii copy with new kanji; characters outside the subset fall back to the system font.
Requires: pip install fonttools
"""
import pathlib
import re
import sys

from fontTools import subset

ROOT = pathlib.Path(__file__).resolve().parent.parent
OUT = ROOT / "app/src/main/res/font/mochiy_pop_one.ttf"
SOURCES = [ROOT / "app/src/main/res/values/kawaii.xml"]

base = set(chr(c) for c in range(0x20, 0x7F))            # ASCII
base |= set(chr(c) for c in range(0x3041, 0x3097))       # hiragana
base |= set(chr(c) for c in range(0x30A1, 0x30FD))       # katakana incl. ー
base |= set(chr(c) for c in range(0xFF01, 0xFF5F))       # full-width ASCII
base |= set("、。・「」『』〜…♡★☆！？　")
for source in SOURCES:
    text = re.sub(r"<[^>]+>", "", source.read_text(encoding="utf-8"))
    base |= set(text.replace("\\n", ""))
chars = "".join(sorted(c for c in base if c.isprintable() or c == "　"))

options = subset.Options()
options.layout_features = ["*"]
options.name_IDs = ["*"]
options.name_languages = ["*"]
options.notdef_outline = True
options.drop_tables += ["DSIG"]
font = subset.load_font(sys.argv[1], options)
subsetter = subset.Subsetter(options)
subsetter.populate(text=chars)
subsetter.subset(font)
OUT.parent.mkdir(parents=True, exist_ok=True)
subset.save_font(font, str(OUT), options)
print(f"{len(chars)} characters -> {OUT} ({OUT.stat().st_size} bytes)")
