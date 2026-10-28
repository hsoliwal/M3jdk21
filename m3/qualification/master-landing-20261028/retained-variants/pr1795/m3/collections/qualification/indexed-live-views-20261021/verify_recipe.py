#!/usr/bin/env python3
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""Read-only exact-source verification; not an OpenRewrite execution proof."""
import hashlib
from pathlib import Path

ROOT = Path(__file__).resolve().parents[4]
CRATE = ROOT / "m3/tooling/migration-recipes/src/main/resources/com/synexia/rewrite/hash-pinned-java/m3-indexed-live-views-20261021"
PRE = Path(__file__).resolve().parent / "pre"
SUCCESSIONS = Path(__file__).resolve().parent / "RECEIVER_SUCCESSORS.tsv"


def digest(data: bytes) -> str:
    return hashlib.sha256(data).hexdigest()


def inside_repo(path: Path) -> Path:
    resolved = path.resolve()
    if not resolved.is_relative_to(ROOT) or path.is_symlink():
        raise RuntimeError(f"unsafe source path: {path}")
    return path


def main() -> None:
    rows = [row.split("\t") for row in (CRATE / "manifest.tsv")
            .read_text(encoding="utf-8").splitlines()
            if row.strip() and not row.startswith("#")]
    if len(rows) != 6 or any(len(row) != 4 for row in rows):
        raise RuntimeError("six exact recipe rows required")
    if [row[0] for row in rows] != sorted({row[0] for row in rows}):
        raise RuntimeError("manifest paths must be strictly increasing")
    # Retain the original historical recipe and accept only source-qualified,
    # exact-byte successors. The successor ledger is itself a reviewed recipe
    # target, not a permissive pattern or an unbounded list of exceptions.
    admitted_paths = {row[0] for row in rows}
    successors = {}
    for line in SUCCESSIONS.read_text(encoding="utf-8").splitlines():
        if not line.strip() or line.startswith("#"):
            continue
        fields = line.split("\t")
        if len(fields) != 4:
            raise RuntimeError("invalid successor ledger entry")
        path, original, current, provenance = fields
        if path not in admitted_paths or path in successors:
            raise RuntimeError(f"unreviewed successor target: {path}")
        expected = next(row[2] for row in rows if row[0] == path)
        if original != expected or len(current) != 64 or any(
                ch not in "0123456789abcdef" for ch in current):
            raise RuntimeError(f"unqualified successor source hash: {path}")
        if not provenance.startswith("https://github.com/hsoliwal/"):
            raise RuntimeError(f"missing original-owner provenance: {path}")
        successors[path] = current
    for path, before, after, template in rows:
        if not path.startswith("m3/collections/src/") or not template.endswith(".java.txt"):
            raise RuntimeError(f"invalid receiver path: {path}")
        recipe_file = inside_repo(CRATE / template)
        if not recipe_file.is_file() or digest(recipe_file.read_bytes()) != after:
            raise RuntimeError(f"recipe template drift: {template}")
        receiver = inside_repo(ROOT / path)
        if not receiver.is_file():
            raise RuntimeError(f"receiver missing: {path}")
        actual = digest(receiver.read_bytes())
        if actual != after and actual != successors.get(path):
            raise RuntimeError(f"receiver != recipe or qualified successor: {path}")
        if actual != after:
            print(f"SUCCESSOR_PIN_PASS {path} original={after} current={actual}")
        if before != "ABSENT":
            prior = inside_repo(PRE / (Path(path).name + ".txt"))
            if not prior.is_file() or digest(prior.read_bytes()) != before:
                raise RuntimeError(f"source preimage drift: {path}")
        print(f"PIN_PASS {path} before={before} after={after}")
    print(f"M3_INDEXED_LIVE_RECIPE_PIN_PASS files={len(rows)}")


if __name__ == "__main__":
    main()
