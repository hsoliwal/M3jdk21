#!/usr/bin/env python3
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""Replay immutable v4 in an isolated historical tree, then admit exact current owners.

This successor does not modify the v4 receipt, verifier or original contracts.
Its effective source lane is three exact Git-blob/SHA256-validated Java/C files.
"""
from __future__ import annotations

import argparse
import hashlib
import importlib.util
import json
import os
from pathlib import Path, PurePosixPath
import subprocess
import sys
import tempfile

HERE = Path(__file__).resolve().parent
ROOT = HERE.parents[3]
V4 = "m3/collections/qualification/owned-apache-multivalue-20261024/"
D1 = ("m3/collections/recipe/owned-apache-multivalue-20261024/src/main/"
      "resources/com/synexia/rewrite/hash-pinned-java/apache-multivalue-v2-to-v3/")
RECEIPT_BLOB = "88460dfd7fc4e3bced30bf0869dd8441a034b1f4"
EXPECTED_V4_BLOB = "8c5db55ab4eb2ea04d6ef46ef29e3eaa3cb9950f"
EXPECTED_V4_VERIFIER = "f51ca531cd1ead74955ad0fa1e4ee7ac769e9ae8"
EXPECTED_V4_PROBE = "820797f4e45097bbfc5021f6fe487960b18fb5ec"


def blob(raw: bytes) -> str:
    return hashlib.sha1(b"blob " + str(len(raw)).encode("ascii") + b"\0" + raw).hexdigest()


def digest(raw: bytes) -> str:
    return hashlib.sha256(raw).hexdigest()


def pin(raw: bytes, record: dict, path: str) -> None:
    if (len(raw) != record["bytes"] or digest(raw) != record["sha256"]
            or ("gitBlob" in record and blob(raw) != record["gitBlob"])):
        raise ValueError("immutable source identity drift: " + path)


def local(root: Path, relative: str) -> Path:
    value = PurePosixPath(relative)
    if (not relative.startswith("m3/collections/") or value.is_absolute()
            or any(segment in ("", ".", "..") for segment in value.parts)
            or "\\" in relative):
        raise ValueError("unapproved source path: " + relative)
    path = root.joinpath(*value.parts)
    if not path.resolve().is_relative_to(root.resolve()):
        raise ValueError("source leaves repository: " + relative)
    return path


def read(root: Path, relative: str) -> bytes:
    file = local(root, relative)
    if not file.is_file() or file.is_symlink():
        raise ValueError("missing or symlinked file: " + relative)
    return file.read_bytes()


def original_module(root: Path):
    path = local(root, V4 + "verify_source_binding_successor_v4.py")
    spec = importlib.util.spec_from_file_location("m3_original_immutable_v4", path)
    if spec is None or spec.loader is None:
        raise ValueError("missing original immutable v4 module")
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


def verify(root: Path = ROOT, jar: Path | None = None,
           args_file: Path | None = None, probe: bool = False) -> dict:
    root = root.resolve()
    raw_receipt = read(root, "m3/collections/qualification/apache-multivalue-source-successor/RECEIPT.json")
    if blob(raw_receipt) != RECEIPT_BLOB:
        raise ValueError("v5 successor receipt drift")
    successor = json.loads(raw_receipt)
    if (successor.get("schema") != "M3_APACHE_MULTIVALUE_SOURCE_SUCCESSOR_V5"
            or len(successor.get("successors", [])) != 3
            or successor["predecessor"]["gitBlob"] != EXPECTED_V4_BLOB
            or successor["originalVerifier"]["gitBlob"] != EXPECTED_V4_VERIFIER
            or successor["originalProbe"]["gitBlob"] != EXPECTED_V4_PROBE):
        raise ValueError("v5 source transition schema drift")

    original = read(root, successor["predecessor"]["path"])
    pin(original, successor["predecessor"], successor["predecessor"]["path"])
    verifier_raw = read(root, successor["originalVerifier"]["path"])
    if blob(verifier_raw) != EXPECTED_V4_VERIFIER:
        raise ValueError("original v4 verifier changed")
    probe_raw = read(root, successor["originalProbe"]["path"])
    if blob(probe_raw) != EXPECTED_V4_PROBE:
        raise ValueError("original v4 negative probe changed")
    v4 = json.loads(original)
    v3 = json.loads(read(root, v4["predecessor"]["path"]))
    provenance = json.loads(read(root, v3["history"][0]["path"]))
    if (len(v4["successors"]) != 10 or len(provenance["sources"]) != 33
            or len(v4["added"]) != 2):
        raise ValueError("original Apache multivalue inventory changed")

    replacements: dict[str, bytes] = {}
    pins: dict[str, dict] = {}
    for row in successor["successors"]:
        path = row["path"]
        original_rows = [entry for entry in v4["successors"] if entry["path"] == path]
        if (len(original_rows) != 1 or original_rows[0]["after"] != row["previous"]
                or path in replacements):
            raise ValueError("source successor lacks an exact original v4 pin: " + path)
        current = read(root, path)
        pin(current, row["current"], path)
        snapshot = read(root, "m3/collections/qualification/apache-multivalue-source-successor/"
                        + row["snapshot"])
        pin(snapshot, row["previous"], path + " historical")
        if current == snapshot:
            raise ValueError("no real successor for: " + path)
        replacements[path] = snapshot
        pins[path] = row["current"]
    if len(replacements) != 3:
        raise ValueError("three qualified Java/JNI successors required")

    paths = {row["path"] for row in provenance["sources"]}
    paths.update(row["path"] for row in
                 v3["history"] + v3["arrayAndNativeOwners"] + v4["added"])
    paths.update((v3["before"]["path"], v3["sdkPath"],
                  v4["predecessor"]["path"], v4["predecessorVerifier"]["path"],
                  v4["recipe"]["path"], successor["predecessor"]["path"],
                  successor["originalVerifier"]["path"]))
    # The original negative-control runner verifies every historical source.
    # It needs the original v4 verifier and the three original D1 templates.
    templates = [
        D1 + row["path"].rsplit("/", 1)[1] + ".before.txt"
        for row in v4["successors"] if row["reason"].startswith("D1:")
    ]
    if len(templates) != 3:
        raise ValueError("expected three original D1 counter recipe fixtures")
    paths.update(templates)
    if not set(replacements).issubset(paths):
        raise ValueError("source replacement not in original v4 inventory")

    original_runner = original_module(root)
    with tempfile.TemporaryDirectory(prefix="m3-apache-v4-immutable-replay-") as scratch:
        scratch_root = Path(scratch)
        historical = scratch_root / "historical"
        historical.mkdir()
        size = 0
        for path in sorted(paths):
            current = read(root, path)
            restored = replacements.get(path, current)
            size += len(restored)
            if size > 16 * 1024 * 1024:
                raise ValueError("unexpected original-source memory bound")
            dest = local(historical, path)
            dest.parent.mkdir(parents=True, exist_ok=True)
            with dest.open("xb") as output:
                output.write(restored)
        receipt_location = V4 + "SDK_SOURCE_BINDING_SUCCESSOR_V4.json"
        dest = local(historical, receipt_location)
        dest.parent.mkdir(parents=True, exist_ok=True)
        with dest.open("xb") as output:
            output.write(original)

        prior = original_runner.run(historical, jar=jar)
        if (prior.get("source_custody") != "PASS"
                or prior.get("sources") != 33
                or prior.get("successors") != 10
                or prior.get("added") != 2):
            raise ValueError("unchanged v4 replay rejected")
        negative_count = 0
        if probe:
            if jar is None:
                raise ValueError("original negative probe requires exact provider JAR")
            cmd = [sys.executable, "-I", "-S", "-B",
                   str(local(root, successor["originalProbe"]["path"])),
                   "--root", str(historical), "--jar", str(jar.resolve()),
                   "--work", str(scratch_root / "negative")]
            result = subprocess.run(cmd, cwd=root, text=True, capture_output=True,
                                    timeout=500, check=False)
            if result.returncode != 0:
                raise ValueError("original v4 negative suite refused: "
                                 + (result.stderr + result.stdout)[-4000:])
            report = json.loads(result.stdout)
            if (report.get("status") != "PASS" or not report.get("restored")
                    or report.get("positiveCases") != 2
                    or report.get("negativeCases", 0) < 40):
                raise ValueError("v4 original drift refusal evidence incomplete")
            negative_count = report["negativeCases"]

    if args_file is not None:
        resolved = args_file.resolve()
        if (args_file.is_symlink() or args_file.exists()
                or resolved.is_relative_to(root)):
            raise ValueError("argument output would overwrite source or other output")
        owned = [row["path"] for row in provenance["sources"]
                 if "/implementations/" in row["path"]]
        paths_out = ['"' + str(local(root, name).resolve()).replace("\\", "/") + '"'
                     for name in owned]
        args_file.parent.mkdir(parents=True, exist_ok=True)
        with args_file.open("x", encoding="utf-8", newline="\n") as output:
            output.write("\n".join(paths_out) + "\n")
    return {"status": "PASS", "historicV4": "PASS", "originalSourceRows": 33,
            "changedJavaNativeOwners": len(replacements),
            "originalNegativeRefusals": negative_count,
            "providerVerified": jar is not None, "argsGenerated": args_file is not None}


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--root", type=Path, default=ROOT)
    parser.add_argument("--jar", type=Path)
    parser.add_argument("--args", type=Path)
    parser.add_argument("--probe", action="store_true")
    options = parser.parse_args()
    try:
        print(json.dumps(verify(options.root, options.jar, options.args, options.probe),
                         sort_keys=True))
    except (ValueError, OSError, KeyError, subprocess.SubprocessError,
            json.JSONDecodeError) as error:
        print("M3_APACHE_V5_REFUSED: " + str(error), file=sys.stderr)
        raise SystemExit(1)


if __name__ == "__main__":
    main()
