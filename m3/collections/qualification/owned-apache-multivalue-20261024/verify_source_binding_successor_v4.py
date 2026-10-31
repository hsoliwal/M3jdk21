#!/usr/bin/env python3
# Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
"""Qualify the D1 source successor of the sealed v3 multivalue receipt without altering it.

The v3 receipt and verifier stay byte-exact history. Every v3 pin still holds except the rows this sealed v4
receipt lists as successors: the three D1 counter bodies, and the array/native owners and SDK that master changed
after v3 was sealed. A successor row is admitted only if its before pin equals the v3 pin and the file equals its
after pin. Added files and the v2-to-v3 recipe manifest are pinned as well. Java behaviour, recipe execution and
provider parity remain separate proofs.
"""
from __future__ import annotations
import argparse
import hashlib
import json
import re
from pathlib import Path, PurePosixPath

HERE = Path(__file__).resolve().parent
RECEIPT_NAME = "SDK_SOURCE_BINDING_SUCCESSOR_V4.json"
RECEIPT_BYTES = 8204
RECEIPT_SHA256 = "3360d65da5075cee73e8c74a7aa863ce6dffa5faf0e7e8f5741526a301825646"
PIN_KEYS = ("bytes", "sha256", "gitBlob")

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

def same_pin(successor: dict, v3_pin: dict) -> bool:
    """Every field the v3 pin records (bytes, SHA-256, Git blob where kept) equals the successor's preimage."""
    return all(successor.get(key) == v3_pin[key] for key in PIN_KEYS if key in v3_pin)

def run(root: Path, jar: Path | None = None, args_file: Path | None = None) -> dict:
    root = root.resolve()
    receipt_path = HERE / RECEIPT_NAME
    raw = checked(receipt_path)
    if len(raw) != RECEIPT_BYTES or sha256(raw) != RECEIPT_SHA256:
        raise ValueError("sealed successor receipt differs")
    receipt = json.loads(raw)
    v3 = json.loads(pinned(root, receipt["predecessor"]))
    pinned(root, receipt["predecessorVerifier"])
    history = [pinned(root, row) for row in v3["history"]]
    old = json.loads(history[0])
    v2 = json.loads(history[2])
    sources = old["sources"]
    owned = [row for row in sources if "/implementations/" in row["path"]]
    sdk_row = {"path": v3["sdkPath"], **v3["after"]}
    if (len(sources) != v3["oldSourceRows"] or len([row for row in sources if row["path"] == v3["sdkPath"]]) != 1
            or len({row["path"] for row in sources}) != len(sources)
            or len(owned) != v3["unchangedOwnedRows"] or owned != v2["targets"]):
        raise ValueError("historical source composition differs")
    v3_pins = {row["path"]: row for row in owned + v3["arrayAndNativeOwners"]}
    v3_pins[sdk_row["path"]] = sdk_row
    successors = {row["path"]: row for row in receipt["successors"]}
    if len(successors) != len(receipt["successors"]) or not set(successors) <= set(v3_pins):
        raise ValueError("successor rows differ from the v3 pins")
    for path, row in successors.items():
        if not same_pin({"path": path, **row["before"]}, v3_pins[path]):
            raise ValueError("successor preimage differs from the v3 pin: " + path)
        pinned(root, {"path": path, **row["after"]})
    for path, row in v3_pins.items():
        if path not in successors:
            pinned(root, row)
    pinned(root, v3["before"])
    sdk = checked(input_path(root, v3["sdkPath"]))
    if len(re.findall(rb"\bpublic\s+static\b", sdk)) < v3["preservedOldPublicStaticMethods"] + len(v3["addedFactories"]):
        raise ValueError("SDK public declaration count fell")
    for name in v3["addedFactories"]:
        if len(re.findall(rb"public\s+static\s+[^;{=]*\b" + name.encode("ascii") + rb"\s*\(", sdk)) != 1:
            raise ValueError("added factory declaration differs: " + name)
    for row in receipt["added"] + [receipt["recipe"]]:
        pinned(root, row)
    if jar is not None:
        data = checked(jar)
        provider = old["provider"]
        if (jar.name != provider["basename"] or len(data) != provider["bytes"]
                or sha256(data) != provider["sha256"]):
            raise ValueError("provider custody differs")
    if args_file is not None:
        output = args_file.resolve()
        protected = [input_path(root, path) for path in v3_pins]
        protected += [input_path(root, row["path"]) for row in v3["history"] + receipt["added"]]
        protected += [input_path(root, row["path"]) for row in
                      (v3["before"], receipt["predecessor"], receipt["predecessorVerifier"], receipt["recipe"])]
        protected += [receipt_path, Path(__file__).resolve()]
        if (args_file.is_symlink() or output.is_relative_to(root)
                or output in [path.resolve() for path in protected]
                or (output.exists() and any(output.samefile(path) for path in protected))):
            raise ValueError("argument output aliases protected source")
        paths = [str(input_path(root, row["path"]).resolve()).replace("\\", "/") for row in owned]
        args_file.write_text("\n".join('"' + path + '"' for path in paths) + "\n", encoding="utf-8")
    return {"sources": len(sources), "source_custody": "PASS", "provider_checked": jar is not None,
            "predecessor_sealed": True, "successors": len(successors), "added": len(receipt["added"]),
            "effective_sdk_bytes": len(sdk), "added_factories": v3["addedFactories"]}

def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--root", type=Path, default=HERE.parents[3])
    parser.add_argument("--jar", type=Path)
    parser.add_argument("--args", type=Path)
    args = parser.parse_args()
    print(json.dumps(run(args.root, args.jar, args.args)))

if __name__ == "__main__":
    main()
