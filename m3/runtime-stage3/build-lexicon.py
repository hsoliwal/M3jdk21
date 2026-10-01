#!/usr/bin/env python3
# Copyright 2026 Hitesh Soliwal <hsoliwal@gmail.com>
# SPDX-License-Identifier: Apache-2.0
"""Build the exact read-only UTF-16LE MIndex lexicon fixture used by stage-3 tests."""
from __future__ import annotations

import hashlib
import pathlib
import struct
import sys

MAGIC = 0x4D334C4558303031
HEADER = 64

def main() -> int:
    if len(sys.argv) != 2:
        raise SystemExit("usage: build-lexicon.py OUTPUT")
    output = pathlib.Path(sys.argv[1])
    words = ["", " ", "alpha", "beta", "join", "string", "word"]
    if words != sorted(words):
        raise AssertionError("fixture must be UTF-16 lexical order")

    encoded = [word.encode("utf-16le", "surrogatepass") for word in words]
    units = sum(len(value) // 2 for value in encoded)
    payload = HEADER + 8 * len(words)
    image = bytearray(payload + 2 * units)

    struct.pack_into(">QIIQQ", image, 0, MAGIC, 1, len(words), payload, units)
    cursor = 0
    for row, value in enumerate(encoded):
        struct.pack_into(">II", image, HEADER + 8 * row, cursor, len(value) // 2)
        begin = payload + 2 * cursor
        image[begin:begin + len(value)] = value
        cursor += len(value) // 2

    digest = hashlib.sha256(image[:32] + image[HEADER:]).digest()
    image[32:64] = digest
    output.parent.mkdir(parents=True, exist_ok=True)
    output.write_bytes(image)
    print(f"M3_M_INDEX_LEXICON_BUILT path={output} records={len(words)} bytes={len(image)}")
    return 0

if __name__ == "__main__":
    raise SystemExit(main())
