#!/usr/bin/env python3
# Copyright (c) 2026 Hitesh Soliwal and contributors.
# SPDX-License-Identifier: Apache-2.0
"""Read-only JEP source-blob witnesses for an OpenJDK-derived Java 21 target.

A matching target hash proves only that a current file matches a recorded receipt.
It does NOT prove that an upstream feature, Java 21 compatibility, or a JDK build
has been accepted. JEP implementation evidence remains in the existing DAG.
"""

from __future__ import annotations

import argparse
import csv
import hashlib
import io
import re
import os
import sys
from collections import Counter
from dataclasses import dataclass
from pathlib import Path
from typing import Iterable, Mapping, Sequence

SHA256 = re.compile(r"[0-9a-f]{64}\Z")
BAD_STATES = frozenset({"INVALID_RECEIPT", "SOURCE_DRIFT"})
FIELDS = (
    "release", "jep", "title", "disposition", "packet_count",
    "receipt_count", "product_paths_checked", "product_hashes_matched",
    "witness_state", "java21_compatibility", "promotion",
)


@dataclass(frozen=True)
class Witness:
    release: int
    jep: int
    title: str
    disposition: str
    packet_count: int
    receipt_count: int
    product_paths_checked: int
    product_hashes_matched: int
    witness_state: str
    java21_compatibility: str = "NOT_PROVEN_BY_WITNESS"
    promotion: str = "NOT_AUTHORIZED_BY_WITNESS"


def read_catalogue(path: Path) -> list[dict[str, str]]:
    with path.open(encoding="utf-8", newline="") as stream:
        reader = csv.DictReader(stream, delimiter="\t", strict=True)
        if reader.fieldnames != ["release", "jep", "title", "domain", "disposition", "reason", "superseded_by"]:
            raise ValueError("unexpected JEP catalogue header")
        rows = list(reader)
    if not rows or any(None in row for row in rows):
        raise ValueError("empty or malformed JEP catalogue")
    keys = [(int(row["release"]), int(row["jep"])) for row in rows]
    if keys != sorted(set(keys)):
        raise ValueError("JEP catalogue is unsorted or contains duplicates")
    return rows


def safe_target(root: Path, relative: str) -> Path:
    """Reject traversal and filesystem aliases before reading a claimed product blob."""
    if (not relative or len(relative) > 4096 or relative.startswith(("/", "\\"))
            or ":" in relative or "\\" in relative
            or any(ord(c) < 32 or ord(c) == 127 for c in relative)):
        raise ValueError("noncanonical product path")
    parts = relative.split("/")
    if any(p in {"", ".", "..", ".git"} for p in parts):
        raise ValueError("noncanonical product path")
    path = root
    for component in parts:
        path = path / component
        if path.is_symlink():
            raise ValueError("product path is a symlink or has a symlinked ancestor")
    return path


def read_receipt(path: Path) -> dict[str, str]:
    with path.open(encoding="utf-8", newline="") as stream:
        reader = csv.reader(stream, delimiter="\t", strict=True)
        if next(reader, None) != ["field", "value"]:
            raise ValueError("unexpected receipt header")
        receipt: dict[str, str] = {}
        for row in reader:
            if len(row) != 2 or not row[0] or row[0] in receipt:
                raise ValueError("malformed or duplicate receipt field")
            receipt[row[0]] = row[1]
    if not receipt:
        raise ValueError("empty receipt")
    return receipt


def packet_dirs(root: Path, jep: int) -> list[Path]:
    directory = root / "m3" / "backports" / "recipes"
    if not directory.is_dir() or directory.is_symlink():
        return []
    prefixes = (f"jep-{jep}", f"j{jep}")
    return sorted((p for p in directory.iterdir()
                   if p.is_dir() and not p.is_symlink()
                   and any(p.name == prefix or p.name.startswith(prefix + "-"
                           ) for prefix in prefixes)), key=lambda p: p.name)


