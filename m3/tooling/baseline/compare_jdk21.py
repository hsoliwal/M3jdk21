#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0
"""Verbatim OpenJDK 21 baseline comparator for M3JDK21."""

from __future__ import annotations

import argparse
import hashlib
from pathlib import Path


BASELINE_COMMIT = "890adb6410dab4606a4f26a942aed02fb2f55387"
DEFAULT_EXCLUDES = (".git/", "m3/")


def sha256(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as handle:
        for chunk in iter(lambda: handle.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest()


def inventory(root: Path, excludes: tuple[str, ...] = DEFAULT_EXCLUDES) -> dict[str, Path]:
    result: dict[str, Path] = {}
    for path in sorted(p for p in root.rglob("*") if p.is_file()):
        rel = path.relative_to(root).as_posix()
        if any(rel == prefix.rstrip("/") or rel.startswith(prefix) for prefix in excludes):
            continue
        result[rel] = path
    return result


def compare(baseline: Path, current: Path) -> list[tuple[str, str, str, str, str]]:
    left = inventory(baseline)
    right = inventory(current)
    rows: list[tuple[str, str, str, str, str]] = []
    for rel in sorted(set(left) | set(right)):
        base_path = left.get(rel)
        current_path = right.get(rel)
        if base_path is None:
            rows.append((rel, "", sha256(current_path), "ADDED", "false"))
            continue
        if current_path is None:
            rows.append((rel, sha256(base_path), "", "DELETED", "false"))
            continue
        base_hash = sha256(base_path)
        current_hash = sha256(current_path)
        equal = base_hash == current_hash and base_path.read_bytes() == current_path.read_bytes()
        rows.append(
            (rel, base_hash, current_hash, "VERBATIM_EQUAL" if equal else "MODIFIED", str(equal).lower())
        )
    return rows


def write_ledger(rows: list[tuple[str, str, str, str, str]], output: Path) -> None:
    output.parent.mkdir(parents=True, exist_ok=True)
    with output.open("w", encoding="utf-8", newline="") as handle:
        handle.write("path\tbaseline_sha256\tcurrent_sha256\tstatus\tverbatim_equal\n")
        for row in rows:
            handle.write("\t".join(row) + "\n")


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--baseline", type=Path, required=True)
    parser.add_argument("--current", type=Path, default=Path("."))
    parser.add_argument("--output", type=Path, default=Path("m3/evidence/jdk21-verbatim.tsv"))
    args = parser.parse_args()

    rows = compare(args.baseline.resolve(), args.current.resolve())
    write_ledger(rows, args.output)
    counts: dict[str, int] = {}
    for _, _, _, status, _ in rows:
        counts[status] = counts.get(status, 0) + 1
    print(f"baseline_commit={BASELINE_COMMIT}")
    for status in sorted(counts):
        print(f"{status}={counts[status]}")
    print(f"ledger={args.output}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
