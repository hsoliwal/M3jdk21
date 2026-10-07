#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0
from __future__ import annotations

import csv
import hashlib
from pathlib import Path

UPSTREAM = "39de79eae23410e335d2d1ced8fe3b4d7937a541"
FORBIDDEN = ("redact-argument", "redact-key", "JfrRedactedEvents")

def rows(path: Path):
    with path.open(encoding="utf-8", newline="") as stream:
        return list(csv.DictReader(stream, delimiter="\t"))

def main() -> int:
    root = Path(__file__).resolve().parents[4]
    adaptation = rows(root / "m3/backports/recipes/jdk-8367584/adaptation.tsv")
    if len(adaptation) != 4:
        raise SystemExit(f"expected 4 adaptation rows, found {len(adaptation)}")
    for row in adaptation:
        if row["upstream_commit"] != UPSTREAM:
            raise SystemExit(f"donor drift: {row['target_path']}")
        path = root / row["target_path"]
        if not path.is_file():
            raise SystemExit(f"missing target: {row['target_path']}")
        actual = hashlib.sha256(path.read_bytes()).hexdigest()
        if actual != row["target_sha256"]:
            raise SystemExit(f"target drift: {row['target_path']}: {actual}")
        body = path.read_text(encoding="utf-8")
        for token in FORBIDDEN:
            if token in body:
                raise SystemExit(f"forbidden JEP 536 redaction token in {row['target_path']}: {token}")

    seeds = rows(root / "m3/backports/UPSTREAM_CHANGE_SEEDS.tsv")
    seed = next((r for r in seeds if r["jbs"] == "JDK-8367584"), None)
    if seed is None or seed["upstream_commit"] != UPSTREAM:
        raise SystemExit("JDK-8367584 seed lineage missing")
    if seed["disposition"] != "candidate-adapted":
        raise SystemExit("JDK-8367584 must remain candidate-adapted")

    dcmd = (root / "src/hotspot/share/jfr/dcmd/jfrDcmds.cpp").read_text(encoding="utf-8")
    option_set = (root / "src/hotspot/share/jfr/recorder/service/jfrOptionSet.cpp").read_text(encoding="utf-8")
    test = (root / "test/jdk/jdk/jfr/startupargs/TestOptionsHelp.java").read_text(encoding="utf-8")
    for token in (
        "Syntax : -XX:FlightRecorderOptions:[options]",
        "Multiple options are separated",
    ):
        if token not in dcmd and token not in test:
            raise SystemExit(f"help contract missing: {token}")
    if 'strcmp(FlightRecorderOptions, "help") == 0' not in option_set:
        raise SystemExit("startup help dispatch missing")
    print("PASS: JDK-8367584 current-tree help-leaf recovery verifier")
    return 0

if __name__ == "__main__":
    raise SystemExit(main())
