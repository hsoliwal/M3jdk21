#!/usr/bin/env python3
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""Writes one side of the differential dump driver from M3ObjectSignalDump.java.tmpl (recipe object-signal-20261107).

The template is written in the donor vocabulary. side=donor: the package of the first PACKAGE row of
RELOCATION_MAP.tsv, the progress and codec imports from the SUBSET_ONLY rows (dotted from their donor paths), the
encoder placeholder bound to codecOf and the //@DONOR_BEGIN..//@DONOR_END block kept (markers removed). side=relocated:
the target package, no import, the encoder placeholder empty (a parenthesized expression), the donor block removed and
every TYPE row applied as a whole word, the rename relocate.py applies. Donor package names are read from the map, so
this file names none. Output: <out>/<package path>/M3ObjectSignalDump.java; prints its path.

usage: python -I gen_dump.py <RELOCATION_MAP.tsv> <template> donor|relocated <out-dir>
"""
from __future__ import annotations

import re
import sys
from pathlib import Path


def rows(map_path: Path) -> list:
    return [line.split("\t") for line in map_path.read_text(encoding="utf-8").splitlines()
            if line.strip() and not line.startswith("#") and not line.startswith("kind\t")]


def dotted(donor_path: str) -> str:
    return donor_path.split("/src/main/java/", 1)[1][:-len(".java")].replace("/", ".")


def main(argv: list) -> int:
    map_path, template, side, out = Path(argv[1]), Path(argv[2]), argv[3], Path(argv[4])
    table = rows(map_path)
    donor_package, target_package = next((row[1], row[2]) for row in table if row[0] == "PACKAGE")
    closure = [dotted(row[1]) for row in table if row[0] == "SUBSET_ONLY"]
    text = template.read_text(encoding="utf-8")
    block = re.compile(r"^//@DONOR_BEGIN\n(.*?)^//@DONOR_END\n", re.S | re.M)
    if side == "donor":
        package = donor_package
        text = block.sub(lambda match: match.group(1), text)
        text = text.replace("@IMPORTS@", "\n".join("import %s;" % name for name in sorted(closure)))
        text = text.replace("@ENCODER@", "codecOf")
    elif side == "relocated":
        package = target_package
        text = block.sub("", text)
        text = text.replace("@IMPORTS@\n", "").replace("@ENCODER@", "")
        for row in table:
            if row[0] == "TYPE" and row[1] != row[2]:
                text = re.sub(r"\b%s\b" % re.escape(row[1]), row[2], text)
    else:
        raise SystemExit("side must be donor or relocated")
    text = text.replace("@PACKAGE@", package)
    if "@" + "PACKAGE@" in text or "@ENCODER@" in text or "@IMPORTS@" in text:
        raise SystemExit("unbound placeholder")
    target = out / package.replace(".", "/") / "M3ObjectSignalDump.java"
    target.parent.mkdir(parents=True, exist_ok=True)
    target.write_bytes(text.encode("utf-8"))
    print(target)
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv))
