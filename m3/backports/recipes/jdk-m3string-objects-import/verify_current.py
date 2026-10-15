#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0
"""Exact source-image verification for the canonical M3 cursor import recipe receiver."""

from __future__ import annotations

import argparse
import hashlib
from pathlib import Path

RELATIVE = Path("src/java.base/share/classes/java/lang/M3StringSearchPrecompute.java")
BEFORE = "4d84b8ff3bb7b0937a9b6451024d3aac0aacfe23549847a15a62b53657354cb5"
AFTER = "edae3c8d3afbd7c1b78aa3f88a4d3fdcd6bd03c9f8ee2ace3044255b44f571ac"
ANCHOR = b"import java.util.Arrays;\n"
ADDITION = b"import java.util.Objects;\n"
IMPORT = ANCHOR + ADDITION


def sha256(data: bytes) -> str:
    return hashlib.sha256(data).hexdigest()


def verify(source: bytes) -> None:
    if sha256(source) != AFTER:
        raise ValueError("reviewed M3StringSearchPrecompute postimage drift")
    if source.count(IMPORT) != 1 or source.count(ADDITION) != 1:
        raise ValueError("missing or duplicated exact Objects import")
    original = source.replace(IMPORT, ANCHOR, 1)
    if sha256(original) != BEFORE:
        raise ValueError("change exceeds the approved Objects import")
    if b"GPLv2 with the Classpath exception." not in original:
        raise ValueError("OpenJDK license boundary unexpectedly changed")
    if source.count(b"Objects.checkIndex") != 1:
        raise ValueError("expected cursor index consumer missing")
    if source.count(b"Objects.checkFromToIndex") != 1:
        raise ValueError("expected cursor range consumer missing")


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--root", type=Path, default=Path(__file__).resolve().parents[4])
    args = parser.parse_args()
    verify((args.root / RELATIVE).read_bytes())
    print("PASS: exact M3StringSearchPrecompute import-only postimage")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
