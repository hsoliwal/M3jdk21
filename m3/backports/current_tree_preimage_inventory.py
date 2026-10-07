#!/usr/bin/env python3
# Copyright (c) 2026 Hitesh Soliwal.
# SPDX-License-Identifier: Apache-2.0
"""Inventory exact current-tree preimages for one backport packet without mutation."""

from __future__ import annotations

import argparse
import csv
import hashlib
from dataclasses import dataclass
from pathlib import Path
from typing import Sequence


@dataclass(frozen=True)
class Preimage:
    path: str
    status: str
    sha256: str
    bytes: int
    kind: str


_NATIVE_SUFFIXES = (
    ".c",
    ".cc",
    ".cpp",
    ".cxx",
    ".h",
    ".hh",
    ".hpp",
    ".s",
    ".asm",
)


def canonical_relative(value: str) -> str:
    if (
        not value
        or len(value) > 4096
        or value.startswith(("/", "\\"))
        or "\\" in value
        or ":" in value
        or any(ord(ch) < 32 or ord(ch) == 127 for ch in value)
    ):
        raise ValueError(f"noncanonical relative path: {value!r}")
    parts = value.split("/")
    if any(part in {"", ".", "..", ".git"} for part in parts):
        raise ValueError(f"noncanonical relative path: {value!r}")
    return "/".join(parts)


def kind(path: str) -> str:
    lower = path.lower()
    if lower.endswith(".java"):
        return "JAVA"
    if lower.endswith(_NATIVE_SUFFIXES):
        return "NATIVE"
    if lower.endswith(
        (
            ".xml",
            ".properties",
            ".gmk",
            ".m4",
            ".template",
            ".txt",
            ".md",
            ".conf",
        )
    ):
        return "TEXT"
    return "RESOURCE"


def selected_paths(path_file: Path) -> list[str]:
    values = [
        canonical_relative(line.strip())
        for line in path_file.read_text(encoding="utf-8").splitlines()
        if line.strip() and not line.lstrip().startswith("#")
    ]
    if not values:
        raise ValueError("paths file selected zero paths")
    if len(values) != len(set(values)):
        raise ValueError("paths file contains duplicates")
    if values != sorted(values):
        raise ValueError("paths file must be sorted")
    return values


def inventory(root: Path, paths: Sequence[str]) -> list[Preimage]:
    repository = root.resolve()
    rows: list[Preimage] = []
    for relative in paths:
        canonical = canonical_relative(relative)
        candidate = (repository / canonical).resolve(strict=False)
        try:
            candidate.relative_to(repository)
        except ValueError as failure:
            raise ValueError(f"path escapes repository: {canonical}") from failure

        lexical = repository / canonical
        if lexical.is_symlink():
            raise ValueError(f"symlink preimage forbidden: {canonical}")
        if not lexical.exists():
            rows.append(Preimage(canonical, "ABSENT", "ABSENT", 0, kind(canonical)))
            continue
        if not lexical.is_file():
            raise ValueError(f"non-file preimage forbidden: {canonical}")

        data = lexical.read_bytes()
        rows.append(
            Preimage(
                canonical,
                "PRESENT",
                hashlib.sha256(data).hexdigest(),
                len(data),
                kind(canonical),
            )
        )
    return rows


def write_tsv(rows: Sequence[Preimage], out) -> None:
    writer = csv.writer(out, delimiter="\t", lineterminator="\n")
    writer.writerow(("path", "status", "sha256", "bytes", "kind"))
    for row in rows:
        writer.writerow((row.path, row.status, row.sha256, row.bytes, row.kind))


def parse_args(argv: Sequence[str] | None = None) -> argparse.Namespace:
    parser = argparse.ArgumentParser()
    parser.add_argument("--root", type=Path, default=Path("."))
    parser.add_argument("--paths-file", type=Path, required=True)
    parser.add_argument("--out", type=Path, required=True)
    return parser.parse_args(argv)


def main(argv: Sequence[str] | None = None) -> int:
    args = parse_args(argv)
    paths = selected_paths(args.paths_file)
    rows = inventory(args.root, paths)
    args.out.parent.mkdir(parents=True, exist_ok=True)
    with args.out.open("w", encoding="utf-8", newline="") as handle:
        write_tsv(rows, handle)
    present = sum(row.status == "PRESENT" for row in rows)
    absent = len(rows) - present
    print(f"CURRENT_PREIMAGE_INVENTORY rows={len(rows)} present={present} absent={absent}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
