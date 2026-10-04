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

    @property
    def effective_sha256(self) -> str:
        """Normalized content identity; valid converged rows have consistent hashes."""
        return self.post_sha256


def _sha256_bytes(payload: bytes) -> str:
    return hashlib.sha256(payload).hexdigest()


def _relative_path(value: str) -> str:
    """One portable path identity: no traversal, aliases, controls or Git metadata."""
    if not isinstance(value, str):
        raise ValueError("path must be text")
    text = value.replace("\\", "/")
    parts = text.split("/")
    if (
        text != text.strip()
        or ":" in text
        or any(ord(char) < 32 or ord(char) == 127 for char in text)
        or any(part in {"", ".", ".."} or part.lower() == ".git" for part in parts)
    ):
        raise ValueError(f"invalid relative path: {value!r}")
    return text


def _canonical_path(value: str) -> str:
    text = _relative_path(value)
    if not text.startswith("src/") or not text.endswith(".java"):
        raise ValueError(f"invalid Java target path: {value!r}")
    return text


def regular_file(root: Path, relative: str) -> Path:
    """Resolve a selected file without following any symlink below the admitted root."""
    path = root
    for part in _relative_path(relative).split("/"):
        path = path / part
        if path.is_symlink():
            raise ValueError(f"symlink in selected path: {relative}")
    if not path.is_file() or not path.resolve().is_relative_to(root):
        raise ValueError(f"selected file missing or escaped root: {relative}")
    return path


def require_converged(row: Row) -> None:
    """A fixed-point declaration requires internally consistent source evidence."""
    if row.status not in _ALLOWED or not row.fixed_point:
        raise ValueError(
            f"Java target is not at FILE fixed point: {row.path}: "
            f"{row.status}/{row.fixed_point}"
        )
    changed = row.status == "CONVERGED_CHANGED"
    if changed != (row.pre_sha256 != row.post_sha256):
        raise ValueError(f"inconsistent convergence hashes: {row.path}")
    if changed != bool(row.candidate):
        raise ValueError(f"inconsistent convergence candidate: {row.path}")


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
        if (reader.fieldnames is None
                or len(set(reader.fieldnames)) != len(reader.fieldnames)
                or not required.issubset(reader.fieldnames)):
            raise ValueError("SOURCE_CONVERGENCE manifest header mismatch")

        rows: dict[str, Row] = {}
        for raw in reader:
            if None in raw or any(value is None for value in raw.values()):
                raise ValueError("SOURCE_CONVERGENCE manifest row width mismatch")
            target = _canonical_path(raw["path"])
            if target in rows:
                raise ValueError(f"duplicate convergence row: {target}")
            pre = raw["preSha256"].strip()
            post = raw["postSha256"].strip()
            if not _hash(pre) or not _hash(post):
                raise ValueError(f"invalid convergence hash: {target}")
            fixed_text = raw["fixedPoint"].strip().lower()
            if fixed_text not in {"true", "false"}:
                raise ValueError(f"invalid fixedPoint value: {target}")
            fixed = fixed_text == "true"
            status = raw["status"].strip()
            candidate = _relative_path(raw["candidate"]) if raw["candidate"] else ""
            row = Row(target, pre, post, fixed, status, candidate)
            if status in _ALLOWED:
                require_converged(row)
            rows[target] = row
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
        require_converged(row)
        source = regular_file(root, target)
        current = _sha256_bytes(source.read_bytes())
        if current != row.pre_sha256:
            raise ValueError(
                f"Java target preimage drift: {target}: "
                f"{current} != {row.pre_sha256}"
            )

        if row.status == "CONVERGED_CHANGED":
            candidate = regular_file(manifest.parent, row.candidate)
            actual_post = _sha256_bytes(candidate.read_bytes())
            if actual_post != row.post_sha256:
                raise ValueError(
                    f"candidate postimage drift: {target}: "
                    f"{actual_post} != {row.post_sha256}"
                )

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
