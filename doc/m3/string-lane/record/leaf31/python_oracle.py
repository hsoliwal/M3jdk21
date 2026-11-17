# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
# Copyright (c) 2026 Synexia, Hitesh Soliwal and others
"""python_oracle.py <work-dir>: CPython 3.12 (Unicode 15.0.0) as the oracle of caseFold and equalsFoldCase.
Texts are UTF-16 code units in hex (big endian), one per line, as CaseFamilyProof corpus writes them; a text becomes a
Python str with surrogatepass (a pair is one code point, a lone surrogate stays one) and back. Writes:
CASEFOLD_POINTS.txt  every code point 0..10FFFF whose chr(cp).casefold() differs from it: hex cp TAB hex units
CASEFOLD_TEXTS.txt   the casefold of each line of fold_texts.txt, hex units
FOLD_EQUAL.bin       for every ordered pair (a, b) of pair_texts.txt: '1' if a.casefold() == b.casefold() else '0'
FOLD_EQUAL_EXTRA.bin the same for each line 'a TAB b' of pair_extra.txt"""
import os
import sys
import unicodedata


def dec(h):
    return bytes.fromhex(h).decode("utf-16-be", "surrogatepass")


def enc(s):
    return s.encode("utf-16-be", "surrogatepass").hex().upper()


def lines(path):
    return open(path, encoding="ascii", newline="").read().split("\n")[:-1]


def main():
    work = sys.argv[1]
    changed = 0
    with open(os.path.join(work, "CASEFOLD_POINTS.txt"), "w", encoding="ascii", newline="") as w:
        for cp in range(0x110000):
            folded = chr(cp).casefold()
            if folded != chr(cp):
                w.write("%X\t%s\n" % (cp, enc(folded)))
                changed += 1
    texts = lines(os.path.join(work, "fold_texts.txt"))
    with open(os.path.join(work, "CASEFOLD_TEXTS.txt"), "w", encoding="ascii", newline="") as w:
        w.write("".join(enc(dec(h).casefold()) + "\n" for h in texts))
    folds = [dec(h).casefold() for h in lines(os.path.join(work, "pair_texts.txt"))]
    equal = bytearray()
    for a in folds:
        equal.extend(49 if a == b else 48 for b in folds)
    open(os.path.join(work, "FOLD_EQUAL.bin"), "wb").write(equal)
    extra = bytearray()
    for line in lines(os.path.join(work, "pair_extra.txt")):
        a, b = line.split("\t")
        extra.append(49 if dec(a).casefold() == dec(b).casefold() else 48)
    open(os.path.join(work, "FOLD_EQUAL_EXTRA.bin"), "wb").write(extra)
    print("python", sys.version.split()[0], "unicode", unicodedata.unidata_version, "folding_points", changed,
          "texts", len(texts), "pairs", len(equal), "extra_pairs", len(extra))


main()
