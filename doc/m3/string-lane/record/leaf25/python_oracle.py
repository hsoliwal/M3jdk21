# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
# Copyright (c) 2026 Synexia, Hitesh Soliwal and others
"""python_oracle.py <out-dir>: CPython 3.12 as the oracle of compareCodePoints and canonicalEquals.
Builds the same text corpus as CompareFamilyProof (every string of 0..3 units over UNITS, in the same order), turns
each UTF-16 unit sequence into a Python str (surrogatepass: a pair becomes one code point, a lone surrogate stays) and
writes, for every ordered pair (a, b), one byte: CODEPOINT_ORDER.bin '<' '=' '>' by Python string comparison (code
point order) and NFC_EQUAL.bin '1' or '0' for unicodedata.normalize('NFC', a) == normalize('NFC', b)."""
import itertools
import os
import sys
import unicodedata

UNITS = [ord("a"), ord("A"), ord("e"), 0x0301, 0xE9, 0xDF, 0x1E9E, 0x212A, ord("k"), 0xD83D, 0xDE00, 0xDE01]


def texts():
    out = []
    for length in range(4):
        total = len(UNITS) ** length
        for code in range(total):
            units, rest = [], code
            for _ in range(length):
                units.append(UNITS[rest % len(UNITS)])
                rest //= len(UNITS)
            out.append(b"".join(u.to_bytes(2, "little") for u in units).decode("utf-16-le", "surrogatepass"))
    return out


def main():
    out = sys.argv[1]
    corpus = texts()
    nfc = [unicodedata.normalize("NFC", t) for t in corpus]
    order = bytearray()
    equal = bytearray()
    for i, a in enumerate(corpus):
        for j, b in enumerate(corpus):
            order.append(60 if a < b else 61 if a == b else 62)
            equal.append(49 if nfc[i] == nfc[j] else 48)
    open(os.path.join(out, "CODEPOINT_ORDER.bin"), "wb").write(order)
    open(os.path.join(out, "NFC_EQUAL.bin"), "wb").write(equal)
    print("python", sys.version.split()[0], "unicode", unicodedata.unidata_version, "texts", len(corpus),
          "pairs", len(order))


main()
