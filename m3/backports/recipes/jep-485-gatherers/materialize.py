#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0
"""Exact opt-in materializer for the retained JEP 485 OpenRewrite postimage crate."""

from __future__ import annotations

import argparse
import hashlib
from dataclasses import dataclass
from pathlib import Path
import os
import sys


CRATE = Path(
    "m3/tooling/migration-recipes/src/main/resources/"
    "com/m3/rewrite/backport/jdk21-hash-pinned/jdk24-jep485-stream-gatherers"
)


@dataclass(frozen=True)
class Target:
    path: str
    before: str
    after: str
    resource: str


def sha256_bytes(data: bytes) -> str:
    return hashlib.sha256(data).hexdigest()


def canonical_relative(value: str) -> str:
    if not value or value.startswith(("/", "\\")) or "\\" in value or ":" in value:
        raise ValueError(f"non-canonical path: {value!r}")
    parts = value.split("/")
    if any(part in ("", ".", "..", ".git") for part in parts):
        raise ValueError(f"non-canonical path: {value!r}")
    return "/".join(parts)


def load_manifest(repo: Path) -> list[Target]:
    crate = repo / CRATE
    manifest = crate / "manifest.tsv"
    rows: list[Target] = []
    previous = ""
    for raw in manifest.read_text(encoding="utf-8").splitlines():
        if not raw or raw.startswith("#"):
            continue
        cells = raw.split("\t")
        if len(cells) != 4:
            raise ValueError(f"invalid manifest row: {raw!r}")
        path, before, after, resource = cells
        path = canonical_relative(path)
        resource = canonical_relative(resource)
        if path <= previous:
            raise ValueError("manifest paths must be strictly sorted")
        previous = path
        if before != "ABSENT" and (len(before) != 64 or any(c not in "0123456789abcdef" for c in before)):
            raise ValueError(f"invalid preimage hash: {path}")
        if len(after) != 64 or any(c not in "0123456789abcdef" for c in after):
            raise ValueError(f"invalid postimage hash: {path}")
        payload = (crate / resource).read_bytes()
        if sha256_bytes(payload) != after:
            raise ValueError(f"template hash drift: {path}")
        rows.append(Target(path, before, after, resource))
    if not rows:
        raise ValueError("empty JEP 485 manifest")
    return rows


def target_state(repo: Path, target: Target) -> tuple[str, str]:
    path = repo / target.path
    if path.is_symlink():
        raise ValueError(f"symlink target forbidden: {target.path}")
    if not path.exists():
        return "ABSENT", "ABSENT"
    if not path.is_file():
        raise ValueError(f"non-file target forbidden: {target.path}")
    digest = sha256_bytes(path.read_bytes())
    if digest == target.after:
        return "AFTER", digest
    if target.before != "ABSENT" and digest == target.before:
        return "BEFORE", digest
    raise ValueError(f"JEP485_PREIMAGE_DRIFT:{target.path}:{digest}")


def verify_declared_path(repo: Path, relative: str) -> Path:
    root = repo.resolve()
    candidate = (repo / relative).resolve(strict=False)
    try:
        candidate.relative_to(root)
    except ValueError as failure:
        raise ValueError(f"path escapes repository: {relative}") from failure
    return candidate


def materialize(repo: Path, mode: str, receipt: Path | None) -> int:
    rows = load_manifest(repo)
    receipts: list[tuple[str, str, str]] = []

    for target in rows:
        state, observed = target_state(repo, target)
        receipts.append((target.path, state, observed))

        if mode == "verify-post":
            if state != "AFTER":
                raise ValueError(f"JEP485_POSTIMAGE_MISSING:{target.path}:{state}")
            continue

        if mode == "check":
            if state == "ABSENT" and target.before != "ABSENT":
                raise ValueError(f"JEP485_REQUIRED_PREIMAGE_MISSING:{target.path}")
            continue

        if state == "AFTER":
            continue
        if state == "ABSENT" and target.before != "ABSENT":
            raise ValueError(f"JEP485_REQUIRED_PREIMAGE_MISSING:{target.path}")
        if state == "BEFORE" and target.before == "ABSENT":
            raise ValueError(f"JEP485_UNEXPECTED_PREIMAGE:{target.path}")

        destination = verify_declared_path(repo, target.path)
        source = repo / CRATE / target.resource
        destination.parent.mkdir(parents=True, exist_ok=True)
        temporary = destination.with_name(destination.name + ".m3-jep485.tmp")
        if temporary.exists() or temporary.is_symlink():
            temporary.unlink()
        temporary.write_bytes(source.read_bytes())
        os.replace(temporary, destination)

        digest = sha256_bytes(destination.read_bytes())
        if digest != target.after:
            raise ValueError(f"JEP485_POSTIMAGE_DRIFT:{target.path}:{digest}")

    if receipt is not None:
        receipt.parent.mkdir(parents=True, exist_ok=True)
        body = ["path\tinput_state\tinput_sha256\tpost_sha256"]
        for target, row in zip(rows, receipts):
            path, state, observed = row
            post = target.after if mode == "apply" or state == "AFTER" else ""
            body.append(f"{path}\t{state}\t{observed}\t{post}")
        receipt.write_text("\n".join(body) + "\n", encoding="utf-8")

    return len(rows)


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--root", type=Path, default=Path("."))
    parser.add_argument(
        "--mode",
        choices=("check", "apply", "verify-post"),
        default="check",
    )
    parser.add_argument("--receipt", type=Path)
    args = parser.parse_args()

    try:
        count = materialize(args.root, args.mode, args.receipt)
    except (OSError, ValueError) as failure:
        print(f"jep485 materialization refused: {failure}", file=sys.stderr)
        return 2

    print(f"jep485 {args.mode}: {count} manifest targets accepted")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
