#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0
"""Seal m3/ports/compiletime Java sources into the hash-pinned recipe crate.

Deterministic: targets are every *.java under m3/ports/compiletime/src in byte order of their
repository path; each becomes an ABSENT -> SHA-256 row and an identical NN-<File>.java.txt
resource. Re-running on unchanged sources is a byte-identical fixed point.
"""
from __future__ import annotations

import hashlib
from pathlib import Path

ROOT = Path(__file__).resolve().parents[3]
PORT = ROOT / "m3/ports/compiletime"
CRATE_NAME = "m3-compiletime-regex-precompute"
CRATE = ROOT / "m3/tooling/migration-recipes/src/main/resources/com/m3/rewrite/backport/jdk21-hash-pinned" / CRATE_NAME


def targets() -> list[Path]:
    paths = sorted(PORT.joinpath("src").rglob("*.java"), key=lambda p: p.relative_to(ROOT).as_posix())
    if not paths or len(paths) > 99:
        raise SystemExit(f"crate target budget: {len(paths)}")
    return paths


def main() -> None:
    CRATE.mkdir(parents=True, exist_ok=True)
    for stale in CRATE.glob("*.java.txt"):
        stale.unlink()
    rows = ["# path\tbefore_sha256\tafter_sha256\tresource"]
    for index, path in enumerate(targets()):
        data = path.read_bytes()
        if b"\r" in data:
            raise SystemExit(f"CR byte in {path}")
        resource = f"{index:02d}-{path.name}.txt"
        (CRATE / resource).write_bytes(data)
        rows.append("\t".join([path.relative_to(ROOT).as_posix(), "ABSENT",
                               hashlib.sha256(data).hexdigest(), resource]))
    (CRATE / "manifest.tsv").write_text("\n".join(rows) + "\n", encoding="utf-8", newline="\n")
    print(f"sealed {len(rows) - 1} targets into {CRATE.relative_to(ROOT).as_posix()}")


if __name__ == "__main__":
    main()
