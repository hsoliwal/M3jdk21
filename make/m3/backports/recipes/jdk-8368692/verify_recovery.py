#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0
from __future__ import annotations

import csv
import hashlib
from pathlib import Path

UPSTREAM = "9131c72d63cac7d2a0e845952cee0e3c7edbfc93"

def rows(path: Path):
    with path.open(encoding="utf-8", newline="") as stream:
        return list(csv.DictReader(stream, delimiter="\t"))

def main() -> int:
    root = Path(__file__).resolve().parents[4]
    adaptation = rows(root / "m3/backports/recipes/jdk-8368692/adaptation.tsv")
    if len(adaptation) != 3:
        raise SystemExit(f"expected 3 adaptation rows, found {len(adaptation)}")
    for row in adaptation:
        if row["upstream_commit"] != UPSTREAM:
            raise SystemExit(f"donor drift: {row['target_path']}")
        path = root / row["target_path"]
        if not path.is_file():
            raise SystemExit(f"missing target: {row['target_path']}")
        actual = hashlib.sha256(path.read_bytes()).hexdigest()
        if actual != row["target_sha256"]:
            raise SystemExit(f"target drift: {row['target_path']}: {actual}")
    seeds = rows(root / "m3/backports/UPSTREAM_CHANGE_SEEDS.tsv")
    seed = next((r for r in seeds if r["jbs"] == "JDK-8368692"), None)
    if seed is None or seed["upstream_commit"] != UPSTREAM:
        raise SystemExit("JDK-8368692 seed lineage missing")
    password = (root / "src/java.base/share/classes/sun/security/util/Password.java").read_text(encoding="utf-8")
    security = (root / "src/java.base/share/conf/security/java.security").read_text(encoding="utf-8")
    test = (root / "test/jdk/sun/security/tools/keytool/AllowSystemIn.java").read_text(encoding="utf-8")
    if "jdk.security.password.allowSystemIn" not in password:
        raise SystemExit("Password policy property missing")
    if "privilegedGetOverridable" not in password:
        raise SystemExit("JDK21 privileged security-property access adaptation missing")
    if "jdk.security.password.allowSystemIn=true" not in security:
        raise SystemExit("compatibility-preserving true default missing")
    for token in ("default", "true", "false", "invalid"):
        if token not in test:
            raise SystemExit(f"focused jtreg mode missing: {token}")
    print("PASS: JDK-8368692 current-tree recovery verifier")
    return 0

if __name__ == "__main__":
    raise SystemExit(main())
