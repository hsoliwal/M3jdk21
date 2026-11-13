#!/usr/bin/env python3
# Copyright (c) 2026 Hitesh Soliwal.
# SPDX-License-Identifier: Apache-2.0
"""Inventory exact current-tree preimages for one backport packet without mutation."""

from __future__ import annotations

import argparse
import csv
import hashlib
import re
import subprocess
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


@dataclass(frozen=True)
class BaselineAdmission:
    path: str
    current_status: str
    current_sha256: str
    baseline_status: str
    baseline_sha256: str
    kind: str
    admission: str


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


def canonical_ref(value: str) -> str:
    if (
        not value
        or len(value) > 200
        or value.startswith("-")
        or re.fullmatch(r"[A-Za-z0-9][A-Za-z0-9._/+\\-]{0,199}", value) is None
    ):
        raise ValueError(f"noncanonical Git ref: {value!r}")
    return value


def baseline_inventory(
    git_repository: Path, ref: str, paths: Sequence[str]
) -> list[Preimage]:
    repository = git_repository.resolve()
    if not repository.is_dir():
        raise ValueError(f"baseline Git repository missing: {repository}")
    checked_ref = canonical_ref(ref)
    verified = subprocess.run(
        ["git", "-C", str(repository), "rev-parse", "--verify", f"{checked_ref}^{{commit}}"],
        stdout=subprocess.PIPE,
        stderr=subprocess.PIPE,
        check=False,
    )
    if verified.returncode != 0:
        raise ValueError(f"baseline Git ref missing: {checked_ref}")

    rows: list[Preimage] = []
    for relative in paths:
        canonical = canonical_relative(relative)
        listed = subprocess.run(
            ["git", "-C", str(repository), "ls-tree", "-r", "--name-only", checked_ref, "--", canonical],
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
            check=False,
        )
        if listed.returncode != 0:
            raise ValueError(f"cannot inspect baseline path: {canonical}")
        names = listed.stdout.decode("utf-8").splitlines()
        if canonical not in names:
            rows.append(Preimage(canonical, "ABSENT", "ABSENT", 0, kind(canonical)))
            continue

        shown = subprocess.run(
            ["git", "-C", str(repository), "show", f"{checked_ref}:{canonical}"],
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
            check=False,
        )
        if shown.returncode != 0:
            raise ValueError(f"baseline path is not a readable file: {canonical}")
        data = shown.stdout
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


def admit_against_baseline(
    current_root: Path,
    paths: Sequence[str],
    baseline_git: Path,
    baseline_ref: str,
) -> list[BaselineAdmission]:
    current = inventory(current_root, paths)
    baseline = baseline_inventory(baseline_git, baseline_ref, paths)
    if [row.path for row in current] != [row.path for row in baseline]:
        raise ValueError("current/baseline path order mismatch")

    admitted: list[BaselineAdmission] = []
    for current_row, baseline_row in zip(current, baseline, strict=True):
        same = (
            current_row.status == baseline_row.status
            and current_row.sha256 == baseline_row.sha256
        )
        admitted.append(
            BaselineAdmission(
                current_row.path,
                current_row.status,
                current_row.sha256,
                baseline_row.status,
                baseline_row.sha256,
                current_row.kind,
                "MECHANICAL_FILE_REPLAY" if same else "HOLD_CURRENT_TREE_DRIFT",
            )
        )
    return admitted


def write_admission_tsv(rows: Sequence[BaselineAdmission], out) -> None:
    writer = csv.writer(out, delimiter="\t", lineterminator="\n")
    writer.writerow(
        (
            "path",
            "current_status",
            "current_sha256",
            "baseline_status",
            "baseline_sha256",
            "kind",
            "admission",
        )
    )
    for row in rows:
        writer.writerow(
            (
                row.path,
                row.current_status,
                row.current_sha256,
                row.baseline_status,
                row.baseline_sha256,
                row.kind,
                row.admission,
            )
        )


def write_mechanical_paths(rows: Sequence[BaselineAdmission], out) -> None:
    values = [
        row.path
        for row in rows
        if row.admission == "MECHANICAL_FILE_REPLAY"
    ]
    if values != sorted(values):
        raise ValueError("mechanical path output must be sorted")
    for value in values:
        out.write(value + "\n")


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
    parser.add_argument("--baseline-git", type=Path)
    parser.add_argument("--baseline-ref")
    parser.add_argument("--admission-out", type=Path)
    parser.add_argument("--mechanical-paths-out", type=Path)
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

    baseline_args = (args.baseline_git, args.baseline_ref, args.admission_out)
    if any(value is not None for value in baseline_args):
        if any(value is None for value in baseline_args):
            raise ValueError(
                "--baseline-git, --baseline-ref and --admission-out must be supplied together"
            )
        admissions = admit_against_baseline(
            args.root,
            paths,
            args.baseline_git,
            args.baseline_ref,
        )
        args.admission_out.parent.mkdir(parents=True, exist_ok=True)
        with args.admission_out.open("w", encoding="utf-8", newline="") as handle:
            write_admission_tsv(admissions, handle)
        if args.mechanical_paths_out is not None:
            args.mechanical_paths_out.parent.mkdir(parents=True, exist_ok=True)
            with args.mechanical_paths_out.open("w", encoding="utf-8", newline="") as handle:
                write_mechanical_paths(admissions, handle)
        mechanical = sum(
            row.admission == "MECHANICAL_FILE_REPLAY" for row in admissions
        )
        held = len(admissions) - mechanical
        print(
            f"CURRENT_PREIMAGE_ADMISSION rows={len(admissions)} "
            f"mechanical={mechanical} held={held}"
        )
    elif args.mechanical_paths_out is not None:
        raise ValueError("--mechanical-paths-out requires baseline admission mode")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
