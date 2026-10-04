#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0
"""Verify that Java backport targets are at the M3 FILE convergence fixed point."""

from __future__ import annotations

import argparse
import csv
import hashlib
from dataclasses import dataclass
from pathlib import Path


_ALLOWED = {"CONVERGED_CHANGED", "CONVERGED_UNCHANGED"}


@dataclass(frozen=True)
class Row:
    path: str
    pre_sha256: str
    post_sha256: str
    fixed_point: bool
    status: str
    candidate: str


def _sha256_bytes(payload: bytes) -> str:
    return hashlib.sha256(payload).hexdigest()


def _canonical_path(value: str) -> str:
    text = value.strip().replace("\\", "/")
    if (
        not text.startswith("src/")
        or not text.endswith(".java")
        or "/../" in text
        or "/./" in text
        or "\x00" in text
    ):
        raise ValueError(f"invalid Java target path: {value!r}")
    return text


def load_manifest(path: Path) -> dict[str, Row]:
    with path.open("r", encoding="utf-8", newline="") as handle:
        reader = csv.DictReader(handle, delimiter="\t")
        required = {
            "path",
            "preSha256",
            "postSha256",
            "fixedPoint",
            "status",
            "candidate",
        }
        if reader.fieldnames is None or not required.issubset(reader.fieldnames):
            raise ValueError("SOURCE_CONVERGENCE manifest header mismatch")

        rows: dict[str, Row] = {}
        for raw in reader:
            target = _canonical_path(raw["path"])
            if target in rows:
                raise ValueError(f"duplicate convergence row: {target}")
            pre = raw["preSha256"].strip()
            post = raw["postSha256"].strip()
            if not _hash(pre) or not _hash(post):
                raise ValueError(f"invalid convergence hash: {target}")
            fixed = raw["fixedPoint"].strip().lower() == "true"
            status = raw["status"].strip()
            candidate = raw["candidate"].strip().replace("\\", "/")
            rows[target] = Row(target, pre, post, fixed, status, candidate)
        return rows


def load_targets(path: Path) -> list[str]:
    targets: list[str] = []
    for line in path.read_text(encoding="utf-8").splitlines():
        text = line.strip()
        if not text or text.startswith("#"):
            continue
        first = text.split("\t", 1)[0].strip().replace("\\", "/")
        if first.lower() in {"path", "target", "target_path"}:
            continue
        if not first.endswith(".java"):
            continue
        targets.append(_canonical_path(first))
    return sorted(set(targets))


def verify(root: Path, manifest: Path, targets_file: Path) -> str:
    root = root.resolve()
    manifest = manifest.resolve()
    rows = load_manifest(manifest)
    targets = load_targets(targets_file)
    evidence: list[str] = []

    for target in targets:
        row = rows.get(target)
        if row is None:
            raise ValueError(f"missing SOURCE_CONVERGENCE row: {target}")
        if row.status not in _ALLOWED or not row.fixed_point:
            raise ValueError(
                f"Java target is not at FILE fixed point: {target}: "
                f"{row.status}/{row.fixed_point}"
            )

        source = (root / target).resolve()
        if not source.is_file() or not source.is_relative_to(root):
            raise ValueError(f"Java target missing or escaped root: {target}")
        current = _sha256_bytes(source.read_bytes())
        if current != row.pre_sha256:
            raise ValueError(
                f"Java target preimage drift: {target}: "
                f"{current} != {row.pre_sha256}"
            )

        if row.status == "CONVERGED_CHANGED":
            if not row.candidate:
                raise ValueError(f"changed convergence row has no candidate: {target}")
            candidate = (manifest.parent / row.candidate).resolve()
            if not candidate.is_file() or not candidate.is_relative_to(manifest.parent):
                raise ValueError(f"candidate missing or escaped manifest root: {target}")
            actual_post = _sha256_bytes(candidate.read_bytes())
            if actual_post != row.post_sha256:
                raise ValueError(
                    f"candidate postimage drift: {target}: "
                    f"{actual_post} != {row.post_sha256}"
                )
        elif row.candidate:
            raise ValueError(f"unchanged convergence row unexpectedly has candidate: {target}")

        evidence.append(
            "\x1f".join(
                (
                    target,
                    row.pre_sha256,
                    row.post_sha256,
                    row.status,
                    "fixedPoint=true",
                )
            )
        )

    payload = "M3_SOURCE_CONVERGENCE_GATE_V1\n" + "\n".join(evidence) + "\n"
    return hashlib.sha256(payload.encode("utf-8")).hexdigest()


def _hash(value: str) -> bool:
    return len(value) == 64 and all(c in "0123456789abcdef" for c in value)


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("jdk_root", type=Path)
    parser.add_argument("manifest", type=Path)
    parser.add_argument("targets", type=Path)
    args = parser.parse_args()
    root = verify(args.jdk_root, args.manifest, args.targets)
    print(f"SOURCE_CONVERGENCE_GATE\tPASS\t{root}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
