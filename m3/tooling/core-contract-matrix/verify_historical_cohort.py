#!/usr/bin/env python3
# SPDX-FileCopyrightText: 2026 Synexia Project Contributors
# SPDX-License-Identifier: Apache-2.0
"""Admit the entire original 81-blob M3 core cohort from one immutable Git tree.

Source files and authoring manifest remain distinct. No product source is patched,
recreated, symlinked, or copied into today's java.base.
"""
from __future__ import annotations

import argparse
import hashlib
import json
from pathlib import Path, PurePosixPath
import re
import subprocess

EXPECTED_FILES = 81
EXPECTED_JAVA = 75
SCHEMA = "M3_CURRENT_CORE_CONTRACT_INPUTS_V1"
HEX40 = re.compile(r"[0-9a-f]{40}\Z")
HEX64 = re.compile(r"[0-9a-f]{64}\Z")


def command(*args: str) -> bytes:
    result = subprocess.run(args, capture_output=True, timeout=30, check=False)
    if result.returncode:
        raise ValueError(f"failed Git operation {args[0:3]}: {result.stderr[:280]!r}")
    return result.stdout


def regular(path: Path) -> bytes:
    if not path.is_file() or any(item.is_symlink() for item in (path, *path.parents)):
        raise ValueError(f"missing or redirected source: {path}")
    return path.read_bytes()


def safe_name(name: str) -> str:
    p = PurePosixPath(name)
    if (not name or p.is_absolute() or "\\" in name or ":" in name
            or any(x in ("", ".", "..") for x in name.split("/"))):
        raise ValueError(f"unsafe Git path: {name!r}")
    return name


def identity(data: bytes) -> dict[str, object]:
    return {
        "bytes": len(data),
        "gitBlob": hashlib.sha1(b"blob " + str(len(data)).encode("ascii") + b"\0" + data).hexdigest(),
        "sha256": hashlib.sha256(data).hexdigest(),
    }


def verify(repo: Path, manifest_path: Path, expected_commit: str) -> dict[str, object]:
    if not HEX40.fullmatch(expected_commit):
        raise ValueError("expected commit requires a full 40-hex ID")
    manifest_raw = regular(manifest_path)
    manifest = json.loads(manifest_raw)
    if (manifest.get("schema") != SCHEMA or
            manifest.get("repository") != "hsoliwal/M3jdk21" or
            manifest.get("sourceCommit") != expected_commit):
        raise ValueError("historical manifest metadata/commit drift")
    rows = manifest.get("files")
    if not isinstance(rows, list) or len(rows) != EXPECTED_FILES:
        raise ValueError("exact 81 historical source records required")
    if not (len(manifest.get("programs", [])) == 18 and
            manifest.get("modes") == ["jit", "int", "nocompact", "c2"]):
        raise ValueError("historical program/mode census drift")

    actual_commit = command("git", "-C", str(repo), "rev-parse", "HEAD").decode().strip()
    if actual_commit != expected_commit:
        raise ValueError("working source checkout is not pinned historical commit")
    raw_tree = command("git", "-C", str(repo), "ls-tree", "--full-tree", "-rz", "HEAD")
    tree: dict[str, tuple[str, str]] = {}
    for encoded in raw_tree.split(b"\0"):
        if not encoded:
            continue
        metadata, sep, name = encoded.partition(b"\t")
        if not sep:
            raise ValueError("malformed Git tree")
        parts = metadata.decode("ascii").split()
        if len(parts) != 3:
            raise ValueError("malformed Git object metadata")
        decoded = safe_name(name.decode("utf-8", "strict"))
        if decoded in tree:
            raise ValueError("duplicate Git path")
        tree[decoded] = (parts[0], parts[2])

    seen: set[str] = set()
    results: list[dict[str, object]] = []
    for row in rows:
        if not isinstance(row, dict) or set(row) != {"path", "gitBlob", "bytes", "sha256"}:
            raise ValueError("historical source identity row drift")
        name = safe_name(row["path"])
        if name in seen:
            raise ValueError("duplicate historical source record")
        seen.add(name)
        if (not HEX40.fullmatch(row["gitBlob"]) or not HEX64.fullmatch(row["sha256"])
                or type(row["bytes"]) is not int or row["bytes"] < 0):
            raise ValueError("historical source record invalid")
        mode, blob = tree.get(name, (None, None))
        if mode not in ("100644", "100755") or blob != row["gitBlob"]:
            raise ValueError("historical tree/path/blob mismatch: " + name)
        actual = identity(regular(repo / name))
        if actual != {field: row[field] for field in ("bytes", "gitBlob", "sha256")}:
            raise ValueError("historical bytes/source digest mismatch: " + name)
        results.append({"path": name, **actual})

    present_java = {p for p in tree if (p.startswith("m3/core/src/") or
                                     p.startswith("m3/core/test/")) and p.endswith(".java")}
    declared_java = {p for p in seen if (p.startswith("m3/core/src/") or
                                      p.startswith("m3/core/test/")) and p.endswith(".java")}
    if len(present_java) != EXPECTED_JAVA or present_java != declared_java:
        raise ValueError("all original core Java source atoms must be inventoried")
    ordered = sorted(results, key=lambda row: row["path"])
    source_hash = hashlib.sha256(json.dumps(ordered, sort_keys=True,
                                            separators=(",", ":")).encode()).hexdigest()
    return {
        "schema": "M3_HISTORICAL_CORE_COHORT_AUDIT_V1",
        "historicalCommit": actual_commit,
        "repository": "hsoliwal/M3jdk21",
        "manifestSha256": hashlib.sha256(manifest_raw).hexdigest(),
        "fileCount": len(results),
        "javaSourceCount": len(declared_java),
        "qualifiedSourceSetSha256": source_hash,
        "historicalSourceStatus": "EXACT_PINNED_TREE_AND_BYTES",
        "currentRuntimeQualified": False,
    }


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--source", required=True, type=Path)
    parser.add_argument("--manifest", required=True, type=Path)
    parser.add_argument("--expected-commit", required=True)
    parser.add_argument("--report", required=True, type=Path)
    args = parser.parse_args()
    result = verify(args.source, args.manifest, args.expected_commit)
    report = args.report.absolute()
    if report.exists() or args.source.absolute() in report.parents:
        raise ValueError("report must be a fresh path outside the original source checkout")
    report.parent.mkdir(parents=True, exist_ok=True)
    report.write_text(json.dumps(result, indent=2, sort_keys=True) + "\n", encoding="utf-8")
    print(f"M3_HISTORICAL_CORE_COHORT_PASS|files={result['fileCount']}"
          f"|java={result['javaSourceCount']}|commit={result['historicalCommit']}")


if __name__ == "__main__":
    main()
