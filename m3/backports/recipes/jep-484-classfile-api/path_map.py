#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0
"""Deterministic JDK21-internal -> JDK24 Class-File API path/content mapper."""

from __future__ import annotations

import argparse
import csv
import hashlib
import json
from pathlib import Path
import re
import subprocess
import sys
from dataclasses import dataclass


J21 = "src/java.base/share/classes/jdk/internal/classfile/"
J24_PUBLIC = "src/java.base/share/classes/java/lang/classfile/"
J24_INTERNAL = "src/java.base/share/classes/jdk/internal/classfile/"
VERSION_PATTERNS = (
    ("JAVA_VERSION_SYMBOL", re.compile(r"\bJAVA_(22|23|24)_VERSION\b")),
    ("SOURCE_RELEASE_SYMBOL", re.compile(r"\bRELEASE_(22|23|24)\b")),
    ("CLASSFILE_MAJOR_LITERAL", re.compile(r"(?<!\d)(66|67|68)(?!\d)")),
)


@dataclass(frozen=True)
class FileRow:
    path: str
    relative: str
    normalized: str
    sha256: str
    plane: str


def git(repo: Path, *args: str) -> bytes:
    proc = subprocess.run(
        ("git", "-C", str(repo), *args),
        stdout=subprocess.PIPE,
        stderr=subprocess.PIPE,
        check=False,
    )
    if proc.returncode:
        raise ValueError(
            f"git {' '.join(args)} failed {proc.returncode}: "
            f"{proc.stderr.decode('utf-8', errors='replace').strip()}"
        )
    return proc.stdout


def resolve(repo: Path, ref: str) -> str:
    value = git(repo, "rev-parse", "--verify", f"{ref}^{{commit}}").decode().strip()
    if not re.fullmatch(r"[0-9a-f]{40}", value):
        raise ValueError(f"invalid resolved ref: {ref}:{value}")
    return value


def paths(repo: Path, ref: str, prefix: str) -> list[str]:
    body = git(repo, "ls-tree", "-r", "--name-only", ref, "--", prefix).decode("utf-8")
    return sorted(
        line for line in body.splitlines()
        if line.startswith(prefix) and line.endswith(".java")
    )


def blob(repo: Path, ref: str, path: str) -> bytes:
    return git(repo, "show", f"{ref}:{path}")


def normalize(relative: str) -> str:
    return relative.replace("Classfile", "ClassFile")


def inventory(repo: Path, ref: str, prefix: str, plane: str) -> list[FileRow]:
    rows: list[FileRow] = []
    for path in paths(repo, ref, prefix):
        relative = path[len(prefix):]
        payload = blob(repo, ref, path)
        rows.append(
            FileRow(
                path=path,
                relative=relative,
                normalized=normalize(relative),
                sha256=hashlib.sha256(payload).hexdigest(),
                plane=plane,
            )
        )
    return rows


def map_rows(source: list[FileRow], donor: list[FileRow]):
    donor_by_key: dict[str, list[FileRow]] = {}
    for row in donor:
        donor_by_key.setdefault(row.normalized, []).append(row)

    mapped = []
    unmapped = []
    ambiguous = []
    for src in source:
        candidates = donor_by_key.get(src.normalized, [])
        if len(candidates) == 1:
            dst = candidates[0]
            mapped.append((src, dst))
        elif not candidates:
            unmapped.append(src)
        else:
            ambiguous.append((src, tuple(sorted(candidates, key=lambda row: row.path))))

    source_keys = {row.normalized for row in source}
    added = [row for row in donor if row.normalized not in source_keys]
    return mapped, unmapped, ambiguous, sorted(added, key=lambda row: row.path)


def version_signals(repo: Path, ref: str, donor: list[FileRow]):
    result = []
    for row in donor:
        text = blob(repo, ref, row.path).decode("utf-8", errors="strict")
        for number, line in enumerate(text.splitlines(), start=1):
            for kind, pattern in VERSION_PATTERNS:
                for match in pattern.finditer(line):
                    result.append(
                        (
                            row.path,
                            row.plane,
                            kind,
                            match.group(0),
                            number,
                            hashlib.sha256(line.strip().encode("utf-8")).hexdigest(),
                        )
                    )
    return sorted(result)


