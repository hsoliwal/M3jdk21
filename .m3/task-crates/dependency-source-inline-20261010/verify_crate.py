#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0
"""Verify the vendored third-party tree: every file pinned by a PROVENANCE.json under m3/vendor/third_party*
is byte-identical to its pin and every module pom carries the -m3-source-1 version."""
import csv, hashlib, io, json, sys
from pathlib import Path

HERE = Path(__file__).resolve().parent
ROOT = HERE.parents[2]


def sha256(p):
    h = hashlib.sha256()
    with open(p, "rb") as f:
        for chunk in iter(lambda: f.read(1 << 20), b""):
            h.update(chunk)
    return h.hexdigest()


def main():
    bad = 0
    rows = list(csv.DictReader(io.open(HERE / "THIRD_PARTY_MODULES.tsv", encoding="utf-8", newline=""), delimiter="\t"))
    for r in rows:
        d = ROOT / r["path"]
        prov = json.loads((d / "PROVENANCE.json").read_text(encoding="utf-8"))
        for f in prov["files"]:
            p = ROOT / f["repository"]
            if not p.is_file() or sha256(p) != f["sha256"]:
                print("DRIFT", f["repository"]); bad += 1
        if r["kind"] == "MODULE" and "<version>%s</version>" % r["version"] not in (d / "pom.xml").read_text(encoding="utf-8"):
            print("POM_VERSION", r["path"]); bad += 1
    print("checked", len(rows), "entries", "drift", bad)
    return 1 if bad else 0


if __name__ == "__main__":
    sys.exit(main())
