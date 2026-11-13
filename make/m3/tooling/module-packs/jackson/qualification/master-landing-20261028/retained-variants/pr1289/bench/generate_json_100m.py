#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0
# Copyright 2026 Hitesh Soliwal and contributors
"""Generate exact-size UTF-8 JSON for the opt-in M3AST 100 MiB acceptance lane.

The array is real structured JSON (not whitespace padding): many ordered objects,
duplicate fields, Unicode, escaping, nested arrays, negative and exponent numbers,
booleans, nulls, and a final scalar payload. Never overwrite existing files.

Usage: python3 generate_json_100m.py /scratch/m3-100m.json [mib]
The default is 100 MiB = 104,857,600 bytes (more than 100 decimal MB).
"""
import hashlib
import sys
from pathlib import Path


def generate(path: Path, mib: int) -> None:
    if mib < 1 or mib > 1024:
        raise ValueError("MiB must be between 1 and 1024")
    target = mib * 1024 * 1024
    path.parent.mkdir(parents=True, exist_ok=True)
    digest = hashlib.sha256()
    total = 0
    # Explicit escaped control characters and backslash, never raw control bytes.
    trailing_fields = (
        ',"dup":"first","dup":"second","text":"Café🚀\\nA\\\\B\\u0000",'
        '"n":[0,-2,1.25,3e5,null,true,false],"obj":{"k":"é","items":[1,2,3]}}'
    ).encode("utf-8")

    with path.open("xb") as output:
        def emit(data: bytes) -> None:
            nonlocal total
            output.write(data)
            digest.update(data)
            total += len(data)

        emit(b"[")
        rows = 0
        while True:
            row = b'{"id":' + str(rows).encode("ascii") + trailing_fields
            comma = b"," if rows else b""
            # Leave room for one fully valid final JSON object with a nonempty payload.
            if total + len(comma) + len(row) + 2048 >= target:
                break
            emit(comma)
            emit(row)
            rows += 1

        head = b',{"tail":"' if rows else b'{"tail":"'
        foot = b'"}]'
        padding = target - total - len(head) - len(foot)
        if padding < 1:
            raise AssertionError("fixture accounting exceeded target")
        emit(head)
        emit(b"x" * padding)
        emit(foot)

    if total != target or path.stat().st_size != target:
        raise AssertionError("fixture length differs from exact target")
    print("M3_JSON_FIXTURE_OK", "bytes=" + str(total),
          "rows=" + str(rows), "sha256=" + digest.hexdigest())


if __name__ == "__main__":
    if len(sys.argv) not in (2, 3):
        raise SystemExit("usage: generate_json_100m.py OUTPUT.json [MiB]")
    generate(Path(sys.argv[1]), int(sys.argv[2]) if len(sys.argv) == 3 else 100)
