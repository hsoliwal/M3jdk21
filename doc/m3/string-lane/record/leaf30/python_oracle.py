# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
# Copyright (c) 2026 Synexia, Hitesh Soliwal and others
"""python_oracle.py <out-dir>: CPython 3.12 as the oracle of String.translate(Map<Integer, String>).
Builds the same text corpus as ReplaceFamilyProof (every string of 0..4 units over UNITS, then the window-seam texts,
in the same order), turns each UTF-16 unit sequence into a Python str (surrogatepass: a pair becomes one code point, a
lone surrogate stays) and writes TRANSLATE.bin: int32 text count, int32 table count, the corpus, then for each table of
TABLES (same order as the proof) the answer of str.translate for every text. Each string is an int32 unit count and its
UTF-16LE units (surrogatepass), all little-endian."""
import os
import struct
import sys

HIGH, LOW = 0xD83D, 0xDE00
UNITS = [ord("a"), ord("b"), ord("A"), ord(","), HIGH, LOW, ord("x")]
TABLES = [
    {ord("a"): "xy", ord("b"): None, ord(","): ""},
    {0x1F600: "P", HIGH: "h", ord("A"): "A"},
    {ord("x"): chr(HIGH), ord("a"): chr(LOW)},
    {},
    {ord("a"): "a", ord("b"): "b"},
    {LOW: None, ord("x"): "xx", HIGH: chr(HIGH) + chr(LOW)},
]


def text(units):
    return b"".join(u.to_bytes(2, "little") for u in units).decode("utf-16-le", "surrogatepass")


def corpus():
    out = []
    for length in range(5):
        for code in range(len(UNITS) ** length):
            units, rest = [], code
            for _ in range(length):
                units.append(UNITS[rest % len(UNITS)])
                rest //= len(UNITS)
            out.append(text(units))
    k = 505
    while k <= 1030:
        for tail in (text([HIGH, LOW]), ",b", "aA", ""):
            out.append("x" * k + tail)
            out.append(tail + "x" * k)
        k += 498 if k == 520 else 1
    return out


def record(s):
    data = s.encode("utf-16-le", "surrogatepass")
    return struct.pack("<i", len(data) // 2) + data


def main():
    texts = corpus()
    parts = [struct.pack("<ii", len(texts), len(TABLES))]
    parts += [record(t) for t in texts]
    for table in TABLES:
        parts += [record(t.translate(table)) for t in texts]
    open(os.path.join(sys.argv[1], "TRANSLATE.bin"), "wb").write(b"".join(parts))
    print("python", sys.version.split()[0], "texts", len(texts), "tables", len(TABLES))


main()
