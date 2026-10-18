#!/usr/bin/env python3
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""Export receiver inputs from immutable Git objects, independent of checkout edits."""
import hashlib
import json
from pathlib import Path, PurePosixPath
import subprocess

WORK = Path(__file__).resolve().parents[2]
CHECKOUT = WORK / "M3jdk21"
BASE = "4b24824c3aa09d802121447f8970d442a855f064"
DESTINATION = WORK / "runtime-refinement/pinned-baseline"
HISTORY = ("m3/compatibility/m3-object-adapter-progress-20261017.tsv", "m3/compatibility/m3-default-template-source-seal.tsv")


def blob(path, missing=False):
    relative = PurePosixPath(path)
    if relative.is_absolute() or ".." in relative.parts or str(relative) != path:
        raise ValueError("unsafe Git source path")
    result = subprocess.run(["git", "show", BASE + ":" + path], cwd=CHECKOUT, capture_output=True)
    if result.returncode:
        absent = subprocess.run(["git", "ls-tree", BASE, "--", path], cwd=CHECKOUT, capture_output=True, check=True)
        if missing and not absent.stdout:
            return None
        raise ValueError("missing pinned source object: " + path)
    return result.stdout


def export(extra=()):
    commit = subprocess.check_output(["git", "rev-parse", BASE + "^{commit}"], cwd=CHECKOUT, text=True).strip()
    if commit != BASE:
        raise ValueError("pinned Git commit identity mismatch")
    paths = set(HISTORY) | set(extra) | {
        "m3/backports/ALGORITHM_CATALOGUE.tsv", "m3/docs/name-mapping.json",
        "m3/compatibility/check_object_adapter_progress_receiver_20261017.py",
        "m3/compatibility/verify_m3_default_template_20261017.py",
        "src/hotspot/share/classfile/classLoader.cpp",
    }
    for manifest in HISTORY:
        for row in blob(manifest).decode().splitlines():
            if row and not row.startswith("#"):
                paths.add(row.split("\t")[0])
    receipts = []
    for path in sorted(paths):
        content = blob(path, missing=True)
        destination = DESTINATION / path
        if content is None:
            if destination.exists():
                raise ValueError("unexpected file in pinned absent source path: " + path)
            receipts.append({"path": path, "git_blob": "ABSENT", "sha256": "ABSENT"})
            continue
        identity = hashlib.sha1(b"blob " + str(len(content)).encode() + b"\0" + content).hexdigest()
        expected = subprocess.check_output(["git", "rev-parse", BASE + ":" + path], cwd=CHECKOUT, text=True).strip()
        if identity != expected:
            raise ValueError("Git blob bytes mismatch")
        destination.parent.mkdir(parents=True, exist_ok=True)
        if destination.exists() and destination.read_bytes() != content:
            raise ValueError("pinned input export drift: " + path)
        if not destination.exists():
            destination.write_bytes(content)
        receipts.append({"path": path, "git_blob": identity,
                         "sha256": hashlib.sha256(content).hexdigest(), "size": len(content)})
    receipt = {"schema": "M3_IMMUTABLE_RUNTIME_BASELINE_V1", "commit": BASE,
               "source": "git objects; working tree contents are not inputs", "files": receipts}
    (WORK / "runtime-refinement/authoring/pinned-baseline-receipt.json").write_text(json.dumps(receipt, indent=2) + "\n")
    return DESTINATION


if __name__ == "__main__":
    print(export())