def inspect(root: Path, catalogue: Iterable[Mapping[str, str]]) -> list[Witness]:
    """Inspect exact receipt-named product files; never infer feature completion."""
    output: list[Witness] = []
    for row in catalogue:
        jep = int(row["jep"])
        packets = packet_dirs(root, jep)
        receipts = [p / "CURRENT_TREE_RECEIPT.tsv" for p in packets
                    if (p / "CURRENT_TREE_RECEIPT.tsv").is_file()]
        checked = matched = 0
        invalid = drift = False
        for receipt_path in receipts:
            try:
                receipt = read_receipt(receipt_path)
                # A count, a source_materialized=true claim, source-replay delta,
                # or a test-file hash alone is not product-source evidence.
                product = receipt.get("product_path", "")
                digest = receipt.get("product_sha256", "")
                if bool(product) != bool(digest):
                    raise ValueError("incomplete product-path/hash witness")
                if not product:
                    continue
                if not SHA256.fullmatch(digest):
                    raise ValueError("invalid lowercase SHA-256 witness")
                target = safe_target(root, product)
                checked += 1
                if not target.is_file():
                    drift = True
                elif hashlib.sha256(target.read_bytes()).hexdigest() == digest:
                    matched += 1
                else:
                    drift = True
            except (ValueError, UnicodeError, csv.Error, OSError):
                invalid = True
        if invalid:
            state = "INVALID_RECEIPT"
        elif drift:
            state = "SOURCE_DRIFT"
        elif checked:
            state = "RECORDED_PRODUCT_HASH_MATCHED"
        elif receipts:
            state = "RECEIPT_WITHOUT_PRODUCT_HASH"
        elif packets:
            state = "PACKET_WITHOUT_RECEIPT"
        else:
            state = "NO_PACKET"
        output.append(Witness(
            release=int(row["release"]), jep=jep, title=row["title"],
            disposition=row["disposition"], packet_count=len(packets),
            receipt_count=len(receipts), product_paths_checked=checked,
            product_hashes_matched=matched, witness_state=state,
        ))
    return output


def render_tsv(rows: Sequence[Witness]) -> str:
    stream = io.StringIO(newline="")
    writer = csv.writer(stream, delimiter="\t", lineterminator="\n")
    writer.writerow(FIELDS)
    for r in rows:
        writer.writerow((r.release, r.jep, r.title, r.disposition,
                         r.packet_count, r.receipt_count,
                         r.product_paths_checked, r.product_hashes_matched,
                         r.witness_state, r.java21_compatibility, r.promotion))
    return stream.getvalue()


def checked_output_path(root: Path, requested: Path) -> Path:
    """Keep generated evidence under target/, never alias JDK product sources."""
    if ".." in requested.parts:
        raise ValueError("source-witness output contains traversal")
    # Do not resolve the candidate first: resolve() would hide an alias.
    dest = Path(os.path.abspath(requested))
    target_root = root / "m3" / "backports" / "target"
    try:
        suffix = dest.relative_to(target_root)
    except ValueError as exc:
        raise ValueError("source-witness output must be under m3/backports/target") from exc
    if not suffix.parts or any(p in {".", ".."} for p in suffix.parts):
        raise ValueError("invalid source-witness output")
    candidate = root
    for component in ("m3", "backports", "target", *suffix.parts):
        candidate = candidate / component
        if candidate.is_symlink():
            raise ValueError("source-witness output has a symlink alias")
    if dest.is_file() and dest.stat().st_nlink > 1:
        raise ValueError("source-witness output is hard-linked")
    if dest.exists() and not dest.is_file():
        raise ValueError("source-witness output is not a regular file")
    return dest


def main(argv: Sequence[str] | None = None) -> int:
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument("--root", type=Path, required=True)
    p.add_argument("--out", type=Path)
    p.add_argument("--fail-on-drift", action="store_true",
                   help="exit 2 if any receipt is invalid or a recorded hash drifts")
    args = p.parse_args(argv)
    root = args.root.resolve(strict=True)
    rows = inspect(root, read_catalogue(root / "m3/backports/JEP_CATALOGUE.tsv"))
    out = render_tsv(rows)
    if args.out:
        dest = checked_output_path(root, args.out)
        dest.parent.mkdir(parents=True, exist_ok=True)
        dest.write_text(out, encoding="utf-8", newline="")
    else:
        print(out, end="")
    states = ", ".join(f"{key}={value}" for key, value in sorted(
        Counter(row.witness_state for row in rows).items()))
    print(f"JEP_SOURCE_WITNESS rows={len(rows)} {states}", file=sys.stderr)
    return 2 if args.fail_on_drift and any(row.witness_state in BAD_STATES for row in rows) else 0


if __name__ == "__main__":
    raise SystemExit(main())