def write_tsv(path: Path, header, rows) -> None:
    with path.open("w", encoding="utf-8", newline="") as handle:
        writer = csv.writer(handle, delimiter="\t", lineterminator="\n")
        writer.writerow(header)
        writer.writerows(rows)


def run(repo: Path, jdk21_ref: str, jdk24_ref: str, out: Path) -> dict[str, int | str]:
    repo = repo.resolve()
    if not repo.is_dir():
        raise ValueError(f"repository not found: {repo}")
    j21 = resolve(repo, jdk21_ref)
    j24 = resolve(repo, jdk24_ref)

    source = inventory(repo, j21, J21, "JDK21_INTERNAL")
    donor = (
        inventory(repo, j24, J24_PUBLIC, "JDK24_PUBLIC")
        + inventory(repo, j24, J24_INTERNAL, "JDK24_INTERNAL")
    )
    donor = sorted(donor, key=lambda row: row.path)

    mapped, unmapped, ambiguous, added = map_rows(source, donor)
    out.mkdir(parents=True, exist_ok=True)

    write_tsv(
        out / "JDK21_INTERNAL_TO_JDK24.tsv",
        (
            "source_path", "source_relative", "source_sha256",
            "donor_path", "donor_relative", "donor_sha256", "donor_plane",
            "normalized_key", "state",
        ),
        (
            (
                src.path, src.relative, src.sha256,
                dst.path, dst.relative, dst.sha256, dst.plane,
                src.normalized, "DIRECT_DESCENDANT",
            )
            for src, dst in mapped
        ),
    )
    write_tsv(
        out / "UNMAPPED_JDK21.tsv",
        ("source_path", "source_relative", "source_sha256", "normalized_key", "state"),
        (
            (row.path, row.relative, row.sha256, row.normalized, "NO_DIRECT_DESCENDANT")
            for row in unmapped
        ),
    )
    write_tsv(
        out / "AMBIGUOUS.tsv",
        ("source_path", "normalized_key", "candidate_paths", "state"),
        (
            (
                src.path,
                src.normalized,
                ",".join(row.path for row in candidates),
                "AMBIGUOUS_DIRECT_DESCENDANT",
            )
            for src, candidates in ambiguous
        ),
    )
    write_tsv(
        out / "ADDED_JDK24.tsv",
        ("donor_path", "donor_relative", "donor_sha256", "donor_plane", "normalized_key"),
        ((row.path, row.relative, row.sha256, row.plane, row.normalized) for row in added),
    )
    write_tsv(
        out / "VERSION_SIGNALS.tsv",
        ("donor_path", "donor_plane", "signal_kind", "signal", "line", "line_sha256"),
        version_signals(repo, j24, donor),
    )

    summary = {
        "schema": 1,
        "jdk21_ref": jdk21_ref,
        "jdk21_commit": j21,
        "jdk24_ref": jdk24_ref,
        "jdk24_commit": j24,
        "jdk21_internal_files": len(source),
        "jdk24_combined_files": len(donor),
        "direct_descendants": len(mapped),
        "unmapped_jdk21": len(unmapped),
        "ambiguous": len(ambiguous),
        "added_jdk24": len(added),
        "mutation_authority": False,
        "promotion_authority": False,
    }
    (out / "summary.json").write_text(
        json.dumps(summary, indent=2, sort_keys=True) + "\n", encoding="utf-8"
    )
    return summary


def main(argv=None) -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--repo", type=Path, required=True)
    parser.add_argument("--jdk21-ref", default="jdk-21+35")
    parser.add_argument("--jdk24-ref", default="jdk-24+36")
    parser.add_argument("--out", type=Path, required=True)
    args = parser.parse_args(argv)
    try:
        summary = run(args.repo, args.jdk21_ref, args.jdk24_ref, args.out)
    except (OSError, UnicodeError, ValueError) as failure:
        print(f"JEP484 path map refused: {failure}", file=sys.stderr)
        return 2
    print(json.dumps(summary, sort_keys=True))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
