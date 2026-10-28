#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0
# Copyright 2026 Hitesh Soliwal and contributors
"""Reconstruct this fixed documentary packet into a fresh external directory."""

import argparse
import hashlib
import json
from pathlib import Path, PurePosixPath
import shutil
import sys

TRANSPORT = "m3/docs/composed-integration-20261021/EVIDENCE_TRANSPORT.json"
ORIGINAL_SELECTION = "b8dbd94ee955bdf8a9283e553c00208e00725efd3a8872d8941ae7df3ccca830"
LOGICAL_MANIFEST = "3eb363680608829af4d6eee1c2104c817de3d59214252c508fdb299f8bd4af52"
PACKETS = {
    "m3/docs/composed-integration-20261021/evidence/collections/PACKET_MANIFEST.json":
        "5e3d6eb075e61174c3de5b72c10f63c923bafbf9c5d84a45948eb267c23db06a",
    "m3/docs/composed-integration-20261021/evidence/collections/e2/PACKET_MANIFEST.json":
        "f7234aae1e9a690ccf52a7a4d6ef5a970e186fa9c88e3d9679f24efcfe89b297",
    "m3/docs/composed-integration-20261021/evidence/core/PACKET_MANIFEST.json":
        "0bc5395622babd5c0cff4ff2770d6359b14c3645e3a42669f5b85c09211f468d",
    "m3/docs/composed-integration-20261021/evidence/core/E2/PACKET_MANIFEST.json":
        "f39d5b1636ccd78b86dcad2b0e6a954ba0f082c6e6560ddb6e719a5aaa7a5db5",
}


def require(condition, reason):
    if not condition:
        raise ValueError(reason)


def unique_object(pairs):
    result = {}
    for key, value in pairs:
        require(key not in result, "duplicate JSON key")
        result[key] = value
    return result


def parse_json(data):
    return json.loads(data.decode("utf-8"), object_pairs_hook=unique_object)


def safe_path(value):
    require(isinstance(value, str) and 0 < len(value) <= 4096, "invalid path")
    require("\\" not in value and ":" not in value, "invalid path separator")
    require(not any(ord(c) < 32 or ord(c) == 127 for c in value), "control in path")
    require(all(p not in ("", ".", "..") for p in value.split("/")), "unsafe path")
    return PurePosixPath(value)


def read_direct(root, relative):
    path = root
    for part in safe_path(relative).parts:
        path = path / part
        require(not path.is_symlink(), "symlink in direct input")
    require(path.is_file(), "missing or nonregular direct input: " + relative)
    return path.read_bytes()


def sha256(data):
    return hashlib.sha256(data).hexdigest()


def verify_bytes(data, row):
    require(len(data) == row["bytes"], "byte count mismatch: " + row["path"])
    require(sha256(data) == row["sha256"], "SHA-256 mismatch: " + row["path"])
    blob = hashlib.sha1(b"blob " + str(len(data)).encode("ascii") + b"\0" + data).hexdigest()
    require(blob == row["git_blob"], "Git blob mismatch: " + row["path"])


