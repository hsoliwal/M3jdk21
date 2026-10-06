#!/usr/bin/env python3
from __future__ import annotations

import argparse
import hashlib
import pathlib
import sys

ABSENT = "ABSENT"


def sha256(data: bytes) -> str:
    return hashlib.sha256(data).hexdigest()


def safe_relative(value: str) -> pathlib.PurePosixPath:
    path = pathlib.PurePosixPath(value)
    if path.is_absolute() or ".." in path.parts or "." in path.parts:
        raise ValueError(f"unsafe target path: {value}")
    if not value.endswith(".java"):
        raise ValueError(f"Java target required: {value}")
    return path


def rows(manifest: pathlib.Path):
    result = []
    previous = ""
    for raw in manifest.read_text(encoding="utf-8").splitlines():
        if not raw or raw.startswith("#"):
            continue
        cells = raw.split("\t")
        if len(cells) != 4:
            raise ValueError("invalid manifest row")
        path, before, after, template = cells
        safe_relative(path)
        if previous >= path:
            raise ValueError("manifest paths must be sorted and unique")
        previous = path
        if before != ABSENT and len(before) != 64:
            raise ValueError("invalid preimage hash")
        if len(after) != 64:
            raise ValueError("invalid postimage hash")
        result.append((path, before, after, template))
    if not result:
        raise ValueError("empty manifest")
    return result


def materialize(root: pathlib.Path, manifest: pathlib.Path, receipt: pathlib.Path) -> None:
    root = root.resolve()
    resource_dir = manifest.parent.resolve()
    lines = ["path\tbefore\tafter\tstate"]
    for rel, before, after, template in rows(manifest):
        target = root / pathlib.PurePosixPath(rel)
        if target.exists() and target.is_symlink():
            raise ValueError(f"symlink target rejected: {rel}")
        current = ABSENT if not target.exists() else sha256(target.read_bytes())
        if current == after:
            state = "FIXED_POINT"
        elif current == before:
            payload = (resource_dir / template).read_bytes()
            if sha256(payload) != after:
                raise ValueError(f"template hash drift: {rel}")
            target.parent.mkdir(parents=True, exist_ok=True)
            target.write_bytes(payload)
            if sha256(target.read_bytes()) != after:
                raise ValueError(f"postimage drift: {rel}")
            state = "MATERIALIZED"
        else:
            raise ValueError(f"preimage drift: {rel}: {current}")
        lines.append(f"{rel}\t{before}\t{after}\t{state}")
    receipt.parent.mkdir(parents=True, exist_ok=True)
    receipt.write_text("\n".join(lines) + "\n", encoding="utf-8")


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--root", required=True, type=pathlib.Path)
    parser.add_argument("--manifest", required=True, type=pathlib.Path)
    parser.add_argument("--receipt", required=True, type=pathlib.Path)
    args = parser.parse_args()
    materialize(args.root, args.manifest, args.receipt)
    return 0


if __name__ == "__main__":
    sys.exit(main())
