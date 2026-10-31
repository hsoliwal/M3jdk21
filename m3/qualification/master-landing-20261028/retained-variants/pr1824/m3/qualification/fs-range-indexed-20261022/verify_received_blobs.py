#!/usr/bin/env python3
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""Read-only exact Git blob and file-verifier admission check."""
from __future__ import annotations

import hashlib
from pathlib import Path
import re
import sys

ROOT = Path(__file__).resolve().parents[3]
HERE = Path(__file__).resolve().parent


def blob(raw: bytes) -> str:
    return hashlib.sha1(b"blob " + str(len(raw)).encode("ascii") + b"\x00" + raw).hexdigest()


def main() -> None:
    names = []
    for line in (HERE / "RECEIVED_DONORS.tsv").read_text(encoding="utf-8").splitlines():
        if not line or line.startswith("#"):
            continue
        fields = line.split("\t")
        if len(fields) != 6:
            raise ValueError("expected exactly six source receipt columns")
        name, before, after, donor, recipe, category = fields
        if (name.startswith("/") or "\\" in name
                or any(part in ("", ".", "..") for part in name.split("/"))
                or not re.fullmatch(r"[a-f0-9]{40}", after)
                or before != "ABSENT" and not re.fullmatch(r"[a-f0-9]{40}", before)
                or not donor.isdecimal() or not recipe.isdecimal() or not category):
            raise ValueError("unsafe source receipt: " + name)
        path = ROOT / name
        if path.is_symlink() or not path.is_file() or blob(path.read_bytes()) != after:
            raise ValueError("source postimage mismatch: " + name)
        names.append(name)
    if len(names) != 7 or len(set(names)) != 7 or names != sorted(names):
        raise ValueError("incomplete or unordered source manifest")

    import ast
    verifier = (ROOT / "m3/file/verify.py").read_text(encoding="utf-8")
    module_tests = (ROOT / "m3/file/src/test/java/com/m3/file")
    candidates = sorted(p.stem for p in module_tests.glob("*Contract.java"))
    start = verifier.index("        contracts = {")
    end = verifier.index("        if {path.stem", start)
    expr = verifier[start:end].split("=", 1)[1]
    keys = sorted(ast.literal_eval(expr).keys())
    if keys != candidates:
        raise ValueError("strict host/linked module contract names differ")
    if len(keys) != 13:
        raise ValueError("expected 13 source contract entry points")
    print("M3FS_RANGE_INDEXED_RECEIVER_PASS files=7 module_contracts=13")


if __name__ == "__main__":
    try:
        main()
    except (OSError, ValueError, SyntaxError) as error:
        print("M3FS_RANGE_INDEXED_RECEIVER_REFUSED " + str(error), file=sys.stderr)
        sys.exit(1)
