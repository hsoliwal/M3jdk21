#!/usr/bin/env python3
# Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0
"""Qualify one exact SDK append without altering historical source receipts.

Adapts the existing M3 Apache verifier and SDK owner-repair byte checker.
Java/provider behavior and receiving-recipe execution remain separate proofs.
"""
from __future__ import annotations
import argparse
import hashlib
import json
import re
from pathlib import Path, PurePosixPath

HERE = Path(__file__).resolve().parent
RECEIPT_NAME = "SDK_SOURCE_BINDING_SUCCESSOR_V3.json"
RECEIPT_BYTES = 4649
RECEIPT_SHA256 = "18a5bbbe10e3a97c01d7b3d55277a3e3dabc7ed4666ee00f1d7fb094ae8653d7"

def checked(path: Path) -> bytes:
    if not path.is_file() or path.is_symlink():
        raise ValueError("missing or symlinked input: " + str(path))
    return path.read_bytes()

def sha256(data: bytes) -> str:
    return hashlib.sha256(data).hexdigest()

def blob(data: bytes) -> str:
    return hashlib.sha1(b"blob " + str(len(data)).encode("ascii") + b"\0" + data).hexdigest()

def input_path(root: Path, name: str) -> Path:
    relative = PurePosixPath(name)
    if relative.is_absolute() or ".." in relative.parts or not name.startswith("m3/collections/"):
        raise ValueError("unapproved source path: " + name)
    path = root.joinpath(*relative.parts)
    if not path.resolve().is_relative_to(root):
        raise ValueError("input escapes repository root: " + name)
    return path

def pinned(root: Path, row: dict) -> bytes:
    data = checked(input_path(root, row["path"]))
    if len(data) != row["bytes"] or sha256(data) != row["sha256"]:
        raise ValueError("source custody differs: " + row["path"])
    if "gitBlob" in row and blob(data) != row["gitBlob"]:
        raise ValueError("Git blob differs: " + row["path"])
    return data

def run(root: Path, jar: Path | None = None, args_file: Path | None = None) -> dict:
    root = root.resolve()
    receipt_path = HERE / RECEIPT_NAME
    raw = checked(receipt_path)
    if len(raw) != RECEIPT_BYTES or sha256(raw) != RECEIPT_SHA256:
        raise ValueError("sealed successor receipt differs")
    receipt = json.loads(raw)
    history = [pinned(root, row) for row in receipt["history"]]
    old = json.loads(history[0])
    v2 = json.loads(history[2])
    sources = old["sources"]
    sdk_rows = [row for row in sources if row["path"] == receipt["sdkPath"]]
    owned = [row for row in sources if "/implementations/" in row["path"]]
    if (len(sources) != receipt["oldSourceRows"] or len(sdk_rows) != 1
            or len({row["path"] for row in sources}) != len(sources)
            or len(owned) != receipt["unchangedOwnedRows"] or owned != v2["targets"]):
        raise ValueError("historical source composition differs")
    before = pinned(root, receipt["before"])
    predecessor = sdk_rows[0]
    if len(before) != predecessor["bytes"] or sha256(before) != predecessor["sha256"]:
        raise ValueError("historical SDK link differs")
    for row in owned:
        pinned(root, row)
    after_row = {"path": receipt["sdkPath"], **receipt["after"]}
    after = pinned(root, after_row)
    addition = after[len(before) - 2:len(after) - 2]
    if (before[-2:] != b"}\n" or after != before[:-2] + addition + before[-2:]
            or len(addition) != receipt["append"]["bytes"]
            or sha256(addition) != receipt["append"]["sha256"]):
        raise ValueError("SDK is not the exact additive successor")
    old_methods = len(re.findall(rb"\bpublic\s+static\b", before))
    new_methods = len(re.findall(rb"\bpublic\s+static\b", after))
    if old_methods != receipt["preservedOldPublicStaticMethods"] or new_methods != old_methods + 4:
        raise ValueError("SDK public declaration count differs")
    for name in receipt["addedFactories"]:
        if len(re.findall(rb"\b" + name.encode("ascii") + rb"\s*\(", addition)) != 1:
            raise ValueError("added factory declaration differs: " + name)
    for row in receipt["arrayAndNativeOwners"]:
        pinned(root, row)
    if jar is not None:
        data = checked(jar)
        provider = old["provider"]
        if (jar.name != provider["basename"] or len(data) != provider["bytes"]
                or sha256(data) != provider["sha256"]):
            raise ValueError("provider custody differs")
    if args_file is not None:
        output = args_file.resolve()
        protected = [input_path(root, row["path"]) for row in sources]
        protected += [input_path(root, row["path"]) for row in receipt["history"]]
        protected += [input_path(root, receipt["before"]["path"]), receipt_path, Path(__file__).resolve()]
        protected += [input_path(root, row["path"]) for row in receipt["arrayAndNativeOwners"]]
        if (args_file.is_symlink() or output.is_relative_to(root)
                or output in [path.resolve() for path in protected]
                or (output.exists() and any(output.samefile(path) for path in protected))):
            raise ValueError("argument output aliases protected source")
        paths = [str(input_path(root, row["path"]).resolve()).replace("\\", "/") for row in owned]
        args_file.write_text("\n".join('"' + path + '"' for path in paths) + "\n", encoding="utf-8")
    return {"sources": len(sources), "source_custody": "PASS", "provider_checked": jar is not None,
            "predecessor_sealed": True, "effective_sdk_bytes": len(after),
            "old_public_bodies_preserved": old_methods, "added_factories": receipt["addedFactories"]}

def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--root", type=Path, default=HERE.parents[3])
    parser.add_argument("--jar", type=Path)
    parser.add_argument("--args", type=Path)
    args = parser.parse_args()
    print(json.dumps(run(args.root, args.jar, args.args)))

if __name__ == "__main__":
    main()