def preflight(root):
    doc = parse_json(read_direct(root, TRANSPORT))
    require(doc["schema"] == "M3_DOCUMENTARY_EVIDENCE_TRANSPORT_V1", "wrong schema")
    require(doc["original_selection_sha256"] == ORIGINAL_SELECTION, "wrong original seal")
    rows = doc["logical_files"]
    require(isinstance(rows, list) and len(rows) == 82, "wrong logical file count")
    inventory = {}
    for row in rows:
        safe_path(row["path"])
        require(row["path"] not in inventory, "duplicate logical path")
        require(row["mode"] == "100644" and row["preimage"] == "ABSENT", "wrong mode or preimage")
        inventory[row["path"]] = row
    canonical = json.dumps(rows, sort_keys=True, separators=(",", ":"), ensure_ascii=True).encode("utf-8")
    require(doc["logical_manifest_sha256"] == LOGICAL_MANIFEST == sha256(canonical), "logical manifest drift")
    require((doc["logical_file_count"], doc["logical_content_bytes"],
             doc["direct_file_count"], doc["embedded_asset_count"]) == (82, 1153188, 69, 13),
            "transport count drift")
    embedded = {p for p in inventory if PurePosixPath(p).suffix in (".java", ".log", ".csv")}
    direct = set(inventory) - embedded
    expected_direct = [{"physical_path": p, "logical_path": p} for p in sorted(direct)]
    require(doc["direct_files"] == expected_direct, "direct mapping drift")
    assets = doc["embedded_assets"]
    require(isinstance(assets, list) and len(assets) == 13, "wrong embedded count")
    payloads = {}
    for asset in assets:
        path = asset["logical_path"]
        safe_path(path)
        require(path in embedded and path not in payloads, "undeclared or duplicate asset")
        require(asset["encoding"] == "UTF-8" and isinstance(asset["content_utf8"], str), "wrong asset encoding")
        for key in ("mode", "bytes", "sha256", "git_blob"):
            require(asset[key] == inventory[path][key], "asset metadata drift")
        payloads[path] = asset["content_utf8"].encode("utf-8")
    require(set(payloads) == embedded, "missing embedded asset")
    for path in sorted(direct):
        payloads[path] = read_direct(root, path)
    for path, data in payloads.items():
        verify_bytes(data, inventory[path])
    expected_packets = [{"path": p, "sha256": h} for p, h in PACKETS.items()]
    require(doc["packet_manifests"] == expected_packets, "packet manifest mapping drift")
    for path, digest in PACKETS.items():
        require(sha256(payloads[path]) == digest, "frozen packet manifest drift")
        members = parse_json(payloads[path])["files"]
        entries = ([{"path": p, "sha256": h} for p, h in members.items()]
                   if isinstance(members, dict) else members)
        seen = set()
        for entry in entries:
            relative = safe_path(entry["path"])
            member = str(PurePosixPath(path).parent / relative)
            require(member in payloads and member not in seen, "invalid packet member")
            seen.add(member)
            require(sha256(payloads[member]) == entry["sha256"], "packet member hash drift")
            if "bytes" in entry:
                require(len(payloads[member]) == entry["bytes"], "packet member size drift")
    return payloads


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--source-root", required=True, type=Path)
    parser.add_argument("--output", required=True, type=Path)
    args = parser.parse_args()
    try:
        root = args.source_root.resolve(strict=True)
        require(root.is_dir(), "source root is not a directory")
        require(not args.output.exists() and not args.output.is_symlink(), "output already exists")
        output = args.output.resolve()
        require(not output.is_relative_to(root), "output must be outside source root")
        require(output.parent.is_dir(), "output parent must already exist")
        payloads = preflight(root)  # No output or parent is created before all checks pass.
        output.mkdir(mode=0o755)
        try:
            for relative, data in sorted(payloads.items()):
                path = output / relative
                path.parent.mkdir(parents=True, exist_ok=True)
                with path.open("xb") as stream:
                    stream.write(data)
                path.chmod(0o644)
        except BaseException:
            shutil.rmtree(output)
            raise
        print(json.dumps({"status": "PASS_DOCUMENTARY_RECONSTRUCTION", "logical_files": 82,
                          "direct_files": 69, "embedded_assets": 13, "content_bytes": 1153188,
                          "verified_packet_manifests": 4,
                          "original_selection_sha256": ORIGINAL_SELECTION,
                          "logical_manifest_sha256": LOGICAL_MANIFEST}, sort_keys=True))
        return 0
    except (OSError, ValueError, KeyError, TypeError, UnicodeError) as error:
        print(json.dumps({"status": "REFUSED_DOCUMENTARY_RECONSTRUCTION", "reason": str(error)}, sort_keys=True),
              file=sys.stderr)
        return 2


if __name__ == "__main__":
    sys.exit(main())
