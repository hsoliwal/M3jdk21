#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0
"""Overlay fixed-point M3 Java convergence candidates onto a detached worktree copy."""

from __future__ import annotations

import argparse
import csv
import hashlib
import shutil
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
        return self.post_sha256 if self.status == "CONVERGED_CHANGED" else self.pre_sha256


def _hash(payload: bytes) -> str:
    return hashlib.sha256(payload).hexdigest()


def _sha(value: str) -> bool:
    return len(value) == 64 and all(char in "0123456789abcdef" for char in value)


def _path(value: str) -> str:
    path = value.strip().replace("\\", "/")
    if (
        not (path.startswith("src/") or path.startswith("test/"))
        or not path.endswith(".java")
        or "/../" in path
        or "/./" in path
        or "\x00" in path
    ):
        raise ValueError(f"invalid Java path: {value!r}")
    return path


def load(path: Path) -> list[Row]:
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
        result: list[Row] = []
        seen: set[str] = set()
        for raw in reader:
            target = _path(raw["path"])
            if target in seen:
                raise ValueError(f"duplicate convergence row: {target}")
            seen.add(target)
            pre = raw["preSha256"].strip()
            post = raw["postSha256"].strip()
            if not _sha(pre) or not _sha(post):
                raise ValueError(f"invalid convergence hash: {target}")
            fixed = raw["fixedPoint"].strip().lower() == "true"
            status = raw["status"].strip()
            if status not in _ALLOWED or not fixed:
                raise ValueError(f"unresolved convergence row: {target}: {status}/{fixed}")
            candidate = raw["candidate"].strip().replace("\\", "/")
            if status == "CONVERGED_CHANGED" and not candidate:
                raise ValueError(f"changed row has no candidate: {target}")
            if status == "CONVERGED_UNCHANGED" and candidate:
                raise ValueError(f"unchanged row has candidate: {target}")
            result.append(Row(target, pre, post, fixed, status, candidate))
        return sorted(result, key=lambda row: row.path)


def materialize(source_root: Path, manifest: Path, worktree_root: Path) -> str:
    source_root = source_root.resolve()
    worktree_root = worktree_root.resolve()
    manifest = manifest.resolve()
    if source_root == worktree_root:
        raise ValueError("normalized worktree must differ from canonical source root")
    if not (worktree_root / "src").is_dir():
        raise ValueError("normalized worktree must already contain an OpenJDK src tree")

    rows = load(manifest)
    evidence: list[str] = []
    for row in rows:
        canonical = (source_root / row.path).resolve()
        target = (worktree_root / row.path).resolve()
        if (
            not canonical.is_file()
            or not canonical.is_relative_to(source_root)
            or not target.is_file()
            or not target.is_relative_to(worktree_root)
        ):
            raise ValueError(f"source/worktree path missing or escaped: {row.path}")

        canonical_hash = _hash(canonical.read_bytes())
        if canonical_hash != row.pre_sha256:
            raise ValueError(
                f"canonical preimage drift: {row.path}: "
                f"{canonical_hash} != {row.pre_sha256}"
            )
        if _hash(target.read_bytes()) != row.pre_sha256:
            raise ValueError(f"worktree preimage drift: {row.path}")

        if row.status == "CONVERGED_CHANGED":
            candidate = (manifest.parent / row.candidate).resolve()
            if not candidate.is_file() or not candidate.is_relative_to(manifest.parent):
                raise ValueError(f"candidate missing or escaped: {row.path}")
            payload = candidate.read_bytes()
            if _hash(payload) != row.post_sha256:
                raise ValueError(f"candidate postimage drift: {row.path}")
            target.write_bytes(payload)

        effective = _hash(target.read_bytes())
        if effective != row.effective_sha256:
            raise ValueError(f"normalized worktree drift: {row.path}")
        evidence.append(
            "\x1f".join(
                (
                    row.path,
                    row.pre_sha256,
                    row.post_sha256,
                    row.status,
                    effective,
                )
            )
        )

    payload = "M3_NORMALIZED_JDK21_BASELINE_V1\n" + "\n".join(evidence) + "\n"
    root = hashlib.sha256(payload.encode("utf-8")).hexdigest()
    receipt = worktree_root / "m3-normalized-baseline.tsv"
    receipt.write_text(
        "path\tpreSha256\tpostSha256\tstatus\teffectiveSha256\n"
        + "\n".join(
            "\t".join(
                (
                    row.path,
                    row.pre_sha256,
                    row.post_sha256,
                    row.status,
                    row.effective_sha256,
                )
            )
            for row in rows
        )
        + "\n",
        encoding="utf-8",
    )
    (worktree_root / "m3-normalized-baseline.root").write_text(root + "\n", encoding="utf-8")
    return root


def copy_tree(source: Path, destination: Path) -> None:
    source = source.resolve()
    destination = destination.resolve()
    if destination.exists():
        raise ValueError(f"destination already exists: {destination}")
    shutil.copytree(
        source,
        destination,
        symlinks=True,
        ignore=shutil.ignore_patterns(".git", "build", "target"),
    )


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("source_root", type=Path)
    parser.add_argument("manifest", type=Path)
    parser.add_argument("worktree_root", type=Path)
    parser.add_argument(
        "--copy",
        action="store_true",
        help="copy source_root to worktree_root before overlay; otherwise require an existing detached worktree",
    )
    args = parser.parse_args()

    if args.copy:
        copy_tree(args.source_root, args.worktree_root)
    root = materialize(args.source_root, args.manifest, args.worktree_root)
    print(f"M3_NORMALIZED_BASELINE\tPASS\t{root}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
