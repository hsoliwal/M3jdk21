#!/usr/bin/env python3
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
# SPDX-License-Identifier: Apache-2.0
"""Receive only authenticated, actual OpenRewrite Results, serially by path."""
import hashlib
import json
import os
from pathlib import Path
import subprocess

WORK = Path(__file__).resolve().parent
ROOT = WORK.parent / "m3jdk21-integrated"
OUTPUT = WORK / "token-recovery-sdk-output"
MODULE = WORK / "recipe-token-candidates"
HANDOFF = WORK / "token-recovery-sdk-handoff.json"
INVENTORY = WORK / "token-recovery-recipe-inventory-pre-receive.json"
MANIFEST = MODULE / "src/main/resources/com/synexia/rewrite/m3jdk21-integrated-utilities-20261018/manifest.tsv"


def sha(path):
    return hashlib.sha256(path.read_bytes()).hexdigest() if path.is_file() else "ABSENT"


def require(condition, message):
    if not condition:
        raise SystemExit(message)


require(sha(HANDOFF) == "d6a8a17ff4a28fa8dc0aa266c7c64b790632410b0270ea008f4a4d8707978fc4", "handoff drift")
require(sha(INVENTORY) == "bd4b3ebb81f82034caab8fec0829e7e77f3099da781dd714c2becce81962d8b0", "source inventory drift")
handoff = json.loads(HANDOFF.read_text())
inventory = json.loads(INVENTORY.read_text())
require(sha(MANIFEST) == handoff["manifest_sha256"], "manifest drift")
require(sha(OUTPUT / "RECIPE_RESULTS.json") == handoff["actual_result_receipt_sha256"], "SDK result receipt drift")
head = subprocess.check_output(["git", "rev-parse", "HEAD"], cwd=ROOT, text=True).strip()
require(head == handoff["input_core_commit"], "receiver commit drift")
require(not subprocess.check_output(["git", "status", "--porcelain"], cwd=ROOT), "receiver is not clean")
for entry in inventory["files"]:
    require(sha(MODULE / entry["path"]) == entry["sha256"], "canonical candidate drift: " + entry["path"])

results = json.loads((OUTPUT / "RECIPE_RESULTS.json").read_text())
emitted = {entry["path"]: entry["sha256"] for entry in results["outputs"]}
require(len(emitted) == len(results["outputs"]) == 13, "result count mismatch")
rows = [line.split("\t") for line in MANIFEST.read_text().splitlines() if line]
require(len(rows) == 490 and len({r[0] for r in rows}) == 490, "manifest coverage mismatch")
changes = []
for path, before, after, resource in rows:
    rel = Path(path)
    require(not rel.is_absolute() and ".." not in rel.parts, "unsafe manifest path")
    target = ROOT / rel
    require(not any(p.is_symlink() for p in [target, *target.parents]), "symlink receiver path: " + path)
    require(sha(target) == before, "receiver preimage mismatch: " + path)
    if before != after:
        require(emitted.get(path) == after and sha(OUTPUT / rel) == after, "actual Result mismatch: " + path)
        changes.append((path, before, after))
    else:
        require(path not in emitted and resource == "KEEP", "non-result KEEP mismatch: " + path)
require(len(changes) == 13 and {p for p, _, _ in changes} == set(emitted), "manifest/result mismatch")

# Admission above completes for every input before any target mutation.
for path, before, after in changes:
    target = ROOT / path
    target.parent.mkdir(parents=True, exist_ok=True)
    temporary = target.with_name(target.name + ".m3-receive-tmp")
    with temporary.open("xb") as stream:
        stream.write((OUTPUT / path).read_bytes())
    os.chmod(temporary, 0o644)
    os.replace(temporary, target)
    require(sha(target) == after, "serial receive mismatch: " + path)
for path, before, after, resource in rows:
    require(sha(ROOT / path) == after, "received coverage mismatch: " + path)

receipt = {
    "schema": "M3_TOKEN_ACTUAL_SDK_RESULT_RECEIVE_V1",
    "input_commit": head,
    "handoff_sha256": sha(HANDOFF),
    "source_inventory_sha256": sha(INVENTORY),
    "manifest_sha256": sha(MANIFEST),
    "actual_result_receipt_sha256": sha(OUTPUT / "RECIPE_RESULTS.json"),
    "canonical_source_files_authenticated": len(inventory["files"]),
    "received_actual_results": len(changes),
    "verified_postimages": len(rows),
    "unchanged_keep_files": len(rows) - len(changes),
    "source_authoring": "NONE; exact SDK Result bytes copied in manifest order",
    "files": [{"path": p, "before_sha256": b, "after_sha256": a} for p, b, a in changes],
    "status": "PASSED"
}
(WORK / "token-received-results.json").write_text(json.dumps(receipt, indent=2) + "\n")
print(json.dumps({k: v for k, v in receipt.items() if k != "files"}))
